// A thin Node binding over the Aeron C client library (libaeron.so), loaded
// via FFI: enough surface to attach to a media driver, add publications and
// subscriptions on any channel, offer messages, and poll fragments -- with
// fragment reassembly done here, keyed on session id, using the frame flags.
//
// The library path comes from WIRESPEC_AERON_LIB (default: libaeron.so on the
// loader path); its version must match the media driver it attaches to.

import * as koffi from "koffi";

const BEGIN_FLAG = 0x80;
const END_FLAG = 0x40;

// aeron_header_values_t is copied into a caller buffer; the fields read here
// live in its leading frame struct: flags at byte 5, session id at bytes 12-15.
const HEADER_VALUES_SIZE = 48;
const HEADER_FLAGS_OFFSET = 5;
const HEADER_SESSION_ID_OFFSET = 12;

interface Api {
  contextInit: koffi.KoffiFunction;
  contextSetDir: koffi.KoffiFunction;
  contextClose: koffi.KoffiFunction;
  init: koffi.KoffiFunction;
  start: koffi.KoffiFunction;
  close: koffi.KoffiFunction;
  asyncAddPublication: koffi.KoffiFunction;
  asyncAddPublicationPoll: koffi.KoffiFunction;
  asyncAddSubscription: koffi.KoffiFunction;
  asyncAddSubscriptionPoll: koffi.KoffiFunction;
  publicationOffer: koffi.KoffiFunction;
  subscriptionPoll: koffi.KoffiFunction;
  headerValues: koffi.KoffiFunction;
  errmsg: koffi.KoffiFunction;
  fragmentHandler: koffi.IKoffiCType;
}

let loaded: Api | undefined;

function api(): Api {
  if (loaded) {
    return loaded;
  }
  const lib = koffi.load(process.env.WIRESPEC_AERON_LIB ?? "libaeron.so");
  const fragmentHandler = koffi.proto(
    "void WirespecAeronFragmentHandler(void *clientd, const uint8_t *buffer, size_t length, void *header)",
  );
  loaded = {
    contextInit: lib.func("int aeron_context_init(_Out_ void **context)"),
    contextSetDir: lib.func("int aeron_context_set_dir(void *context, const char *value)"),
    contextClose: lib.func("int aeron_context_close(void *context)"),
    init: lib.func("int aeron_init(_Out_ void **client, void *context)"),
    start: lib.func("int aeron_start(void *client)"),
    close: lib.func("int aeron_close(void *client)"),
    asyncAddPublication: lib.func(
      "int aeron_async_add_publication(_Out_ void **async, void *client, const char *uri, int32_t stream_id)",
    ),
    asyncAddPublicationPoll: lib.func(
      "int aeron_async_add_publication_poll(_Out_ void **publication, void *async)",
    ),
    asyncAddSubscription: lib.func(
      "int aeron_async_add_subscription(_Out_ void **async, void *client, const char *uri, int32_t stream_id, void *on_available, void *available_clientd, void *on_unavailable, void *unavailable_clientd)",
    ),
    asyncAddSubscriptionPoll: lib.func(
      "int aeron_async_add_subscription_poll(_Out_ void **subscription, void *async)",
    ),
    publicationOffer: lib.func(
      "int64_t aeron_publication_offer(void *publication, const uint8_t *buffer, size_t length, void *supplier, void *clientd)",
    ),
    subscriptionPoll: lib.func(
      "int aeron_subscription_poll(void *subscription, WirespecAeronFragmentHandler *handler, void *clientd, size_t fragment_limit)",
    ),
    headerValues: lib.func("int aeron_header_values(void *header, void *values)"),
    errmsg: lib.func("const char *aeron_errmsg()"),
    fragmentHandler,
  };
  return loaded;
}

function lastError(): string {
  return String(api().errmsg());
}

function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

export class Publication {
  constructor(private readonly publication: unknown) {}

  /** The new stream position, or a negative Aeron error code (back pressure, not connected, ...). */
  offer(bytes: Uint8Array): bigint {
    return BigInt(api().publicationOffer(this.publication, bytes, bytes.length, null, null));
  }
}

export class Subscription {
  private readonly assemblies = new Map<number, Uint8Array[]>();
  private readonly headerBuffer = Buffer.alloc(HEADER_VALUES_SIZE);
  private onMessage: (message: Uint8Array) => void = () => {};
  private handlerError: unknown;
  private readonly callback = koffi.register(
    (_clientd: unknown, buffer: unknown, length: number, header: unknown) => {
      try {
        this.onFragment(buffer, Number(length), header);
      } catch (error) {
        this.handlerError = error;
      }
    },
    koffi.pointer(api().fragmentHandler),
  );

  constructor(private readonly subscription: unknown) {}

  /** Poll for fragments, delivering each fully reassembled message to `handler`. */
  poll(handler: (message: Uint8Array) => void, fragmentLimit: number): number {
    this.onMessage = handler;
    const fragments = api().subscriptionPoll(this.subscription, this.callback, null, fragmentLimit);
    if (this.handlerError !== undefined) {
      const error = this.handlerError;
      this.handlerError = undefined;
      throw error;
    }
    return Number(fragments);
  }

  private onFragment(buffer: unknown, length: number, header: unknown): void {
    const bytes = Uint8Array.from(koffi.decode(buffer, koffi.array("uint8_t", length)) as number[]);
    if (api().headerValues(header, this.headerBuffer) < 0) {
      throw new Error(`Cannot read Aeron header: ${lastError()}`);
    }
    const flags = this.headerBuffer.readUInt8(HEADER_FLAGS_OFFSET);
    const sessionId = this.headerBuffer.readInt32LE(HEADER_SESSION_ID_OFFSET);
    if (flags & BEGIN_FLAG && flags & END_FLAG) {
      this.onMessage(bytes);
      return;
    }
    if (flags & BEGIN_FLAG) {
      this.assemblies.set(sessionId, [bytes]);
      return;
    }
    const parts = this.assemblies.get(sessionId);
    if (!parts) {
      return; // fragment of a message whose beginning was never seen
    }
    parts.push(bytes);
    if (flags & END_FLAG) {
      this.assemblies.delete(sessionId);
      this.onMessage(concat(parts));
    }
  }
}

function concat(parts: Uint8Array[]): Uint8Array {
  const assembled = new Uint8Array(parts.reduce((total, part) => total + part.length, 0));
  let position = 0;
  for (const part of parts) {
    assembled.set(part, position);
    position += part.length;
  }
  return assembled;
}

export class Aeron {
  private constructor(
    private readonly context: unknown,
    private readonly client: unknown,
  ) {}

  /** Attach to the media driver whose CnC file lives in `aeronDir`. */
  static connect(aeronDir: string): Aeron {
    const contextOut = [null];
    if (api().contextInit(contextOut) < 0) {
      throw new Error(`Cannot initialize Aeron context: ${lastError()}`);
    }
    const context = contextOut[0];
    if (api().contextSetDir(context, aeronDir) < 0) {
      throw new Error(`Cannot set Aeron directory '${aeronDir}': ${lastError()}`);
    }
    const clientOut = [null];
    if (api().init(clientOut, context) < 0) {
      api().contextClose(context);
      throw new Error(`Cannot attach to media driver at '${aeronDir}': ${lastError()}`);
    }
    const client = clientOut[0];
    if (api().start(client) < 0) {
      api().close(client);
      api().contextClose(context);
      throw new Error(`Cannot start Aeron client: ${lastError()}`);
    }
    return new Aeron(context, client);
  }

  // Both registrations are bounded: the driver can reject them (e.g. /dev/shm
  // exhaustion), which surfaces as the async poll failing or never completing.
  async addPublication(uri: string, streamId: number, timeoutMs = 10_000): Promise<Publication> {
    const asyncOut = [null];
    if (api().asyncAddPublication(asyncOut, this.client, uri, streamId) < 0) {
      throw new Error(`Cannot add publication on '${uri}': ${lastError()}`);
    }
    return new Publication(
      await this.awaitRegistration(api().asyncAddPublicationPoll, asyncOut[0], `publication on '${uri}'`, timeoutMs),
    );
  }

  async addSubscription(uri: string, streamId: number, timeoutMs = 10_000): Promise<Subscription> {
    const asyncOut = [null];
    if (api().asyncAddSubscription(asyncOut, this.client, uri, streamId, null, null, null, null) < 0) {
      throw new Error(`Cannot add subscription on '${uri}': ${lastError()}`);
    }
    return new Subscription(
      await this.awaitRegistration(api().asyncAddSubscriptionPoll, asyncOut[0], `subscription on '${uri}'`, timeoutMs),
    );
  }

  private async awaitRegistration(
    pollFn: koffi.KoffiFunction,
    asyncHandle: unknown,
    what: string,
    timeoutMs: number,
  ): Promise<unknown> {
    const deadline = Date.now() + timeoutMs;
    for (;;) {
      const resourceOut = [null];
      const outcome = pollFn(resourceOut, asyncHandle);
      if (outcome > 0) {
        return resourceOut[0];
      }
      if (outcome < 0) {
        throw new Error(`Cannot register ${what}: ${lastError()}`);
      }
      if (Date.now() >= deadline) {
        throw new Error(`Timed out registering ${what}`);
      }
      await sleep(1);
    }
  }

  close(): void {
    api().close(this.client);
    api().contextClose(this.context);
  }
}
