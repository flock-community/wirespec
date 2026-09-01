import assert from "node:assert/strict";
import { test } from "node:test";

import { decodeCbor, encodeCbor } from "../src/cbor";

test("values round trip through CBOR", () => {
  const value = {
    symbol: "AAPL",
    price: 178.25,
    whole: 42,
    negative: -7,
    active: true,
    note: null,
    tags: ["tech", "large"],
    nested: { currency: "USD" },
  };
  assert.deepEqual(decodeCbor(encodeCbor(value)), value);
});

test("the encoding is binary CBOR, not JSON text", () => {
  const bytes = encodeCbor({ symbol: "AAPL" });
  assert.notEqual(bytes[0], "{".charCodeAt(0));
  assert.equal(bytes[0], 0xa1); // definite map of 1 entry
});

test("indefinite-length maps and strings decode (Jackson streams objects this way)", () => {
  // {_ "symbol": "AAPL", "price": 178.25 _}
  const bytes = Uint8Array.from([
    0xbf, // indefinite map
    0x66, ..."symbol".split("").map((c) => c.charCodeAt(0)),
    0x7f, 0x64, ..."AAPL".split("").map((c) => c.charCodeAt(0)), 0xff, // indefinite string, one chunk
    0x65, ..."price".split("").map((c) => c.charCodeAt(0)),
    0xfb, 0x40, 0x66, 0x48, 0x00, 0x00, 0x00, 0x00, 0x00, // double 178.25
    0xff, // break
  ]);
  assert.deepEqual(decodeCbor(bytes), { symbol: "AAPL", price: 178.25 });
});

test("half and single precision floats decode", () => {
  assert.equal(decodeCbor(Uint8Array.from([0xf9, 0x3c, 0x00])), 1.0);
  assert.equal(decodeCbor(Uint8Array.from([0xfa, 0x47, 0xc3, 0x50, 0x00])), 100000.0);
});

test("truncated values are rejected", () => {
  assert.throws(() => decodeCbor(encodeCbor({ symbol: "AAPL" }).subarray(0, 4)));
});
