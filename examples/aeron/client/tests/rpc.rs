use wirespec_aeron::protocol::RpcFrame;
use wirespec_aeron_client::gen::model::quote::Quote;
use wirespec_aeron_client::gen::model::quote_error::QuoteError;
use wirespec_aeron_client::gen::rpc::get_quote::GetQuote;
use wirespec_aeron_client::rpc::{from_cbor, get_quote_response, ping_response, to_cbor};

#[test]
fn generated_models_round_trip_through_cbor() {
    let quote = Quote { symbol: "FLCK".into(), price: 42.0, currency: "EUR".into() };
    let bytes = to_cbor(&quote).unwrap();

    // Binary on the wire: a CBOR map, not JSON text.
    assert_ne!(b'{', bytes[0]);
    assert_eq!(quote, from_cbor(&bytes).unwrap());
    assert!(quote.validate().is_empty());
}

#[test]
fn result_frame_maps_to_the_generated_response_enum() {
    let quote = Quote { symbol: "AAPL".into(), price: 1.5, currency: "USD".into() };
    let frame = RpcFrame::Result { correlation_id: 42, method: "GetQuote".into(), payload: to_cbor(&quote).unwrap() };

    let expected = GetQuote::Response::Result(GetQuote::Result { value: quote });
    assert_eq!(expected, get_quote_response(&frame).unwrap());
}

#[test]
fn error_frame_maps_to_the_generated_response_enum() {
    let quote_error = QuoteError { code: "UNKNOWN_SYMBOL".into(), message: "nope".into() };
    let frame = RpcFrame::Error { correlation_id: 42, method: "GetQuote".into(), payload: to_cbor(&quote_error).unwrap() };

    let expected = GetQuote::Response::Error(GetQuote::Error { value: quote_error });
    assert_eq!(expected, get_quote_response(&frame).unwrap());
}

#[test]
fn plain_string_results_are_raw_utf8() {
    let frame = RpcFrame::Result { correlation_id: 1, method: "Ping".into(), payload: b"pong".to_vec() };
    assert_eq!("pong", ping_response(&frame).unwrap());
}
