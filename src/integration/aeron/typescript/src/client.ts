// A Wirespec rpc client over Aeron: requests go out on the shared request
// stream, responses come back on this client's own reply stream, correlated by
// id -- the TypeScript counterpart of the Kotlin `AeronRpcClient` and the
// `wirespec-aeron` Rust crate. Attach it to a local media driver; for the
// network, use `aeron:udp?endpoint=host:port` channels.

import { Aeron, Publication, Subscription } from "./aeron";
import {
  decodeRpcFrame,
  DEFAULT_CHANNEL,
  DEFAULT_REPLY_STREAM_ID,
  DEFAULT_REQUEST_STREAM_ID,
  encodeRpcFrame,
  RpcFrame,
} from "./protocol";

function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

export class AeronRpcClient {
  private nextCorrelationId = BigInt(Date.now()) * 1_000_000n;

  private constructor(
    private readonly aeron: Aeron,
    private readonly publication: Publication,
    private readonly subscription: Subscription,
    private readonly replyChannel: string,
    private readonly replyStreamId: number,
  ) {}

  /** Connect over shared memory (`aeron:ipc`) on the default streams. */
  static connect(aeronDir: string): Promise<AeronRpcClient> {
    return AeronRpcClient.connectChannels(
      aeronDir,
      DEFAULT_CHANNEL,
      DEFAULT_REQUEST_STREAM_ID,
      DEFAULT_CHANNEL,
      DEFAULT_REPLY_STREAM_ID,
    );
  }

  /**
   * Connect on explicit channels, e.g. `aeron:udp?endpoint=host:port` for the
   * network: requests go out on `requestChannel`, and `replyChannel` -- a host
   * resolvable by both sides -- is subscribed locally and advertised in every
   * request for the responses.
   */
  static async connectChannels(
    aeronDir: string,
    requestChannel: string,
    requestStreamId: number,
    replyChannel: string,
    replyStreamId: number,
  ): Promise<AeronRpcClient> {
    const aeron = Aeron.connect(aeronDir);
    try {
      const publication = await aeron.addPublication(requestChannel, requestStreamId);
      const subscription = await aeron.addSubscription(replyChannel, replyStreamId);
      return new AeronRpcClient(aeron, publication, subscription, replyChannel, replyStreamId);
    } catch (error) {
      aeron.close();
      throw error;
    }
  }

  /** Send one request and await its RESULT or ERROR frame. */
  async call(method: string, payload: Uint8Array, timeoutMs = 10_000): Promise<RpcFrame> {
    this.nextCorrelationId += 1n;
    const correlationId = this.nextCorrelationId;
    const request = encodeRpcFrame({
      kind: "request",
      correlationId,
      method,
      replyChannel: this.replyChannel,
      replyStreamId: this.replyStreamId,
      payload,
    });
    const deadline = Date.now() + timeoutMs;
    await this.offer(request, deadline);
    return this.awaitResponse(correlationId, deadline, method);
  }

  private async offer(bytes: Uint8Array, deadline: number): Promise<void> {
    while (this.publication.offer(bytes) <= 0n) {
      if (Date.now() >= deadline) {
        throw new Error("Timed out offering request to Aeron");
      }
      await sleep(1);
    }
  }

  private async awaitResponse(correlationId: bigint, deadline: number, method: string): Promise<RpcFrame> {
    for (;;) {
      let response: RpcFrame | undefined;
      this.subscription.poll((message) => {
        if (response) {
          return;
        }
        // Other clients may share this reply stream; keep only our correlation id.
        try {
          const frame = decodeRpcFrame(message);
          if (frame.correlationId === correlationId) {
            response = frame;
          }
        } catch {
          // not a Wirespec rpc frame; ignore
        }
      }, 10);
      if (response) {
        return response;
      }
      if (Date.now() >= deadline) {
        throw new Error(`Timed out waiting for a response to rpc '${method}'`);
      }
      await sleep(1);
    }
  }

  close(): void {
    this.aeron.close();
  }
}
