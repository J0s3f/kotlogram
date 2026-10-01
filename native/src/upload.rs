//! Incremental upload plumbing: the progress counter every upload reports through, a bounded
//! channel reader the chunked upload feeds, and a counting reader the path uploads wrap their file
//! in.
//!
//! grammers' `Client::upload_stream` reads its source one [`MAX_CHUNK_SIZE`](grammers' 512 KiB)
//! part at a time, so the source is the natural place to observe an upload: nothing is buffered
//! beyond what grammers is about to send. A streamed upload therefore hands grammers a reader over
//! a bounded channel instead of a `Cursor<Vec<u8>>` holding the whole file, and both that reader
//! and the file reader of a path upload count the bytes they hand over into one [`UploadProgress`].

use std::io;
use std::pin::Pin;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::task::{Context, Poll};
use std::time::Instant;

use serde::Serialize;
use tokio::io::{AsyncRead, ReadBuf};
use tokio::sync::mpsc;

/// How many chunks may wait in a streamed upload's channel before the producer is throttled.
///
/// The bound, not the file size, is what the upload retains: the producer's `send` awaits room,
/// so at most this many chunks plus the one grammers is filling its part from are held at a time.
pub(crate) const STREAM_CHANNEL_CAPACITY: usize = 4;

/// The live progress of one upload: how many bytes have been handed to grammers out of how many
/// were declared, and when the upload started.
///
/// `total` is an atomic because a path upload only learns the file's length when it opens it, after
/// the progress slot was allocated. All counters are relaxed: they are a progress readout, not a
/// synchronisation primitive.
#[derive(Debug)]
pub(crate) struct UploadProgress {
    total: AtomicU64,
    sent: AtomicU64,
    started: Instant,
}

/// The snapshot `uploadProgress` answers: bytes sent and declared, elapsed time and average rate.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UploadProgressDto {
    pub(crate) sent: u64,
    pub(crate) total: u64,
    pub(crate) elapsed_millis: u64,
    /// Average bytes per second since the upload started; zero before any time has passed.
    pub(crate) bytes_per_second: f64,
}

impl UploadProgress {
    /// A progress counter for an upload of [total] bytes, starting now.
    pub(crate) fn new(total: u64) -> Arc<Self> {
        Self::starting_at(total, Instant::now())
    }

    /// A progress counter with an explicit start, so rate arithmetic is deterministic in tests.
    pub(crate) fn starting_at(total: u64, started: Instant) -> Arc<Self> {
        Arc::new(Self {
            total: AtomicU64::new(total),
            sent: AtomicU64::new(0),
            started,
        })
    }

    /// Records the total once it is known, which a path upload does after opening the file.
    pub(crate) fn set_total(&self, total: u64) {
        self.total.store(total, Ordering::Relaxed);
    }

    /// Adds [bytes] to the sent count; the readers call this as they hand data to grammers.
    fn add(&self, bytes: u64) {
        self.sent.fetch_add(bytes, Ordering::Relaxed);
    }

    /// The readout a poll returns.
    pub(crate) fn snapshot(&self) -> UploadProgressDto {
        let sent = self.sent.load(Ordering::Relaxed);
        let total = self.total.load(Ordering::Relaxed);
        let elapsed = self.started.elapsed();
        let seconds = elapsed.as_secs_f64();
        UploadProgressDto {
            sent,
            total,
            elapsed_millis: elapsed.as_millis() as u64,
            bytes_per_second: if seconds > 0.0 {
                sent as f64 / seconds
            } else {
                0.0
            },
        }
    }
}

/// The receiving end of a streamed upload's channel, presented as an async byte source.
///
/// A chunk may be shorter than the read buffer, so the leftover is kept until it is handed over;
/// the reader therefore retains at most one chunk at a time, whatever the total upload is. An
/// empty chunk is skipped rather than answered as end-of-file.
pub(crate) struct ChannelReader {
    receiver: mpsc::Receiver<Vec<u8>>,
    current: Vec<u8>,
    position: usize,
}

impl ChannelReader {
    pub(crate) fn new(receiver: mpsc::Receiver<Vec<u8>>) -> Self {
        Self {
            receiver,
            current: Vec::new(),
            position: 0,
        }
    }

    /// How many bytes are buffered but not yet handed over; used to assert the bound in tests.
    #[cfg(test)]
    pub(crate) fn retained(&self) -> usize {
        self.current.len() - self.position
    }
}

impl AsyncRead for ChannelReader {
    fn poll_read(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<io::Result<()>> {
        let this = self.get_mut();
        loop {
            if this.position < this.current.len() {
                let available = &this.current[this.position..];
                let take = available.len().min(buf.remaining());
                buf.put_slice(&available[..take]);
                this.position += take;
                if this.position == this.current.len() {
                    this.current.clear();
                    this.position = 0;
                }
                return Poll::Ready(Ok(()));
            }
            match this.receiver.poll_recv(cx) {
                Poll::Ready(Some(chunk)) => {
                    if chunk.is_empty() {
                        continue;
                    }
                    this.current = chunk;
                    this.position = 0;
                }
                // The sender was dropped: every byte has been delivered, so this is end-of-file.
                Poll::Ready(None) => return Poll::Ready(Ok(())),
                Poll::Pending => return Poll::Pending,
            }
        }
    }
}

/// Wraps a reader so every byte it hands to grammers is counted into [UploadProgress].
pub(crate) struct ProgressReader<R> {
    inner: R,
    progress: Arc<UploadProgress>,
}

impl<R> ProgressReader<R> {
    pub(crate) fn new(inner: R, progress: Arc<UploadProgress>) -> Self {
        Self { inner, progress }
    }
}

impl<R: AsyncRead + Unpin> AsyncRead for ProgressReader<R> {
    fn poll_read(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<io::Result<()>> {
        let this = self.get_mut();
        let before = buf.filled().len();
        let result = Pin::new(&mut this.inner).poll_read(cx, buf);
        if let Poll::Ready(Ok(())) = result {
            let read = buf.filled().len() - before;
            if read > 0 {
                this.progress.add(read as u64);
            }
        }
        result
    }
}

/// A bounded chunk channel and its reader, the pair a streamed upload is built from.
pub(crate) fn stream_channel() -> (mpsc::Sender<Vec<u8>>, ChannelReader) {
    let (sender, receiver) = mpsc::channel(STREAM_CHANNEL_CAPACITY);
    (sender, ChannelReader::new(receiver))
}

#[cfg(test)]
mod tests {
    use std::io::Cursor;
    use std::time::Duration;

    use tokio::io::AsyncReadExt;

    use super::*;

    fn runtime() -> tokio::runtime::Runtime {
        tokio::runtime::Builder::new_current_thread()
            .build()
            .expect("a test runtime")
    }

    #[test]
    fn the_channel_reader_yields_what_was_sent_then_ends() {
        let runtime = runtime();
        runtime.block_on(async {
            let (sender, mut reader) = stream_channel();
            sender.send(b"hello".to_vec()).await.expect("a chunk");
            sender.send(b" world".to_vec()).await.expect("a chunk");
            drop(sender);

            let mut read = Vec::new();
            reader.read_to_end(&mut read).await.expect("the stream reads");
            assert_eq!(read, b"hello world");
        });
    }

    #[test]
    fn the_channel_reader_keeps_a_partial_chunk_for_the_next_read() {
        let runtime = runtime();
        runtime.block_on(async {
            let (sender, mut reader) = stream_channel();
            sender.send(b"abcdef".to_vec()).await.expect("a chunk");
            drop(sender);

            // Read three bytes at a time: a chunk larger than the buffer is handed over in pieces
            // without ever losing or duplicating one.
            let mut collected = Vec::new();
            let mut buffer = [0u8; 3];
            loop {
                let read = reader.read(&mut buffer).await.expect("the stream reads");
                if read == 0 {
                    break;
                }
                collected.extend_from_slice(&buffer[..read]);
                assert!(reader.retained() <= 3, "the reader kept more than the remainder");
            }
            assert_eq!(collected, b"abcdef");
        });
    }

    #[test]
    fn the_channel_is_bounded_so_a_large_stream_does_not_accumulate() {
        // The point of the channel: capacity is a constant, so a producer that outruns the uploader
        // is throttled instead of the file piling up in memory.
        let (sender, _reader) = stream_channel();
        for index in 0..STREAM_CHANNEL_CAPACITY {
            sender
                .try_send(vec![index as u8])
                .expect("a chunk fits in the bound");
        }
        assert!(
            sender.try_send(vec![0xff]).is_err(),
            "the channel must not grow past its capacity"
        );
    }

    #[test]
    fn the_progress_reader_counts_every_byte_it_hands_over() {
        let runtime = runtime();
        let progress = UploadProgress::new(6);
        let mut reader = ProgressReader::new(
            Cursor::new(b"foobar".to_vec()),
            Arc::clone(&progress),
        );

        let mut read = Vec::new();
        runtime
            .block_on(reader.read_to_end(&mut read))
            .expect("the reader reads");

        assert_eq!(read, b"foobar");
        let snapshot = progress.snapshot();
        assert_eq!(snapshot.sent, 6);
        assert_eq!(snapshot.total, 6);
    }

    #[test]
    fn progress_reports_the_total_it_is_told_and_a_rate_once_bytes_move() {
        let started = Instant::now() - Duration::from_secs(2);
        let progress = UploadProgress::starting_at(0, started);
        assert_eq!(progress.snapshot().total, 0);
        progress.set_total(1_000);

        let reader_progress = Arc::clone(&progress);
        let runtime = runtime();
        let mut reader = ProgressReader::new(Cursor::new(vec![7u8; 1_000]), reader_progress);
        let mut read = Vec::new();
        runtime
            .block_on(reader.read_to_end(&mut read))
            .expect("the reader reads");

        let snapshot = progress.snapshot();
        assert_eq!(snapshot.total, 1_000);
        assert_eq!(snapshot.sent, 1_000);
        assert!(snapshot.elapsed_millis >= 2_000);
        // Averaged over at least two seconds, the rate cannot exceed 500 B/s for 1 000 bytes.
        assert!(snapshot.bytes_per_second > 0.0);
        assert!(snapshot.bytes_per_second < 1_000.0);
    }
}
