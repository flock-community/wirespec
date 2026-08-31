//! JSON (de)serialization between the Wirespec generated models and the RPC
//! payloads, following the Wirespec body serialization contract: JSON values,
//! with plain string bodies as raw UTF-8 text.

use crate::gen::model::quote::Quote;
use crate::gen::model::quote_error::QuoteError;
use crate::gen::model::quote_list::QuoteList;
use crate::gen::model::watchlist::Watchlist;
use crate::gen::rpc::get_quote::GetQuote;
use crate::protocol::RpcFrame;
use serde_json::{json, Value};

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
            value: json_to_quote(&parse(payload)?)?,
        })),
        RpcFrame::Error { payload, .. } => Ok(GetQuote::Response::Error(GetQuote::Error {
            value: json_to_quote_error(&parse(payload)?)?,
        })),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetQuote'".into()),
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

/// Map a GetWatchlistQuotes response frame onto the rpc's result type.
pub fn get_watchlist_quotes_response(frame: &RpcFrame) -> Result<QuoteList, String> {
    match frame {
        RpcFrame::Result { payload, .. } => json_to_quote_list(&parse(payload)?),
        RpcFrame::Error { payload, .. } => Err(format!("Rpc 'GetWatchlistQuotes' failed: {}", String::from_utf8_lossy(payload))),
        RpcFrame::Request { .. } => Err("Unexpected REQUEST frame for rpc 'GetWatchlistQuotes'".into()),
    }
}

/// Serialize the client-served GetWatchlist result: the payload of its RESULT frame.
pub fn watchlist_to_payload(watchlist: &Watchlist) -> Vec<u8> {
    json!({ "symbols": watchlist.symbols }).to_string().into_bytes()
}

pub fn json_to_quote_list(value: &Value) -> Result<QuoteList, String> {
    Ok(QuoteList {
        quotes: value
            .get("quotes")
            .and_then(Value::as_array)
            .ok_or("QuoteList is missing 'quotes'")?
            .iter()
            .map(json_to_quote)
            .collect::<Result<_, _>>()?,
    })
}

pub fn json_to_quote(value: &Value) -> Result<Quote, String> {
    Ok(Quote {
        symbol: string_field(value, "symbol")?,
        price: value.get("price").and_then(Value::as_f64).ok_or("Quote is missing 'price'")?,
        currency: string_field(value, "currency")?,
    })
}

pub fn json_to_quote_error(value: &Value) -> Result<QuoteError, String> {
    Ok(QuoteError {
        code: string_field(value, "code")?,
        message: string_field(value, "message")?,
    })
}

fn parse(payload: &[u8]) -> Result<Value, String> {
    serde_json::from_slice(payload).map_err(|e| e.to_string())
}

fn string_field(value: &Value, field: &str) -> Result<String, String> {
    value
        .get(field)
        .and_then(Value::as_str)
        .map(str::to_string)
        .ok_or(format!("Missing string field '{field}'"))
}
