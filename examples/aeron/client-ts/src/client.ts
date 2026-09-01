// The TypeScript half of the Aeron example: a Node client attached to its own
// local media driver through the Aeron C client library, talking to the
// Kotlin Spring Boot backend (../server) over the network - every channel is
// an aeron:udp endpoint with a host both sides can resolve.

import * as os from "node:os";
import * as path from "node:path";

import { AeronRpcClient } from "@flock/wirespec-aeron";

import { emptyParams, getQuoteParams, getQuoteResponse, getWatchlistQuotesResponse, pingResponse } from "./rpc";

const TIMEOUT_MS = 10_000;

const envOr = (name: string, fallback: string): string => process.env[name] ?? fallback;

const sleep = (milliseconds: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function main(): Promise<void> {
  const aeronDir =
    process.argv[2] ?? process.env.AERON_DIR ?? path.join(os.tmpdir(), "wirespec-aeron");
  const requestChannel = envOr("REQUEST_CHANNEL", "aeron:udp?endpoint=localhost:40123");
  const replyChannel = envOr("REPLY_CHANNEL", "aeron:udp?endpoint=localhost:40127");

  console.log(`Attaching to Aeron media driver at ${aeronDir}`);

  // The local media driver may still be coming up (e.g. a sidecar container).
  const deadline = Date.now() + 20_000;
  let client: AeronRpcClient;
  for (;;) {
    try {
      client = await AeronRpcClient.connectChannels(aeronDir, requestChannel, 1001, replyChannel, 1002);
      break;
    } catch (error) {
      if (Date.now() >= deadline) {
        throw error;
      }
      await sleep(500);
    }
  }

  const pong = pingResponse(await client.call("Ping", emptyParams(), TIMEOUT_MS));
  console.log(`Ping -> ${pong}`);

  for (const symbol of ["AAPL", "GOOG", "FLCK", "NOPE"]) {
    const response = getQuoteResponse(await client.call("GetQuote", getQuoteParams(symbol), TIMEOUT_MS));
    if ("price" in response.value) {
      const quote = response.value;
      console.log(`GetQuote ${symbol} -> ${quote.symbol} ${quote.price} ${quote.currency}`);
    } else {
      const quoteError = response.value;
      console.log(`GetQuote ${symbol} -> error ${quoteError.code}: ${quoteError.message}`);
    }
  }

  // The full loop, across three languages: this call makes the Kotlin backend
  // call GetWatchlist on the Rust client, and the quotes for its watchlist
  // come back here. Longer deadline than the backend's own reverse-call
  // timeout, so a failure inside the loop surfaces as the backend's typed
  // error instead of a silent timeout here.
  const quoteList = getWatchlistQuotesResponse(await client.call("GetWatchlistQuotes", emptyParams(), 30_000));
  for (const quote of quoteList.quotes) {
    console.log(`GetWatchlistQuotes -> ${quote.symbol} ${quote.price} ${quote.currency}`);
  }

  client.close();
}

main().then(
  () => process.exit(0),
  (error) => {
    console.error(error instanceof Error ? error.message : error);
    process.exit(1);
  },
);
