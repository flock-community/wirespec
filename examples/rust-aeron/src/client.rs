//! A synchronous Wirespec RPC client over Aeron: requests go out on the shared
//! request stream, responses come back on this client's own reply stream,
//! correlated by id. Attach it to the media driver embedded in the Kotlin
//! Spring Boot backend (../maven-spring-aeron) to talk to it over shared memory.

use std::cell::RefCell;
use std::ffi::CString;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use aeron_rs::aeron::Aeron;
use aeron_rs::concurrent::atomic_buffer::{AlignedBuffer, AtomicBuffer};
use aeron_rs::concurrent::logbuffer::header::Header;
use aeron_rs::context::Context;
use aeron_rs::fragment_assembler::FragmentAssembler;
use aeron_rs::publication::Publication;
use aeron_rs::subscription::Subscription;
use aeron_rs::utils::types::Index;

use crate::protocol::{RpcFrame, DEFAULT_CHANNEL, DEFAULT_REPLY_STREAM_ID, DEFAULT_REQUEST_STREAM_ID};

pub struct AeronRpcClient {
    _aeron: Aeron,
    publication: Arc<Mutex<Publication>>,
    subscription: Arc<Mutex<Subscription>>,
    reply_stream_id: i32,
    next_correlation_id: i64,
}

impl AeronRpcClient {
    pub fn connect(aeron_dir: &str) -> Result<Self, String> {
        Self::connect_streams(aeron_dir, DEFAULT_REQUEST_STREAM_ID, DEFAULT_REPLY_STREAM_ID)
    }

    pub fn connect_streams(aeron_dir: &str, request_stream_id: i32, reply_stream_id: i32) -> Result<Self, String> {
        let mut context = Context::new();
        context.set_aeron_dir(aeron_dir.to_string());
        context.set_error_handler(|error| eprintln!("Aeron error: {error:?}"));
        let mut aeron = Aeron::new(context).map_err(|e| format!("Cannot attach to media driver at '{aeron_dir}': {e:?}"))?;

        let channel = CString::new(DEFAULT_CHANNEL).unwrap();
        let publication_id = aeron.add_publication(channel.clone(), request_stream_id).map_err(|e| format!("{e:?}"))?;
        let subscription_id = aeron.add_subscription(channel, reply_stream_id).map_err(|e| format!("{e:?}"))?;
        let publication = loop {
            if let Ok(publication) = aeron.find_publication(publication_id) {
                break publication;
            }
            std::thread::yield_now();
        };
        let subscription = loop {
            if let Ok(subscription) = aeron.find_subscription(subscription_id) {
                break subscription;
            }
            std::thread::yield_now();
        };

        Ok(Self {
            _aeron: aeron,
            publication,
            subscription,
            reply_stream_id,
            next_correlation_id: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_nanos() as i64)
                .unwrap_or_default(),
        })
    }

    /// Send one request and block for its RESULT or ERROR frame.
    pub fn call(&mut self, method: &str, payload: Vec<u8>, timeout: Duration) -> Result<RpcFrame, String> {
        self.next_correlation_id += 1;
        let correlation_id = self.next_correlation_id;
        let request = RpcFrame::Request {
            correlation_id,
            method: method.to_string(),
            reply_channel: DEFAULT_CHANNEL.to_string(),
            reply_stream_id: self.reply_stream_id,
            payload,
        };
        let deadline = Instant::now() + timeout;
        self.offer(&request.encode(), deadline)?;
        self.await_response(correlation_id, deadline, method)
    }

    fn offer(&self, bytes: &[u8], deadline: Instant) -> Result<(), String> {
        let aligned = AlignedBuffer::with_capacity(bytes.len() as Index);
        let buffer = AtomicBuffer::from_aligned(&aligned);
        buffer.put_bytes(0, bytes);
        loop {
            match self.publication.lock().unwrap().offer_part(buffer, 0, bytes.len() as Index) {
                Ok(position) if position > 0 => return Ok(()),
                _ if Instant::now() >= deadline => return Err("Timed out offering request to Aeron".into()),
                _ => std::thread::sleep(Duration::from_millis(1)),
            }
        }
    }

    fn await_response(&self, correlation_id: i64, deadline: Instant, method: &str) -> Result<RpcFrame, String> {
        let response: RefCell<Option<RpcFrame>> = RefCell::new(None);
        let mut on_message = |buffer: &AtomicBuffer, offset: Index, length: Index, _header: &Header| {
            if response.borrow().is_some() {
                return;
            }
            // Other clients may share this reply stream; keep only our correlation id.
            if let Ok(frame) = RpcFrame::decode(buffer.as_sub_slice(offset, length)) {
                if frame.correlation_id() == correlation_id {
                    *response.borrow_mut() = Some(frame);
                }
            }
        };
        let mut assembler = FragmentAssembler::new(&mut on_message, None);
        let mut handler = assembler.handler();
        loop {
            self.subscription.lock().unwrap().poll(&mut handler, 10);
            if let Some(frame) = response.borrow_mut().take() {
                return Ok(frame);
            }
            if Instant::now() >= deadline {
                return Err(format!("Timed out waiting for a response to rpc '{method}'"));
            }
            std::thread::sleep(Duration::from_micros(100));
        }
    }
}
