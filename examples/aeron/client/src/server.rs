//! The serving side of the client: the Kotlin backend calls rpcs here, in the
//! server-to-client direction. `TraderTerminal` implements the generated
//! `GetWatchlist::Service` trait, and `serve_watchlist` answers requests for it
//! on its own Aeron stream, mirroring the Kotlin `AeronRpcServer`.

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
use aeron_rs::utils::types::Index;

use crate::gen::model::watchlist::Watchlist;
use crate::gen::rpc::get_watchlist::GetWatchlist;
use crate::protocol::{RpcFrame, DEFAULT_CHANNEL};
use crate::serialization::watchlist_to_payload;

pub struct TraderTerminal;

impl GetWatchlist::Service for TraderTerminal {
    async fn get_watchlist() -> Watchlist {
        Watchlist {
            symbols: vec!["AAPL".into(), "FLCK".into()],
        }
    }
}

/// The generated rpc Service methods are `async`; their bodies here never
/// suspend, so a minimal polling executor bridges them into this sync server.
fn block_on<F: Future>(future: F) -> F::Output {
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

/// Serve GetWatchlist on `stream_id` until `running` clears, on a dedicated
/// Aeron connection. Signals `ready` once the subscription is in place.
pub fn serve_watchlist(aeron_dir: &str, stream_id: i32, running: Arc<AtomicBool>, ready: Sender<()>) -> Result<(), String> {
    let mut context = Context::new();
    context.set_aeron_dir(aeron_dir.to_string());
    context.set_error_handler(|error| eprintln!("Aeron error: {error:?}"));
    let mut aeron = Aeron::new(context).map_err(|e| format!("{e:?}"))?;

    let channel = CString::new(DEFAULT_CHANNEL).unwrap();
    let subscription_id = aeron.add_subscription(channel, stream_id).map_err(|e| format!("{e:?}"))?;
    let subscription = loop {
        if let Ok(subscription) = aeron.find_subscription(subscription_id) {
            break subscription;
        }
        std::thread::yield_now();
    };
    let _ = ready.send(());

    let responses: RefCell<Vec<FramedReply>> = RefCell::new(Vec::new());
    let mut on_request = |buffer: &AtomicBuffer, offset: Index, length: Index, _header: &Header| {
        let Ok(RpcFrame::Request { correlation_id, method, reply_channel, reply_stream_id, .. }) =
            RpcFrame::decode(buffer.as_sub_slice(offset, length))
        else {
            return;
        };
        println!("Serving {method} for the backend");
        let frame = match method.as_str() {
            "GetWatchlist" => RpcFrame::Result {
                correlation_id,
                method,
                payload: watchlist_to_payload(&block_on(<TraderTerminal as GetWatchlist::Service>::get_watchlist())),
            },
            _ => RpcFrame::Error {
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
            if let Err(error) = publish(&mut aeron, &mut publications, &framed) {
                eprintln!("Failed to answer {}: {error}", framed.frame.correlation_id());
            }
        }
        std::thread::sleep(Duration::from_micros(100));
    }
    Ok(())
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
            let publication = loop {
                if let Ok(publication) = aeron.find_publication(publication_id) {
                    break publication;
                }
                std::thread::yield_now();
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
