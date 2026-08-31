use wirespec_aeron::protocol::RpcFrame;

// These hex vectors are shared verbatim with the Kotlin integration
// (src/integration/aeron RpcFrameTest); a change here is a wire protocol change.
const GOLDEN_REQUEST: &str = "01012a00000000000000080047657451756f746509006165726f6e3a697063ea030000110000007b2273796d626f6c223a224141504c227d";
const GOLDEN_RESULT: &str = "01022a00000000000000080047657451756f74652e0000007b2273796d626f6c223a224141504c222c227072696365223a312e352c2263757272656e6379223a22555344227d";
const GOLDEN_ERROR: &str = "01032a00000000000000080047657451756f74652a0000007b22636f6465223a22554e4b4e4f574e5f53594d424f4c222c226d657373616765223a226e6f7065227d";

fn from_hex(hex: &str) -> Vec<u8> {
    (0..hex.len()).step_by(2).map(|i| u8::from_str_radix(&hex[i..i + 2], 16).unwrap()).collect()
}

fn to_hex(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

#[test]
fn request_encodes_to_the_golden_wire_format() {
    let frame = RpcFrame::Request {
        correlation_id: 42,
        method: "GetQuote".into(),
        reply_channel: "aeron:ipc".into(),
        reply_stream_id: 1002,
        payload: br#"{"symbol":"AAPL"}"#.to_vec(),
    };
    assert_eq!(GOLDEN_REQUEST, to_hex(&frame.encode()));
}

#[test]
fn frames_round_trip() {
    for hex in [GOLDEN_REQUEST, GOLDEN_RESULT, GOLDEN_ERROR] {
        let frame = RpcFrame::decode(&from_hex(hex)).unwrap();
        assert_eq!(42, frame.correlation_id());
        assert_eq!(hex, to_hex(&frame.encode()));
    }
}

#[test]
fn truncated_frames_are_rejected() {
    assert!(RpcFrame::decode(&from_hex(GOLDEN_REQUEST)[..20]).is_err());
    assert!(RpcFrame::decode(&[2, 1]).is_err());
}
