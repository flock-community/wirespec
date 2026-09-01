import assert from "node:assert/strict";
import { test } from "node:test";

import { decodeRpcFrame, encodeRpcFrame } from "../src/protocol";

// These hex vectors are shared verbatim with the Kotlin integration
// (src/integration/aeron RpcFrameTest) and the wirespec-aeron Rust crate;
// a change here is a wire protocol change.
const GOLDEN_REQUEST =
  "01012a00000000000000080047657451756f746509006165726f6e3a697063ea030000110000007b2273796d626f6c223a224141504c227d";
const GOLDEN_RESULT =
  "01022a00000000000000080047657451756f74652e0000007b2273796d626f6c223a224141504c222c227072696365223a312e352c2263757272656e6379223a22555344227d";
const GOLDEN_ERROR =
  "01032a00000000000000080047657451756f74652a0000007b22636f6465223a22554e4b4e4f574e5f53594d424f4c222c226d657373616765223a226e6f7065227d";

const fromHex = (hex: string): Uint8Array => Uint8Array.from(Buffer.from(hex, "hex"));
const toHex = (bytes: Uint8Array): string => Buffer.from(bytes).toString("hex");

test("request encodes to the golden wire format", () => {
  const encoded = encodeRpcFrame({
    kind: "request",
    correlationId: 42n,
    method: "GetQuote",
    replyChannel: "aeron:ipc",
    replyStreamId: 1002,
    payload: new TextEncoder().encode('{"symbol":"AAPL"}'),
  });
  assert.equal(toHex(encoded), GOLDEN_REQUEST);
});

test("frames round trip", () => {
  for (const hex of [GOLDEN_REQUEST, GOLDEN_RESULT, GOLDEN_ERROR]) {
    const frame = decodeRpcFrame(fromHex(hex));
    assert.equal(frame.correlationId, 42n);
    assert.equal(frame.method, "GetQuote");
    assert.equal(toHex(encodeRpcFrame(frame)), hex);
  }
});

test("truncated frames are rejected", () => {
  assert.throws(() => decodeRpcFrame(fromHex(GOLDEN_REQUEST).subarray(0, 20)));
  assert.throws(() => decodeRpcFrame(Uint8Array.from([2, 1])));
});
