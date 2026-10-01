//! Tests for the update pump, driven by a fake stream.
//!
//! The bug these guard is a loss, and a loss cannot be asserted against a live Telegram session, so
//! the stream is faked with the one property that makes it lossy: a batch is taken off the channel
//! and only then awaited, which is grammers' `next_raw` resolving the peer map after consuming the
//! batch. Cancelling in that window drops the batch; the pump, which never cancels, keeps it.
//!
//! What each test proves, and what it does not:
//!
//! * `a_reader_that_gives_up_still_receives_the_update_it_waited_for` and
//!   `many_updates_arrive_in_order_and_none_are_lost_across_give_ups` are the behavioural ones: an
//!   update a reader gave up on is still delivered, whole and in order, to the next poll. They prove
//!   nothing about grammers itself — the fake stands in for `next_raw`, and a real session holds the
//!   peer map open for as long as its cache takes.
//! * `a_cancelled_poll_of_the_same_producer_loses_the_update` is the same fake driven the way a read
//!   operation used to drive it. It passes *because* the update is lost, so the loss the pump removes
//!   is documented rather than remembered, and the fix has something to be measured against.
//! * `no_read_operation_cancels_a_grammers_poll` is structural: it pins a pattern in the source, not a
//!   behaviour, and cannot tell whether the pump works — only that the old shape has not come back.
//! * `a_pump_with_a_poll_outstanding_stops_without_blocking_close` and
//!   `a_pump_whose_readers_are_gone_stops_by_itself` cover the lifecycle `close` depends on.

use std::future::{pending, Future};
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::time::Duration;

use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::sync::Mutex;

use super::{run, stop_task, Handoff};

/// A wait a test expects to run out. Long enough that a loaded machine does not end it early, short
/// enough that giving up six times in a row costs nothing.
const GIVE_UP: Duration = Duration::from_millis(25);

/// The upper bound of anything a test waits for, so a stalled pump fails the test rather than
/// hanging it.
const PATIENCE: Duration = Duration::from_secs(5);

/// One batch of updates, as an opaque payload a reader can only get back by not losing it.
type Batch = Vec<u8>;

/// grammers' update stream, with the one behaviour that makes a poll lossy.
///
/// `next_raw` takes a batch off the channel and only then awaits, which is where grammers resolves
/// the peer map and writes it to the session. A caller that stops waiting in that window drops a
/// batch it has already consumed.
struct FakeStream {
    batches: mpsc::UnboundedReceiver<Batch>,
    /// Reports each batch as it is taken off the channel, before the await.
    taken: mpsc::UnboundedSender<Batch>,
    /// Releases the await, which a test opens once it has acted on [Self::taken].
    peer_map: mpsc::UnboundedReceiver<()>,
}

impl FakeStream {
    /// A stream with the handles a test drives it through.
    fn new() -> (Self, FakeSource) {
        let (to_poll, batches) = mpsc::unbounded_channel();
        let (to_stream, taken) = mpsc::unbounded_channel();
        let (resolved, peer_map) = mpsc::unbounded_channel();
        (
            Self {
                batches,
                taken: to_stream,
                peer_map,
            },
            FakeSource {
                batches: to_poll,
                taken,
                resolved,
            },
        )
    }

    /// One poll, shaped like `UpdateStream::next_raw`.
    ///
    /// A poll that finds nothing is a wait rather than an answer, which is what grammers does: the
    /// empty channel shows up as the caller's own wait running out, not as a result.
    async fn next_raw(&mut self) -> Batch {
        let batch = self
            .batches
            .recv()
            .await
            .expect("the test holds the source open");
        let _ = self.taken.send(batch.clone());
        // The peer map: an await with the batch held in a local variable across it.
        self.peer_map.recv().await;
        batch
    }
}

/// What a test holds to feed a fake stream and to observe it.
struct FakeSource {
    batches: mpsc::UnboundedSender<Batch>,
    taken: mpsc::UnboundedReceiver<Batch>,
    resolved: mpsc::UnboundedSender<()>,
}

impl FakeSource {
    /// Hands one batch to the stream, as the sender pool does when it reads one off the socket.
    fn deliver(&self, batch: Batch) {
        let _ = self.batches.send(batch);
    }

    /// Waits for a poll to have taken [expected] off the channel, which is the last point at which
    /// the update still exists anywhere.
    async fn taken(&mut self, expected: &Batch) {
        let taken = tokio::time::timeout(PATIENCE, self.taken.recv())
            .await
            .expect("a poll takes a batch")
            .expect("the stream is open");
        assert_eq!(&taken, expected, "the batches are taken in the order they arrived");
    }

    /// Lets the awaited peer map resolve, so the poll holding the batch returns it.
    fn resolve_peer_map(&self) {
        let _ = self.resolved.send(());
    }
}

/// The producer a pump is started with: one grammers-shaped poll per call, awaited to completion.
fn producer(
    stream: Arc<Mutex<FakeStream>>,
) -> impl FnMut() -> Pin<Box<dyn Future<Output = Batch> + Send>> {
    move || {
        let stream = Arc::clone(&stream);
        Box::pin(async move { stream.lock().await.next_raw().await })
    }
}

/// A runtime with a timer, which every wait in these tests needs.
fn runtime() -> Runtime {
    tokio::runtime::Builder::new_current_thread()
        .enable_time()
        .build()
        .expect("a test runtime")
}

/// How many updates the ordering test runs through, each of them with a reader that gives up.
const ROUNDS: usize = 6;

#[test]
fn a_reader_that_gives_up_still_receives_the_update_it_waited_for() {
    let runtime = runtime();
    let batch: Batch = vec![7; 8];
    let (stream, mut source) = FakeStream::new();
    source.deliver(batch.clone());
    let stream = Arc::new(Mutex::new(stream));
    let (handoff, items) = Handoff::<Batch>::pair();
    let task = runtime.spawn(run(producer(stream), items));

    runtime.block_on(async {
        source.taken(&batch).await;
        // The reader gives up while the pump holds the batch it took off the channel.
        assert!(
            handoff.next(GIVE_UP).await.is_none(),
            "the reader is still waiting while the batch is held, so nothing can be delivered"
        );

        // The peer map resolves afterwards. Telegram was never asked for the batch a second time, so
        // the only way the reader can see it is through the pump's buffer: giving up cost the reader
        // its wait, not its update.
        source.resolve_peer_map();
        assert_eq!(
            handoff.next(PATIENCE).await,
            Some(batch),
            "the next poll must receive the update the first one gave up on, unchanged"
        );
    });

    task.abort();
}

#[test]
fn many_updates_arrive_in_order_and_none_are_lost_across_give_ups() {
    let runtime = runtime();
    let batches: Vec<Batch> = (0..ROUNDS).map(|round| vec![round as u8; 4]).collect();
    let (stream, mut source) = FakeStream::new();
    // Every batch is delivered up front, so the pump buffers a queue of them while the reader is
    // away: the reader giving up repeatedly must still leave that queue whole and ordered.
    for batch in &batches {
        source.deliver(batch.clone());
    }
    let stream = Arc::new(Mutex::new(stream));
    let (handoff, items) = Handoff::<Batch>::pair();
    let task = runtime.spawn(run(producer(stream), items));

    let received = runtime.block_on(async {
        let mut received: Vec<Batch> = Vec::new();
        for (round, batch) in batches.iter().enumerate() {
            source.taken(batch).await;
            assert!(
                handoff.next(GIVE_UP).await.is_none(),
                "round {round} should give up with its batch still held"
            );
            source.resolve_peer_map();
            received.push(
                handoff
                    .next(PATIENCE)
                    .await
                    .unwrap_or_else(|| panic!("round {round} lost its update")),
            );
        }
        received
    });

    assert_eq!(
        received, batches,
        "every update must arrive, unchanged and in the order it was sent"
    );
    task.abort();
}

#[test]
fn a_cancelled_poll_of_the_same_producer_loses_the_update() {
    // The shape the pump replaced, over the same fake: the caller waits for grammers' poll with a
    // timeout, and the timeout drops the future while the batch sits in a local variable between the
    // channel and the peer map.
    let runtime = runtime();
    let batch: Batch = vec![7; 8];
    let (mut stream, source) = FakeStream::new();
    source.deliver(batch.clone());

    let cancelled = runtime.block_on(async {
        tokio::time::timeout(GIVE_UP, stream.next_raw()).await
    });
    assert!(
        cancelled.is_err(),
        "the wait has to end inside the peer map for this to show anything"
    );

    // The batch went off the channel with the poll that was dropped, and it was never buffered, so
    // the next poll waits for something that is never sent again: the update is lost. Resolving the
    // peer map now changes nothing, because the poll holding the batch is gone.
    source.resolve_peer_map();
    let after = runtime.block_on(async {
        tokio::time::timeout(GIVE_UP, stream.next_raw()).await
    });
    assert!(
        after.is_err(),
        "the update was lost with the cancelled poll: {after:?}"
    );
}

#[test]
fn no_read_operation_cancels_a_grammers_poll() {
    // Structural, and only that. It pins a pattern rather than a behaviour: it cannot tell whether
    // the pump delivers what it buffers, only that the cancelling timeout is not around a grammers
    // poll any more. The two behavioural tests above are what establish that nothing is lost.
    let source = std::fs::read_to_string(
        std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("src/ops/updates.rs"),
    )
    .expect("src/ops/updates.rs must be readable");

    for cancelling in ["tokio::time::timeout(", "updates.next()", "updates.next_raw()"] {
        assert!(
            !source.contains(cancelling),
            "src/ops/updates.rs must not contain {cancelling:?}: a timeout around grammers' own poll \
             drops a batch that was consumed but not yet buffered, so the wait belongs on the pump's \
             channel instead"
        );
    }
    assert!(
        source.contains("native.update_pump.next(timeout)"),
        "a read operation should wait on the pump's channel"
    );
}

#[test]
fn a_pump_with_a_poll_outstanding_stops_without_blocking_close() {
    // `close` stops the pump while a poll may be outstanding, and that poll can legitimately wait
    // for Telegram for as long as grammers' no-updates timeout. The stop must not wait for it, and
    // must hand the runtime back: the client drops the runtime straight afterwards, and a task still
    // holding the stream would hold the session with it.
    let runtime = runtime();
    let released = Arc::new(AtomicBool::new(false));
    let inside = Arc::clone(&released);
    let (_handoff, items) = Handoff::<Batch>::pair();

    // A poll that begins and never returns, which is what an idle stream looks like from here.
    let task = runtime.spawn(run(
        move || {
            let released = Arc::clone(&inside);
            Box::pin(async move {
                // Held for as long as the poll is outstanding, and released only when it is dropped.
                let _held = Released(Arc::clone(&released));
                pending::<Batch>().await
            }) as Pin<Box<dyn Future<Output = Batch> + Send>>
        },
        items,
    ));

    runtime.block_on(async { tokio::time::sleep(Duration::from_millis(10)).await });
    assert!(
        !released.load(Ordering::SeqCst),
        "the outstanding poll is still holding what it took"
    );

    let stopped = std::time::Instant::now();
    assert!(
        stop_task(&runtime, task),
        "stopping a pump whose poll never returns must not wait for the poll"
    );
    assert!(
        stopped.elapsed() < PATIENCE,
        "close must not be held up by an outstanding poll, took {:?}",
        stopped.elapsed()
    );
    assert!(
        released.load(Ordering::SeqCst),
        "the outstanding poll must be dropped, which is what releases the stream"
    );
}

#[test]
fn a_pump_whose_readers_are_gone_stops_by_itself() {
    // The other way a pump ends: every reader is gone, so there is nothing left to pump for and the
    // task must not stay behind holding the stream. The order matters — the batch is left held in the
    // poll so the readers are certainly gone by the time the poll comes to hand it over, which is
    // the send that finds nobody waiting.
    let runtime = runtime();
    let batch: Batch = vec![7; 8];
    let (stream, mut source) = FakeStream::new();
    source.deliver(batch.clone());
    let stream = Arc::new(Mutex::new(stream));
    let (handoff, items) = Handoff::<Batch>::pair();
    let task = runtime.spawn(run(producer(stream), items));

    runtime.block_on(async {
        source.taken(&batch).await;
        drop(handoff);
        source.resolve_peer_map();
    });
    assert!(
        runtime
            .block_on(async { tokio::time::timeout(PATIENCE, task).await })
            .is_ok(),
        "the pump must end once its readers are gone, rather than polling for an update nobody can \
         receive"
    );
}

/// Sets a flag when dropped, so a test can see that a poll future really was released.
struct Released(Arc<AtomicBool>);

impl Drop for Released {
    fn drop(&mut self) {
        self.0.store(true, Ordering::SeqCst);
    }
}