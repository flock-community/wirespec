//! Typed mapping between rpc frames and the generated types. The generated
//! models carry serde derives (the `RustSerde` extension in gen.sh), so each
//! mapping is a serde one-liner.

use serde_json::json;
use wirespec_aeron::protocol::RpcFrame;

use crate::gen::model::quote_list::QuoteList;
use crate::gen::rpc::get_quote::GetQuote;

pub fn get_quote_params(symbol: &str) -> Vec<u8> {
    json!({ "symbol": symbol }).to_string().into_bytes()
}

pub fn empty_params() -> Vec<u8> {
    b"{}".to_vec()
}

/// Map a GetQuote response frame onto the generated `GetQuote::Response` enum.
pub fn get_quote_response(frame: &RpcFrame) -> Result<GetQuote::Response, String> {
    match frame {
        RpcFrame::Result { payload, .. } => Ok(GetQuote::Response::Result(GetQuote::Result {
            value: serde_json::from_slice(payload).map_err(|e| e.to_string())?,
        })),
        RpcFrame::Error { payload, .. } => Ok(GetQuote::Response::Error(GetQuote::Error {
            value: serde_json::from_slice(payload).map_err(|e| e.to_string())?,
        })),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetQuote'".into()),
    }
}

/// Map a GetWatchlistQuotes response frame onto the rpc's result type.
pub fn get_watchlist_quotes_response(frame: &RpcFrame) -> Result<QuoteList, String> {
    match frame {
        RpcFrame::Result { payload, .. } => serde_json::from_slice(payload).map_err(|e| e.to_string()),
        RpcFrame::Error { payload, .. } => Err(format!("Rpc 'GetWatchlistQuotes' failed: {}", String::from_utf8_lossy(payload))),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetWatchlistQuotes'".into()),
    }
}

/// Map a Ping response frame onto the rpc's result type: a plain string, sent as raw UTF-8.
pub fn ping_response(frame: &RpcFrame) -> Result<String, String> {
    match frame {
        RpcFrame::Result { payload, .. } => String::from_utf8(payload.clone()).map_err(|e| e.to_string()),
        RpcFrame::Error { payload, .. } => Err(format!("Rpc 'Ping' failed: {}", String::from_utf8_lossy(payload))),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'Ping'".into()),
    }
}
