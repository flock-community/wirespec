//! Serves Wirespec rpc definitions over Aeron: bind one handler per rpc
//! method, then [AeronRpcServer::serve] on the stream the callers publish to.
//! Each response is published on the reply channel and stream the request
//! names, mirroring the Kotlin `AeronRpcServer` of the `aeron-jvm` integration.

use std::cell::RefCell;
use std::collections::HashMap;
use std::ffi::CString;
use std::future::Future;
use std::pin::pin;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::Sender;
use std::sync::{Arc, Mutex};
use std::task::{Context as TaskContext, Poll, Waker};
use std::time::{Duration, Instant};

use aeron_rs::aeron::Aeron;
use aeron_rs::concurrent::atomic_buffer::{AlignedBuffer, AtomicBuffer};
use aeron_rs::concurrent::logbuffer::header::Header;
use aeron_rs::context::Context;
use aeron_rs::fragment_assembler::FragmentAssembler;
use aeron_rs::publication::Publication;
use aeron_rs::subscription::Subscription;
use aeron_rs::utils::types::Index;

use crate::protocol::{RpcFrame, DEFAULT_CHANNEL};

/// Answers a request payload with the rpc's result (`Ok`) or error (`Err`) payload.
pub type Handler = Box<dyn FnMut(&[u8]) -> Result<Vec<u8>, Vec<u8>>>;

/// The generated rpc Service methods are `async`; a body that never suspends
/// bridges into a sync handler through this minimal polling executor.
pub fn block_on<F: Future>(future: F) -> F::Output {
    let mut context = TaskContext::from_waker(Waker::noop());
    let mut future = pin!(future);
    loop {
        match future.as_mut().poll(&mut context) {
            Poll::Ready(value) => return value,
            Poll::Pending => std::thread::yield_now(),
        }
    }
}

struct FramedReply {
    reply_channel: String,
    reply_stream_id: i32,
    frame: RpcFrame,
}

#[derive(Default)]
pub struct AeronRpcServer {
    handlers: HashMap<String, Handler>,
}

impl AeronRpcServer {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn bind(mut self, method: &str, handler: impl FnMut(&[u8]) -> Result<Vec<u8>, Vec<u8>> + 'static) -> Self {
        self.handlers.insert(method.to_string(), Box::new(handler));
        self
    }

    /// Serve on `aeron:ipc` - see [Self::serve_channel] for the network.
    pub fn serve(self, aeron_dir: &str, stream_id: i32, running: Arc<AtomicBool>, ready: Sender<()>) -> Result<(), String> {
        self.serve_channel(aeron_dir, DEFAULT_CHANNEL, stream_id, running, ready)
    }

    /// Serve on `channel`/`stream_id` (e.g. `aeron:udp?endpoint=host:port`)
    /// until `running` clears, on a dedicated Aeron connection. Signals `ready`
    /// once the subscription is in place.
    pub fn serve_channel(self, aeron_dir: &str, channel: &str, stream_id: i32, running: Arc<AtomicBool>, ready: Sender<()>) -> Result<(), String> {
        let mut context = Context::new();
        context.set_aeron_dir(aeron_dir.to_string());
        context.set_error_handler(|error| eprintln!("Aeron error: {error:?}"));
        let mut aeron = Aeron::new(context).map_err(|e| format!("{e:?}"))?;

        let subscription_channel = CString::new(channel).map_err(|e| e.to_string())?;
        let subscription_id = aeron.add_subscription(subscription_channel, stream_id).map_err(|e| format!("{e:?}"))?;
        let subscription: Arc<Mutex<Subscription>> = loop {
            if let Ok(subscription) = aeron.find_subscription(subscription_id) {
                break subscription;
            }
            std::thread::yield_now();
        };
        let _ = ready.send(());

        let handlers = RefCell::new(self.handlers);
        let responses: RefCell<Vec<FramedReply>> = RefCell::new(Vec::new());
        let mut on_request = |buffer: &AtomicBuffer, offset: Index, length: Index, _header: &Header| {
            let Ok(RpcFrame::Request { correlation_id, method, reply_channel, reply_stream_id, payload }) =
                RpcFrame::decode(buffer.as_sub_slice(offset, length))
            else {
                return;
            };
            let frame = match handlers.borrow_mut().get_mut(&method) {
                Some(handler) => match handler(&payload) {
                    Ok(payload) => RpcFrame::Result { correlation_id, method, payload },
                    Err(payload) => RpcFrame::Error { correlation_id, method, payload },
                },
                None => RpcFrame::Error {
                    correlation_id,
                    method: method.clone(),
                    payload: format!("Unknown rpc method: {method}").into_bytes(),
                },
            };
            responses.borrow_mut().push(FramedReply { reply_channel, reply_stream_id, frame });
        };
        let mut assembler = FragmentAssembler::new(&mut on_request, None);
        let mut handler = assembler.handler();

        let mut publications: HashMap<(String, i32), Arc<Mutex<Publication>>> = HashMap::new();
        while running.load(Ordering::SeqCst) {
            subscription.lock().unwrap().poll(&mut handler, 10);
            // Publications happen outside the poll closure: offering needs `aeron` mutably.
            let pending: Vec<FramedReply> = responses.borrow_mut().drain(..).collect();
            for framed in pending {
                match publish(&mut aeron, &mut publications, &framed) {
                    Ok(()) => println!("Answered {} on {}", framed.frame.correlation_id(), framed.reply_channel),
                    Err(error) => eprintln!("Failed to answer {}: {error}", framed.frame.correlation_id()),
                }
            }
            std::thread::sleep(Duration::from_micros(100));
        }
        Ok(())
    }
}

fn publish(
    aeron: &mut Aeron,
    publications: &mut HashMap<(String, i32), Arc<Mutex<Publication>>>,
    framed: &FramedReply,
) -> Result<(), String> {
    let key = (framed.reply_channel.clone(), framed.reply_stream_id);
    let publication = match publications.get(&key) {
        Some(publication) => publication.clone(),
        None => {
            let channel = CString::new(framed.reply_channel.as_str()).map_err(|e| e.to_string())?;
            let publication_id = aeron.add_publication(channel, framed.reply_stream_id).map_err(|e| format!("{e:?}"))?;
            // Bounded: the driver can reject the publication (e.g. /dev/shm
            // exhaustion), which surfaces as find_publication failing forever.
            let deadline = Instant::now() + Duration::from_secs(10);
            let publication = loop {
                match aeron.find_publication(publication_id) {
                    Ok(publication) => break publication,
                    Err(error) if Instant::now() >= deadline => return Err(format!("Reply publication not available: {error:?}")),
                    Err(_) => std::thread::yield_now(),
                }
            };
            publications.insert(key, publication.clone());
            publication
        }
    };
    let bytes = framed.frame.encode();
    let aligned = AlignedBuffer::with_capacity(bytes.len() as Index);
    let buffer = AtomicBuffer::from_aligned(&aligned);
    buffer.put_bytes(0, &bytes);
    let deadline = Instant::now() + Duration::from_secs(10);
    loop {
        match publication.lock().unwrap().offer_part(buffer, 0, bytes.len() as Index) {
            Ok(position) if position > 0 => return Ok(()),
            _ if Instant::now() >= deadline => return Err("Timed out offering response".into()),
            _ => std::thread::sleep(Duration::from_millis(1)),
        }
    }
}
