// Typed mapping between rpc frames and the generated types: the payloads
// travel as CBOR - the same Jackson mapping the backend uses, in a compact
// binary encoding - so each mapping is one encode or decode of a generated
// model (src/gen, emitted from ../spec by gen.sh).

import { RpcFrame } from "@flock/wirespec-aeron";

import { decodeCbor, encodeCbor } from "./cbor";
import { GetQuote } from "./gen/rpc";
import { Quote, QuoteError, QuoteList } from "./gen/model";

export function getQuoteParams(symbol: string): Uint8Array {
  return encodeCbor({ symbol });
}

/** Payload of a request without parameters: no bytes, whatever the encoding. */
export function emptyParams(): Uint8Array {
  return new Uint8Array(0);
}

/** Map a GetQuote response frame onto the generated `GetQuote.Response` union. */
export function getQuoteResponse(frame: RpcFrame): GetQuote.Response {
  switch (frame.kind) {
    case "result":
      return { value: decodeCbor(frame.payload) as Quote };
    case "error":
      return { value: decodeCbor(frame.payload) as QuoteError };
    case "request":
      throw new Error("Unexpected REQUEST frame for rpc 'GetQuote'");
  }
}

/** Map a GetWatchlistQuotes response frame onto the rpc's result type. */
export function getWatchlistQuotesResponse(frame: RpcFrame): QuoteList {
  switch (frame.kind) {
    case "result":
      return decodeCbor(frame.payload) as QuoteList;
    case "error":
      throw new Error(`Rpc 'GetWatchlistQuotes' failed: ${new TextDecoder().decode(frame.payload)}`);
    case "request":
      throw new Error("Unexpected REQUEST frame for rpc 'GetWatchlistQuotes'");
  }
}

/**
 * One line for a full quote, identical across the Rust and TypeScript clients
 * (the Docker integration test asserts the same strings from both).
 */
export function formatQuote(quote: Quote): string {
  const prev = quote.previousClose != null ? `prev ${quote.previousClose.amount} ${quote.previousClose.currency}` : "prev n/a";
  return `${quote.symbol} ${quote.last.amount} ${quote.last.currency} on ${quote.venue.mic} (${quote.venue.city}), ${prev}, history ${quote.history.length}`;
}

/** The short line for watchlist listings. */
export function formatQuoteLine(quote: Quote): string {
  return `${quote.symbol} ${quote.last.amount} ${quote.last.currency} on ${quote.venue.mic}`;
}

/** Map a Ping response frame onto the rpc's result type: a plain string, sent as raw UTF-8. */
export function pingResponse(frame: RpcFrame): string {
  switch (frame.kind) {
    case "result":
      return new TextDecoder().decode(frame.payload);
    case "error":
      throw new Error(`Rpc 'Ping' failed: ${new TextDecoder().decode(frame.payload)}`);
    case "request":
      throw new Error("Unexpected REQUEST frame for rpc 'Ping'");
  }
}
