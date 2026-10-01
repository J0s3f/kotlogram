//! The update pump: the one reader of grammers' update stream, and the buffer it hands updates over
//! through.
//!
//! grammers' `UpdateStream::next_raw` is not safe to cancel, which is the whole reason this module
//! exists. It takes a batch off the updates receiver and only then *awaits* `build_peer_map` to
//! resolve the peers the batch brought; the peer map reaches the session cache whenever
//! `UpdatesConfiguration::auto_cache_peers` is set, and it is set by default. So a batch spends a
//! real await sitting in a local variable, already gone from the channel and not yet in the
//! stream's own buffer. A future dropped in that window has consumed the batch and will never
//! return it: the update is lost, and nothing asks Telegram for it again.
//!
//! `tokio::time::timeout` cancels by dropping, so `timeout(timeout, updates.next())` loses a batch
//! whenever the wait ends inside that window. Enlarging the timeout, clamping a minimum or racing
//! the poll against a `select!` arm all have the same fate, because every one of them drops the
//! future grammers handed us. The only safe shape is to never cancel it at all.
//!
//! So a task owns the stream and loops `next_raw().await` to completion, putting every result into a
//! channel this crate owns. A reader waits on *that* channel with the timeout its caller asked for.
//! Giving up on a receive from it loses nothing: the update is already built and buffered, so the
//! next poll takes it instead of Telegram having to send it again. The typed view is
//! `Update::from_raw` over the raw triple, which is exactly what `UpdateStream::next` does with the
//! output of `next_raw` itself.
//!
//! Two things follow from the pump owning the stream:
//!
//! * **One stream, one reader.** grammers' `UpdateStream` needs `&mut` to be read and only lends
//!   `&` for `sync_update_state`, so the pump is the only thing that ever holds it and every read
//!   operation goes through the one buffered queue. The typed and the raw paths therefore take turns
//!   on one ordered queue, which is what the facade's `UpdatesApi` promises its three read methods.
//! * **Pumping from the start.** grammers' sender pool forwards each batch it reads over a channel
//!   of a hundred batches and *drops* what does not fit, so a stream nobody is reading loses updates
//!   on its own, quite apart from the cancellation above. The pump is therefore started when the
//!   client is built rather than on the first poll, so the pool's channel is being drained from the
//!   first moment of the session.
//!
//! What the pump costs is [`UpdatePump::sync_update_state`], which cannot be carried out while a poll
//! is in flight. The request is queued and the pump performs it between two polls, so it waits for
//! the next update to arrive; the wait is bounded so that a quiet stream reports it rather than
//! blocking a JVM thread for however long grammers would wait.

use std::future::Future;
use std::sync::{Arc, Mutex as StdMutex};
use std::time::Duration;

use grammers_client::client::UpdateStream;
use grammers_client::peer::PeerMap;
use grammers_client::tl;
use grammers_session::updates::State;
use tokio::runtime::Runtime;
use tokio::sync::{mpsc, oneshot};
use tokio::task::JoinHandle;

use crate::error::invocation_error;

#[cfg(test)]
mod tests;

/// How long a reader waits when a test wants the next update as soon as it exists, and how long
/// [`stop_task`] waits for a pump to let go. Both are far above what a scheduled round trip needs
/// and exist only so a stalled test fails instead of hanging.
const PATIENCE: Duration = Duration::from_secs(5);

/// How long [`UpdatePump::sync_update_state`] waits for the pump to reach a moment between two polls.
///
/// The pump cannot be asked to stop what it is doing — that is the point of it — so the request waits
/// for the next update to arrive. On a quiet stream that is however long Telegram takes to send
/// something, which is why the wait ends in an error instead of in a block.
const SYNC_WAIT: Duration = Duration::from_secs(30);

/// Reported when the pump could not write the update state within [`SYNC_WAIT`].
const SYNC_BUSY: &str = "the update stream is between no polls: grammers only writes the update \
                         state when it is not receiving one, so try again once an update arrives";

/// Reported when a sync request is made of a pump that is no longer running.
const NO_PUMP: &str = "the update pump has stopped";

/// One update the pump built, exactly as grammers produced it: the TL update, the state it belongs
/// to, and the peers Telegram sent it with.
///
/// Keeping all three is what lets both read operations project one poll, and `Update::from_raw` over
/// them is the typed view of the same batch `nextRawUpdate` reports.
pub(crate) struct PolledUpdate {
    pub(crate) update: tl::enums::Update,
    pub(crate) state: State,
    pub(crate) peers: PeerMap,
}

/// What a reader receives: an update, or the failure the pump's poll reported.
pub(crate) type Polled = Result<PolledUpdate, String>;

/// The buffered handoff between a pump and its readers.
///
/// The receiving end is behind one lock, which is what makes the typed and the raw read take turns
/// on a single ordered queue instead of each having a stream of its own.
pub(crate) struct Handoff<T> {
    updates: tokio::sync::Mutex<mpsc::UnboundedReceiver<T>>,
}

impl<T> Handoff<T> {
    /// The handoff and the producing end the pump writes to.
    ///
    /// The channel is unbounded because a bounded one would reintroduce the loss this module exists
    /// to remove: a reader that is slow, or absent, must never turn into a dropped update. The
    /// memory an idle client holds is the same order as what grammers itself keeps in the stream's
    /// buffer while a poll is in flight.
    pub(crate) fn pair() -> (Self, mpsc::UnboundedSender<T>) {
        let (sink, updates) = mpsc::unbounded_channel();
        (Self {
            updates: tokio::sync::Mutex::new(updates),
        }, sink)
    }

    /// Waits up to [timeout] for the next update, or `None` when the wait ran out.
    ///
    /// Giving up here is safe and is the point of the module: the update being waited for is already
    /// buffered, so the next reader receives it. `None` also covers the pump having stopped, which
    /// only happens while the client is being closed.
    pub(crate) async fn next(&self, timeout: Duration) -> Option<T> {
        let mut updates = self.updates.lock().await;
        match tokio::time::timeout(timeout, updates.recv()).await {
            Ok(update) => update,
            Err(_) => None,
        }
    }
}

/// Runs [produce] and hands every result to [sink] until the sink is gone.
///
/// Each call to [produce] is awaited to completion and nothing here can cancel it, so a producer that
/// is grammers' `next_raw` always runs its whole batch through the peer map. The loop ends when the
/// last reader is gone, because there is then nothing left to pump for.
pub(crate) async fn run<T, P, F>(mut produce: P, sink: mpsc::UnboundedSender<T>)
where
    P: FnMut() -> F,
    F: Future<Output = T>,
{
    loop {
        let produced = produce().await;
        if sink.send(produced).is_err() {
            return;
        }
    }
}

/// A request the pump can only carry out between two polls, because it needs the stream immutably
/// while the poll itself needs it mutably.
enum Command {
    /// Write the update state, as `UpdateStream::sync_update_state` does, and answer on [reply].
    SyncState(oneshot::Sender<Result<(), String>>),
}

/// The update pump of one client: the buffered updates it has built, and the task still building
/// more.
pub(crate) struct UpdatePump {
    updates: Handoff<Polled>,
    commands: mpsc::UnboundedSender<Command>,
    task: StdMutex<Option<JoinHandle<()>>>,
}

impl UpdatePump {
    /// Starts pumping [stream], which this pump then owns until [`Self::stop`].
    ///
    /// The task is spawned on the client's runtime so it runs between the calls that read the
    /// updates it buffers. The stream is shared with the loop rather than moved into it because a
    /// `sync_update_state` request needs it immutably between polls.
    pub(crate) fn start(runtime: &Runtime, stream: UpdateStream) -> Self {
        let stream = Arc::new(tokio::sync::Mutex::new(stream));
        let (commands, queued) = mpsc::unbounded_channel();
        let queued = Arc::new(tokio::sync::Mutex::new(queued));
        let (updates, sink) = Handoff::pair();

        let task = runtime.spawn(run(
            move || {
                let stream = Arc::clone(&stream);
                let queued = Arc::clone(&queued);
                async move {
                    let mut stream = stream.lock().await;
                    // Anything requested while the last poll was in flight is carried out here,
                    // between polls, which is the only moment grammers lends the stream immutably.
                    let mut requested = queued.lock().await;
                    while let Ok(command) = requested.try_recv() {
                        match command {
                            Command::SyncState(reply) => {
                                let written = stream
                                    .sync_update_state()
                                    .await
                                    .map_err(|error| error.to_string());
                                let _ = reply.send(written);
                            }
                        }
                    }
                    drop(requested);
                    match stream.next_raw().await {
                        Ok((update, state, peers)) => Ok(PolledUpdate {
                            update,
                            state,
                            peers,
                        }),
                        Err(error) => Err(invocation_error(error)),
                    }
                }
            },
            sink,
        ));

        Self {
            updates,
            commands,
            task: StdMutex::new(Some(task)),
        }
    }

    /// Waits up to [timeout] for the next update, or `None` when the wait ran out.
    ///
    /// The wait is on the bridge's own channel, so it can end without taking anything out of
    /// grammers' stream: an update that arrives later is buffered by the pump and handed to the next
    /// poll.
    pub(crate) async fn next(&self, timeout: Duration) -> Option<Polled> {
        self.updates.next(timeout).await
    }

    /// Asks the pump to write the update state, as `syncUpdateState` does.
    ///
    /// The pump performs this between two polls, because a poll holds the stream mutably and cannot
    /// be interrupted without losing the batch it is holding. That makes the wait for the next
    /// update unavoidable, so it is bounded by [`SYNC_WAIT`] and reports [`SYNC_BUSY`] rather than
    /// blocking a JVM thread until Telegram speaks.
    pub(crate) fn sync_update_state(&self, runtime: &Runtime) -> Result<(), String> {
        let (reply, answered) = oneshot::channel();
        self.commands
            .send(Command::SyncState(reply))
            .map_err(|_| NO_PUMP.to_owned())?;
        let answer = runtime.block_on(async {
            tokio::time::timeout(SYNC_WAIT, answered).await
        });
        match answer {
            Ok(Ok(written)) => written,
            Ok(Err(_)) => Err(NO_PUMP.to_owned()),
            Err(_) => Err(SYNC_BUSY.to_owned()),
        }
    }

    /// Stops the pump and waits, bounded, for it to release the stream it owns.
    ///
    /// Called while the client is being closed, and before the sender pool is quit, so the stream is
    /// let go of before the session handle it carries is.
    pub(crate) fn stop(&self, runtime: &Runtime) -> bool {
        match self.task.lock() {
            Ok(mut task) => task.take().map_or(true, |task| stop_task(runtime, task)),
            // A poisoned lock still holds the handle, so the task is left to the runtime dropping
            // the client, which is the outcome this method exists to make prompt.
            Err(_) => false,
        }
    }
}

/// Stops the pump [task] and waits for it to let go of what it owns, up to [`SHUTDOWN_WAIT`].
///
/// The task is aborted rather than asked to finish: a poll that has found nothing waits for Telegram
/// for as long as grammers' own no-updates timeout, which is fifteen minutes, so waiting for the
/// poll to end would hold up `close` for that long. Nothing is lost here that close does not lose
/// anyway — the client is going away, so there is no reader left to hand an update to, and the
/// update state on disk is untouched.
///
/// Returns whether the task was done within the bound.
pub(crate) fn stop_task(runtime: &Runtime, task: JoinHandle<()>) -> bool {
    task.abort();
    matches!(
        runtime.block_on(async { tokio::time::timeout(SHUTDOWN_WAIT, task).await }),
        Ok(_)
    )
}

/// How long [`stop_task`] waits for a pump to release what it holds.
const SHUTDOWN_WAIT: Duration = PATIENCE;