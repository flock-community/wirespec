import assert from "node:assert/strict";
import { test } from "node:test";

import { RpcFrame } from "@flock/wirespec-aeron";

import { encodeCbor } from "../src/cbor";
import { getQuoteParams, getQuoteResponse, getWatchlistQuotesResponse, pingResponse } from "../src/rpc";

const result = (method: string, payload: Uint8Array): RpcFrame => ({
  kind: "result",
  correlationId: 1n,
  method,
  payload,
});

const error = (method: string, payload: Uint8Array): RpcFrame => ({
  kind: "error",
  correlationId: 1n,
  method,
  payload,
});

test("GetQuote parameters encode as a CBOR map", () => {
  const bytes = getQuoteParams("AAPL");
  assert.equal(bytes[0], 0xa1);
  assert.notEqual(bytes[0], "{".charCodeAt(0));
});

test("a RESULT frame maps onto the generated Quote", () => {
  const quote = { symbol: "AAPL", price: 178.25, currency: "USD" };
  const response = getQuoteResponse(result("GetQuote", encodeCbor(quote)));
  assert.deepEqual(response.value, quote);
});

test("an ERROR frame maps onto the generated QuoteError", () => {
  const quoteError = { code: "UNKNOWN_SYMBOL", message: "No quote for symbol 'NOPE'" };
  const response = getQuoteResponse(error("GetQuote", encodeCbor(quoteError)));
  assert.deepEqual(response.value, quoteError);
});

test("GetWatchlistQuotes maps onto the generated QuoteList", () => {
  const quotes = { quotes: [{ symbol: "FLCK", price: 42, currency: "EUR" }] };
  assert.deepEqual(getWatchlistQuotesResponse(result("GetWatchlistQuotes", encodeCbor(quotes))), quotes);
});

test("GetWatchlistQuotes errors throw with the error payload", () => {
  assert.throws(
    () => getWatchlistQuotesResponse(error("GetWatchlistQuotes", new TextEncoder().encode("boom"))),
    /boom/,
  );
});

test("Ping responses are raw UTF-8", () => {
  assert.equal(pingResponse(result("Ping", new TextEncoder().encode("pong"))), "pong");
});
