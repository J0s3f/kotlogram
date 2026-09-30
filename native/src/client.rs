//! The grammers-backed client, its registry, and peer resolution.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

use grammers_client::client::UpdateStream;
use grammers_client::client::{LoginToken, PasswordToken};
use grammers_client::media::Uploaded;
use grammers_client::peer::Peer;
use grammers_client::Client;
use grammers_mtsender::UpdatesConfiguration;
use grammers_mtsender::{SenderPool, SenderPoolHandle};
use grammers_session::storages::SqliteSession;
use grammers_session::types::PeerRef;
use jni::sys::jlong;
use tokio::runtime::Runtime;

use crate::error::{error, invocation_error};
use crate::payload::{FileSource, PeerTarget};

/// One live Telegram session: its Tokio runtime, its grammers client and its registries.
pub(crate) struct NativeClient {
    pub(crate) runtime: Runtime,
    pub(crate) client: Client,
    pub(crate) sender: SenderPoolHandle,
    pub(crate) updates: Mutex<UpdateStream>,
    pub(crate) session: Arc<SqliteSession>,
    pub(crate) api_hash: String,
    pub(crate) authentication: Mutex<AuthenticationState>,
    pub(crate) peers: Mutex<HashMap<i64, Peer>>,
    pub(crate) next_peer_handle: AtomicI64,
    pub(crate) uploads: UploadRegistry,
}

/// One upload that has begun through `uploadStreamBegin` and has not been finished yet.
///
/// The declared name travels with the bytes because `Client::upload_stream` needs it when the
/// stream is finished, and nothing else remembers it between the begin and the finish call.
#[derive(Debug)]
pub(crate) struct StreamUpload {
    pub(crate) name: String,
    pub(crate) data: Vec<u8>,
}

/// The per-client upload state: the in-progress stream buffers and the uploads that are done.
///
/// The two maps share one id space, drawn from [Self::next_handle], so a stream id is never
/// mistaken for an upload handle. A stream that is still taking chunks lives in [Self::streams];
/// once it is finished its [`Uploaded`] moves to [Self::uploads], where a send resolves it by the
/// handle the finish returned. Both maps live as long as the client and are dropped with it, which
/// is the documented lifetime of a handle. Each map is behind its own mutex, and every lock is held
/// only for the length of one lookup or insert.
#[derive(Default)]
pub(crate) struct UploadRegistry {
    streams: Mutex<HashMap<i64, StreamUpload>>,
    uploads: Mutex<HashMap<i64, Uploaded>>,
    next_handle: AtomicI64,
}

impl UploadRegistry {
    /// The next id, shared by stream ids and upload handles so the two never collide.
    fn next(&self) -> i64 {
        self.next_handle.fetch_add(1, Ordering::Relaxed)
    }

    /// Opens a stream under a fresh id and returns it.
    pub(crate) fn begin(&self, name: String) -> Result<i64, String> {
        let id = self.next();
        self.streams
            .lock()
            .map_err(|_| "upload stream registry is poisoned".to_owned())?
            .insert(
                id,
                StreamUpload {
                    name,
                    data: Vec::new(),
                },
            );
        Ok(id)
    }

    /// Appends [bytes] to the stream [id], or reports an unknown stream.
    pub(crate) fn append(&self, id: i64, bytes: &[u8]) -> Result<(), String> {
        let mut streams = self
            .streams
            .lock()
            .map_err(|_| "upload stream registry is poisoned".to_owned())?;
        let stream = streams
            .get_mut(&id)
            .ok_or_else(|| format!("unknown upload stream: {id}"))?;
        stream.data.extend_from_slice(bytes);
        Ok(())
    }

    /// Removes and returns the stream [id], which is what finishing it consumes.
    pub(crate) fn take(&self, id: i64) -> Result<StreamUpload, String> {
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

    let native = Arc::new(NativeClient {
        runtime,
        client,
        sender,
        updates: Mutex::new(updates),
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
pub(crate) fn close_client(handle: jlong) -> String {
    let result = clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())
        .map(|mut map| map.remove(&handle));
    match result {
        Ok(Some(client)) => {
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
/// A path is uploaded now, exactly as a plain `sendFile` always did; a handle is the finished
/// upload the registry kept, reused without a second upload.
pub(crate) async fn resolve_upload(
    native: &NativeClient,
    source: FileSource,
) -> Result<Uploaded, String> {
    match source {
        FileSource::Path(path) => native
            .client
            .upload_file(PathBuf::from(path))
            .await
            .map_err(error),
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

    #[test]
    fn a_started_stream_keeps_its_name_and_accumulates_its_chunks() {
        let registry = UploadRegistry::default();
        let id = registry.begin("report.pdf".to_owned()).expect("a stream");

        registry.append(id, b"foo").expect("the first chunk");
        registry.append(id, b"bar").expect("the second chunk");

        let stream = registry.take(id).expect("the finished stream");
        assert_eq!(stream.name, "report.pdf");
        assert_eq!(stream.data, b"foobar");
        // Taking the stream removes it: a second finish has nothing left to consume.
        assert_eq!(
            registry.take(id).unwrap_err(),
            format!("unknown upload stream: {id}")
        );
    }

    #[test]
    fn a_chunk_for_an_unknown_stream_is_reported() {
        let registry = UploadRegistry::default();
        assert_eq!(
            registry.append(99, b"x").unwrap_err(),
            "unknown upload stream: 99"
        );
        assert_eq!(registry.take(99).unwrap_err(), "unknown upload stream: 99");
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
    fn stream_ids_and_upload_handles_share_one_id_space() {
        let registry = UploadRegistry::default();
        let stream = registry.begin("a.bin".to_owned()).expect("a stream");
        let handle = registry.register(uploaded(1)).expect("a handle");

        assert_ne!(stream, handle);
    }
}
