import assert from "node:assert/strict";
import { test } from "node:test";

import { RpcFrame } from "@flock/wirespec-aeron";

import { encodeCbor } from "../src/cbor";
import { formatQuote, getQuoteParams, getQuoteResponse, getWatchlistQuotesResponse, pingResponse } from "../src/rpc";
import { Quote } from "../src/gen/model";

const flck: Quote = {
  symbol: "FLCK",
  last: { amount: 42, currency: "EUR" },
  previousClose: undefined,
  venue: { mic: "XAMS", city: "Amsterdam" },
  history: [{ at: "16:00", price: { amount: 42, currency: "EUR" } }],
};

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
  const quote: Quote = {
    symbol: "AAPL",
    last: { amount: 178.25, currency: "USD" },
    previousClose: { amount: 176.1, currency: "USD" },
    venue: { mic: "XNAS", city: "New York" },
    history: [{ at: "16:00", price: { amount: 178.25, currency: "USD" } }],
  };
  const response = getQuoteResponse(result("GetQuote", encodeCbor(quote)));
  assert.deepEqual(response.value, quote);
});

test("quotes format identically across clients", () => {
  // The Docker integration test asserts this exact line from both the Rust
  // and the TypeScript client.
  assert.equal(formatQuote(flck), "FLCK 42 EUR on XAMS (Amsterdam), prev n/a, history 1");
});

test("an ERROR frame maps onto the generated QuoteError", () => {
  const quoteError = { code: "UNKNOWN_SYMBOL", message: "No quote for symbol 'NOPE'" };
  const response = getQuoteResponse(error("GetQuote", encodeCbor(quoteError)));
  assert.deepEqual(response.value, quoteError);
});

test("GetWatchlistQuotes maps onto the generated QuoteList", () => {
  const quotes = { quotes: [{ ...flck, previousClose: null }] };
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
