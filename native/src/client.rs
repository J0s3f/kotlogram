//! The grammers-backed client, its registry, and peer resolution.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicI64, AtomicU64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

use grammers_client::client::{LoginToken, PasswordToken};
use grammers_client::media::Uploaded;
use grammers_client::peer::Peer;
use grammers_client::Client;
use grammers_mtsender::UpdatesConfiguration;
use grammers_mtsender::{SenderPool, SenderPoolHandle};
use grammers_session::storages::SqliteSession;
use grammers_session::types::PeerRef;
use jni::sys::jlong;
use tokio::io::AsyncSeekExt;
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::task::JoinHandle;

use crate::error::{error, invocation_error};
use crate::payload::{FileSource, PeerTarget};
use crate::update_pump::UpdatePump;
use crate::upload::{stream_channel, ChannelReader, ProgressReader, UploadProgress};

/// One live Telegram session: its Tokio runtime, its grammers client and its registries.
pub(crate) struct NativeClient {
    pub(crate) runtime: Runtime,
    pub(crate) client: Client,
    pub(crate) sender: SenderPoolHandle,
    pub(crate) update_pump: UpdatePump,
    pub(crate) session: Arc<SqliteSession>,
    pub(crate) api_hash: String,
    pub(crate) authentication: Mutex<AuthenticationState>,
    pub(crate) peers: Mutex<HashMap<i64, Peer>>,
    pub(crate) next_peer_handle: AtomicI64,
    pub(crate) uploads: UploadRegistry,
}

/// One upload that has begun through `uploadStreamBegin` and has not been finished yet.
///
/// The declared name travels with the state because `Client::upload_stream` needs it when the
/// upload task is spawned, and nothing else remembers it between the begin and the finish call.
/// The bytes themselves are **not** here: the task reads them from a bounded channel as the JVM
/// sends them, so only [`upload::STREAM_CHANNEL_CAPACITY`](crate::upload) chunks exist at once.
#[derive(Debug)]
pub(crate) struct StreamUpload {
    pub(crate) size: u64,
    /// How many bytes have been handed to the channel, so a short or fat stream is refused.
    pub(crate) accepted: Arc<AtomicU64>,
    /// The producing end of the channel; dropping it is what ends the reader's stream.
    pub(crate) sender: mpsc::Sender<Vec<u8>>,
    /// The upload in flight; the finish call awaits it.
    pub(crate) task: JoinHandle<Result<Uploaded, String>>,
}

/// The per-client upload state: the in-progress stream buffers and the uploads that are done.
///
/// The three maps share one id space, drawn from [Self::next_handle], so an id cannot be mistaken
/// for a handle or a progress slot. A stream that is still taking chunks lives in `streams`; every
/// upload's live [`UploadProgress`] lives in `progress` (a stream's slot is created with it and a
/// path upload is given one through `uploadProgressBegin`); once a stream is finished its
/// [`Uploaded`] moves to `uploads`, where a send resolves it by the handle the finish returned.
/// The maps live as long as the client and are dropped with it, which is the documented lifetime of
/// a handle. Each map is behind its own mutex, and every lock is held only for the length of one
/// lookup or insert.
#[derive(Default)]
pub(crate) struct UploadRegistry {
    streams: Mutex<HashMap<i64, StreamUpload>>,
    uploads: Mutex<HashMap<i64, Uploaded>>,
    progress: Mutex<HashMap<i64, Arc<UploadProgress>>>,
    next_handle: AtomicI64,
}

impl UploadRegistry {
    /// The next id, shared by stream ids, upload handles and progress slots so they never collide.
    pub(crate) fn next(&self) -> i64 {
        self.next_handle.fetch_add(1, Ordering::Relaxed)
    }

    /// Stores a stream under [id].
    pub(crate) fn insert_stream(&self, id: i64, stream: StreamUpload) -> Result<(), String> {
        self.streams
            .lock()
            .map_err(|_| "upload stream registry is poisoned".to_owned())?
            .insert(id, stream);
        Ok(())
    }

    /// Stores a progress counter under [id].
    pub(crate) fn insert_progress(
        &self,
        id: i64,
        progress: Arc<UploadProgress>,
    ) -> Result<(), String> {
        self.progress
            .lock()
            .map_err(|_| "upload progress registry is poisoned".to_owned())?
            .insert(id, progress);
        Ok(())
    }

    /// Allocates a progress slot for an upload whose bytes are not fed through a stream, which is a
    /// path upload. Returns the id the caller polls and passes back to `uploadFile`/`sendMedia`.
    pub(crate) fn begin_progress(&self, total: u64) -> Result<i64, String> {
        let id = self.next();
        self.insert_progress(id, UploadProgress::new(total))?;
        Ok(id)
    }

    /// The live progress of the upload [id] names.
    pub(crate) fn progress(&self, id: i64) -> Result<Arc<UploadProgress>, String> {
        self.progress
            .lock()
            .map_err(|_| "upload progress registry is poisoned".to_owned())?
            .get(&id)
            .cloned()
            .ok_or_else(|| format!("unknown upload progress: {id}"))
    }

    /// The sender and accepted counter of the stream [id], which is what a chunk needs.
    pub(crate) fn stream_handle(
        &self,
        id: i64,
    ) -> Result<(mpsc::Sender<Vec<u8>>, Arc<AtomicU64>, u64), String> {
        let streams = self
            .streams
            .lock()
            .map_err(|_| "upload stream registry is poisoned".to_owned())?;
        let stream = streams
            .get(&id)
            .ok_or_else(|| format!("unknown upload stream: {id}"))?;
        Ok((stream.sender.clone(), Arc::clone(&stream.accepted), stream.size))
    }

    /// Removes and returns the stream [id], which is what finishing it consumes.
    pub(crate) fn take_stream(&self, id: i64) -> Result<StreamUpload, String> {
        self.streams
            .lock()
            .map_err(|_| "upload stream registry is poisoned".to_owned())?
            .remove(&id)
            .ok_or_else(|| format!("unknown upload stream: {id}"))
    }

    /// Remembers a finished [uploaded] and returns the handle that references it.
    pub(crate) fn register(&self, uploaded: Uploaded) -> Result<i64, String> {
        let handle = self.next();
        self.uploads
            .lock()
            .map_err(|_| "upload registry is poisoned".to_owned())?
            .insert(handle, uploaded);
        Ok(handle)
    }

    /// Resolves an upload [handle] to the file a send reuses, or reports an unknown handle.
    pub(crate) fn get(&self, handle: i64) -> Result<Uploaded, String> {
        self.uploads
            .lock()
            .map_err(|_| "upload registry is poisoned".to_owned())?
            .get(&handle)
            .cloned()
            .ok_or_else(|| format!("unknown upload handle: {handle}"))
    }
}

impl NativeClient {
    /// Starts a streamed upload of [size] bytes named [name] and returns its id.
    ///
    /// A bounded channel and a task reading it through a counting reader are set up, and the task
    /// is spawned on the client's runtime so it runs while the JVM keeps calling
    /// `uploadStreamChunk`. The reader has to start before the chunks arrive, because
    /// `Client::upload_stream` must be told the size before it reads anything, which is why the
    /// size is part of the begin call rather than discovered at the end.
    pub(crate) fn start_stream_upload(&self, name: String, size: u64) -> Result<i64, String> {
        let progress = UploadProgress::new(size);
        let (sender, receiver) = stream_channel();
        let accepted = Arc::new(AtomicU64::new(0));
        let task_reader: ChannelReader = receiver;
        let task_progress = Arc::clone(&progress);
        let task_name = name.clone();
        let client = self.client.clone();
        let task = self.runtime.spawn(async move {
            let mut reader = ProgressReader::new(task_reader, task_progress);
            client
                .upload_stream(&mut reader, size as usize, task_name)
                .await
                .map_err(error)
        });

        let id = self.uploads.next();
        self.uploads.insert_progress(id, Arc::clone(&progress))?;
        self.uploads.insert_stream(
            id,
            StreamUpload {
                size,
                accepted,
                sender,
                task,
            },
        )?;
        Ok(id)
    }

    /// Hands one chunk to the stream [id] names, awaiting room when the uploader is behind.
    ///
    /// More bytes than the declared size is an error rather than a silent truncation.
    pub(crate) fn append_stream(&self, id: i64, bytes: &[u8]) -> Result<(), String> {
        if bytes.is_empty() {
            return Ok(());
        }
        let (sender, accepted, size) = self.uploads.stream_handle(id)?;
        accepted
            .fetch_update(Ordering::Relaxed, Ordering::Relaxed, |current| {
                (current + bytes.len() as u64 <= size).then_some(current + bytes.len() as u64)
            })
            .map_err(|_| {
                format!(
                    "upload stream {id} received more than its declared size of {size} bytes"
                )
            })?;
        self.runtime
            .block_on(sender.send(bytes.to_vec()))
            .map_err(|_| format!("upload stream {id} is no longer accepting chunks"))
    }
}

/// Uploads a local file while counting the bytes read into [progress], grammers' own
/// `Client::upload_file` with the reader wrapped.
///
/// This is `upload_file` reimplemented: open the file, take its length and name, and hand a
/// counting reader to `upload_stream`. The parts sent and the resulting `Uploaded` are identical,
/// so substituting it for `upload_file` changes nothing but the observation point.
pub(crate) async fn upload_path_with_progress(
    client: &Client,
    path: PathBuf,
    progress: Arc<UploadProgress>,
) -> Result<Uploaded, String> {
    let mut file = tokio::fs::File::open(&path).await.map_err(error)?;
    let size = file
        .seek(std::io::SeekFrom::End(0))
        .await
        .map_err(error)?;
    file.seek(std::io::SeekFrom::Start(0))
        .await
        .map_err(error)?;
    let name = path
        .file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .ok_or_else(|| format!("cannot upload a path without a file name: {}", path.display()))?;
    progress.set_total(size);
    let mut reader = ProgressReader::new(file, progress);
    client
        .upload_stream(&mut reader, size as usize, name)
        .await
        .map_err(error)
}

/// The step of the user login flow that has been reached so far.
pub(crate) enum AuthenticationState {
    Idle,
    LoginCode(LoginToken),
    Password(PasswordToken),
}

type Clients = Mutex<HashMap<jlong, Arc<NativeClient>>>;

fn clients() -> &'static Clients {
    static CLIENTS: OnceLock<Clients> = OnceLock::new();
    CLIENTS.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Looks up a live client by the handle Kotlin holds.
pub(crate) fn get_client(handle: jlong) -> Result<Arc<NativeClient>, String> {
    clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())?
        .get(&handle)
        .cloned()
        .ok_or_else(|| format!("unknown client handle: {handle}"))
}

/// Starts a runtime and a grammers session, and registers the result. Returns the handle as a
/// string because that is what the JNI export hands back to Kotlin.
pub(crate) fn create_client(
    api_id: i32,
    api_hash: String,
    session_path: PathBuf,
) -> Result<String, String> {
    let runtime = Runtime::new().map_err(error)?;
    let (client, sender, updates, session, runner) = runtime.block_on(async move {
        let session = Arc::new(SqliteSession::open(session_path).await.map_err(error)?);
        let pool = SenderPool::new(Arc::clone(&session), api_id);
        let client = Client::new(pool.handle.clone());
        let updates = client
            .stream_updates(pool.updates, UpdatesConfiguration::default())
            .await
            .map_err(|error| error.to_string())?;
        Ok::<_, String>((client, pool.handle.thin, updates, session, pool.runner))
    })?;
    runtime.spawn(runner.run());
    // The pump is started here rather than on the first poll: grammers' sender pool forwards each
    // batch it reads over a channel that holds a hundred of them and drops what does not fit, so a
    // stream nobody is reading loses updates on its own. Draining it from the first moment of the
    // session is what keeps that from happening, and the pump is also the only safe reader of the
    // stream, because grammers' own poll cannot be cancelled without losing the batch it holds.
    let update_pump = UpdatePump::start(&runtime, updates);

    let native = Arc::new(NativeClient {
        runtime,
        client,
        sender,
        update_pump,
        session,
        api_hash,
        authentication: Mutex::new(AuthenticationState::Idle),
        peers: Mutex::new(HashMap::new()),
        next_peer_handle: AtomicI64::new(1),
        uploads: UploadRegistry::default(),
    });
    let handle = Arc::as_ptr(&native) as jlong;
    clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())?
        .insert(handle, native);
    Ok(handle.to_string())
}

/// Drops a client from the registry and stops its sender pool. Closing an unknown handle succeeds.
///
/// The update pump is stopped first, and while the runtime is still alive, so the stream it owns is
/// let go of before the pool it reads from is quit and before the session handle goes with the
/// client: a pump left running would hold the session past the close and keep the runtime from
/// shutting down.
pub(crate) fn close_client(handle: jlong) -> String {
    let result = clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())
        .map(|mut map| map.remove(&handle));
    match result {
        Ok(Some(client)) => {
            client.update_pump.stop(&client.runtime);
            client.sender.quit();
            "ok".to_owned()
        }
        Ok(None) => "ok".to_owned(),
        Err(message) => error(message),
    }
}

/// Remembers [peer] and returns the handle Kotlin passes back in later payloads.
pub(crate) fn register_peer(native: &NativeClient, peer: &Peer) -> Result<i64, String> {
    let native_handle = native.next_peer_handle.fetch_add(1, Ordering::Relaxed);
    native
        .peers
        .lock()
        .map_err(|_| "peer registry is poisoned".to_owned())?
        .insert(native_handle, peer.clone());
    Ok(native_handle)
}

/// Resolves a payload peer selector into a [PeerRef], which is what the grammers client methods
/// take.
///
/// The peer is resolved to its full form first and then converted, because the conversion asks
/// grammers whether the peer is usable on its own (it carries an access hash) and falls back to the
/// session cache when it does not.
pub(crate) async fn resolve_peer(
    native: &NativeClient,
    target: &PeerTarget,
) -> Result<PeerRef, String> {
    let peer = resolve_peer_projected(native, target).await?;
    peer.to_ref()
        .await
        .map_err(|error| error.to_string())?
        .ok_or_else(|| "the resolved peer is not usable on its own".to_owned())
}

/// Resolves the file a send attaches.
///
/// A path is uploaded now, exactly as a plain `sendFile` always did, and, when [progress] is given,
/// through the counting reader so a `uploadProgressBegin` slot observes it; a handle is the
/// finished upload the registry kept, reused without a second upload.
pub(crate) async fn resolve_upload(
    native: &NativeClient,
    source: FileSource,
    progress: Option<Arc<UploadProgress>>,
) -> Result<Uploaded, String> {
    match source {
        FileSource::Path(path) => match progress {
            Some(progress) => {
                upload_path_with_progress(&native.client, PathBuf::from(path), progress).await
            }
            None => native
                .client
                .upload_file(PathBuf::from(path))
                .await
                .map_err(error),
        },
        FileSource::Handle(handle) => native.uploads.get(handle),
    }
}

/// Resolves a payload peer selector into the full peer, for projecting.
pub(crate) async fn resolve_peer_projected(
    native: &NativeClient,
    target: &PeerTarget,
) -> Result<Peer, String> {
    if let Some(handle) = target.peer_handle {
        return native
            .peers
            .lock()
            .map_err(|_| "peer registry is poisoned".to_owned())?
            .get(&handle)
            .cloned()
            .ok_or_else(|| format!("unknown peer handle: {handle}"));
    }

    let username = target
        .username
        .as_deref()
        .ok_or_else(|| "a peerHandle or username is required".to_owned())?
        .trim_start_matches('@');
    native
        .client
        .resolve_username(username)
        .await
        .map_err(invocation_error)?
        .ok_or_else(|| format!("username not found: {username}"))
}

#[cfg(test)]
mod tests {
    use grammers_client::tl;

    use super::*;

    fn uploaded(id: i64) -> Uploaded {
        Uploaded::from_raw(tl::enums::InputFile::File(tl::types::InputFile {
            id,
            parts: 1,
            name: format!("file-{id}.bin"),
            md5_checksum: String::new(),
        }))
    }

    /// A stream whose task never touches the network, for exercising the registry alone.
    fn idle_stream(size: u64) -> (StreamUpload, ChannelReader, Runtime) {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .build()
            .expect("a test runtime");
        let (sender, receiver) = stream_channel();
        let task = runtime.spawn(async { Err("not a real upload".to_owned()) });
        (
            StreamUpload {
                size,
                accepted: Arc::new(AtomicU64::new(0)),
                sender,
                task,
            },
            receiver,
            runtime,
        )
    }

    #[test]
    fn a_started_stream_exposes_its_sender_and_declared_size() {
        let registry = UploadRegistry::default();
        let (stream, _receiver, _runtime) = idle_stream(3);
        let id = registry.next();
        registry.insert_stream(id, stream).expect("the stream");

        let (sender, accepted, size) = registry.stream_handle(id).expect("the stream handle");
        assert_eq!(size, 3);
        assert_eq!(accepted.load(Ordering::Relaxed), 0);
        // The sender is the live producing end, not a copy that would close the stream when dropped.
        assert!(!sender.is_closed());

        // Taking the stream removes it: a second finish has nothing left to consume.
        assert!(registry.take_stream(id).is_ok());
        assert_eq!(
            registry.take_stream(id).unwrap_err(),
            format!("unknown upload stream: {id}")
        );
    }

    #[test]
    fn an_operation_for_an_unknown_stream_is_reported() {
        let registry = UploadRegistry::default();
        assert_eq!(
            registry.stream_handle(99).unwrap_err(),
            "unknown upload stream: 99"
        );
        assert_eq!(
            registry.take_stream(99).unwrap_err(),
            "unknown upload stream: 99"
        );
    }

    #[test]
    fn a_progress_slot_is_created_and_polled_by_its_id() {
        let registry = UploadRegistry::default();
        let id = registry.begin_progress(1_024).expect("a progress slot");

        let progress = registry.progress(id).expect("the progress");
        let snapshot = progress.snapshot();
        assert_eq!(snapshot.total, 1_024);
        assert_eq!(snapshot.sent, 0);

        assert_eq!(
            registry.progress(id + 1).unwrap_err(),
            format!("unknown upload progress: {}", id + 1)
        );
    }

    #[test]
    fn a_finished_upload_resolves_by_the_handle_it_was_given() {
        let registry = UploadRegistry::default();
        let handle = registry.register(uploaded(7)).expect("a handle");

        assert_eq!(registry.get(handle).expect("the upload"), uploaded(7));
        assert_eq!(
            registry.get(handle + 1).unwrap_err(),
            format!("unknown upload handle: {}", handle + 1)
        );
    }

    #[test]
    fn stream_ids_handles_and_progress_slots_share_one_id_space() {
        let registry = UploadRegistry::default();
        let (stream, _receiver, _runtime) = idle_stream(1);
        let stream_id = registry.next();
        registry.insert_stream(stream_id, stream).expect("the stream");
        let handle = registry.register(uploaded(1)).expect("a handle");
        let progress = registry.begin_progress(1).expect("a progress slot");

        assert_ne!(stream_id, handle);
        assert_ne!(stream_id, progress);
        assert_ne!(handle, progress);
    }
}
