//! Typed mapping between rpc frames and the generated types. The generated
//! models carry serde derives (the `RustSerde` extension in gen.sh), and the
//! payloads travel as CBOR - the same Jackson mapping the backend uses, in a
//! compact binary encoding - so each mapping is a serde one-liner.

use serde::de::DeserializeOwned;
use serde::Serialize;
use wirespec_aeron::protocol::RpcFrame;

use crate::gen::model::quote::Quote;
use crate::gen::model::quote_list::QuoteList;
use crate::gen::rpc::get_quote::GetQuote;

#[derive(Serialize)]
struct GetQuoteParams<'a> {
    symbol: &'a str,
}

pub fn to_cbor<T: Serialize>(value: &T) -> Result<Vec<u8>, String> {
    let mut bytes = Vec::new();
    ciborium::into_writer(value, &mut bytes).map_err(|e| e.to_string())?;
    Ok(bytes)
}

pub fn from_cbor<T: DeserializeOwned>(payload: &[u8]) -> Result<T, String> {
    ciborium::from_reader(payload).map_err(|e: ciborium::de::Error<std::io::Error>| e.to_string())
}

pub fn get_quote_params(symbol: &str) -> Result<Vec<u8>, String> {
    to_cbor(&GetQuoteParams { symbol })
}

/// Payload of a request without parameters: no bytes, whatever the encoding.
pub fn empty_params() -> Vec<u8> {
    Vec::new()
}

/// Map a GetQuote response frame onto the generated `GetQuote::Response` enum.
pub fn get_quote_response(frame: &RpcFrame) -> Result<GetQuote::Response, String> {
    match frame {
        RpcFrame::Result { payload, .. } => Ok(GetQuote::Response::Result(GetQuote::Result { value: from_cbor(payload)? })),
        RpcFrame::Error { payload, .. } => Ok(GetQuote::Response::Error(GetQuote::Error { value: from_cbor(payload)? })),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetQuote'".into()),
    }
}

/// Map a GetWatchlistQuotes response frame onto the rpc's result type.
pub fn get_watchlist_quotes_response(frame: &RpcFrame) -> Result<QuoteList, String> {
    match frame {
        RpcFrame::Result { payload, .. } => from_cbor(payload),
        RpcFrame::Error { payload, .. } => Err(format!("Rpc 'GetWatchlistQuotes' failed: {}", String::from_utf8_lossy(payload))),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetWatchlistQuotes'".into()),
    }
}

/// One line for a full quote, identical across the Rust and TypeScript clients
/// (the Docker integration test asserts the same strings from both).
pub fn format_quote(quote: &Quote) -> String {
    let prev = quote
        .previous_close
        .as_ref()
        .map(|money| format!("prev {} {}", money.amount, money.currency))
        .unwrap_or_else(|| "prev n/a".to_string());
    format!(
        "{} {} {} on {} ({}), {}, history {}",
        quote.symbol, quote.last.amount, quote.last.currency, quote.venue.mic, quote.venue.city, prev, quote.history.len(),
    )
}

/// The short line for watchlist listings.
pub fn format_quote_line(quote: &Quote) -> String {
    format!("{} {} {} on {}", quote.symbol, quote.last.amount, quote.last.currency, quote.venue.mic)
}

/// Map a Ping response frame onto the rpc's result type: a plain string, sent as raw UTF-8.
pub fn ping_response(frame: &RpcFrame) -> Result<String, String> {
    match frame {
        RpcFrame::Result { payload, .. } => String::from_utf8(payload.clone()).map_err(|e| e.to_string()),
        RpcFrame::Error { payload, .. } => Err(format!("Rpc 'Ping' failed: {}", String::from_utf8_lossy(payload))),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'Ping'".into()),
    }
}
