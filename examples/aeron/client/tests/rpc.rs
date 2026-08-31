use wirespec_aeron::protocol::RpcFrame;
use wirespec_aeron_client::gen::model::quote::Quote;
use wirespec_aeron_client::gen::model::quote_error::QuoteError;
use wirespec_aeron_client::gen::rpc::get_quote::GetQuote;
use wirespec_aeron_client::rpc::{get_quote_response, ping_response};

fn result_frame(payload: &str) -> RpcFrame {
    RpcFrame::Result { correlation_id: 42, method: "GetQuote".into(), payload: payload.as_bytes().to_vec() }
}

#[test]
fn generated_models_round_trip_through_serde() {
    let quote = Quote { symbol: "FLCK".into(), price: 42.0, currency: "EUR".into() };
    let json = serde_json::to_string(&quote).unwrap();

    assert_eq!(r#"{"symbol":"FLCK","price":42.0,"currency":"EUR"}"#, json);
    assert_eq!(quote, serde_json::from_str(&json).unwrap());
    assert!(quote.validate().is_empty());
}

#[test]
fn result_frame_maps_to_the_generated_response_enum() {
    let frame = result_frame(r#"{"symbol":"AAPL","price":1.5,"currency":"USD"}"#);
    let expected = GetQuote::Response::Result(GetQuote::Result {
        value: Quote { symbol: "AAPL".into(), price: 1.5, currency: "USD".into() },
    });
    assert_eq!(expected, get_quote_response(&frame).unwrap());
}

#[test]
fn error_frame_maps_to_the_generated_response_enum() {
    let frame = RpcFrame::Error {
        correlation_id: 42,
        method: "GetQuote".into(),
        payload: br#"{"code":"UNKNOWN_SYMBOL","message":"nope"}"#.to_vec(),
    };
    let expected = GetQuote::Response::Error(GetQuote::Error {
        value: QuoteError { code: "UNKNOWN_SYMBOL".into(), message: "nope".into() },
    });
    assert_eq!(expected, get_quote_response(&frame).unwrap());
}

#[test]
fn plain_string_results_are_raw_utf8() {
    let frame = RpcFrame::Result { correlation_id: 1, method: "Ping".into(), payload: b"pong".to_vec() };
    assert_eq!("pong", ping_response(&frame).unwrap());
}
