// The Wirespec-over-Aeron rpc wire protocol, mirroring the Kotlin codec in
// src/integration/aeron (RpcFrame.kt). Version 1, all integers little-endian:
//
//   u8   protocol version (1)
//   u8   frame kind (1 = REQUEST, 2 = RESULT, 3 = ERROR)
//   i64  correlation id
//   u16  method length, followed by that many bytes of UTF-8 method name
//   -- REQUEST only --
//   u16  reply channel length, followed by that many bytes of UTF-8 Aeron channel URI
//   i32  reply stream id
//   ------------------
//   u32  payload length, followed by that many bytes of payload

export const VERSION = 1;
export const KIND_REQUEST = 1;
export const KIND_RESULT = 2;
export const KIND_ERROR = 3;

export const DEFAULT_CHANNEL = "aeron:ipc";
export const DEFAULT_REQUEST_STREAM_ID = 1001;
export const DEFAULT_REPLY_STREAM_ID = 1002;

export type RpcFrame =
  | {
      kind: "request";
      correlationId: bigint;
      method: string;
      replyChannel: string;
      replyStreamId: number;
      payload: Uint8Array;
    }
  | { kind: "result"; correlationId: bigint; method: string; payload: Uint8Array }
  | { kind: "error"; correlationId: bigint; method: string; payload: Uint8Array };

export function encodeRpcFrame(frame: RpcFrame): Uint8Array {
  const method = new TextEncoder().encode(frame.method);
  const replyChannel =
    frame.kind === "request" ? new TextEncoder().encode(frame.replyChannel) : new Uint8Array(0);
  const request = frame.kind === "request" ? 2 + replyChannel.length + 4 : 0;
  const bytes = new Uint8Array(2 + 8 + 2 + method.length + request + 4 + frame.payload.length);
  const view = new DataView(bytes.buffer);
  let position = 0;
  view.setUint8(position, VERSION);
  position += 1;
  view.setUint8(position, { request: KIND_REQUEST, result: KIND_RESULT, error: KIND_ERROR }[frame.kind]);
  position += 1;
  view.setBigInt64(position, frame.correlationId, true);
  position += 8;
  view.setUint16(position, method.length, true);
  position += 2;
  bytes.set(method, position);
  position += method.length;
  if (frame.kind === "request") {
    view.setUint16(position, replyChannel.length, true);
    position += 2;
    bytes.set(replyChannel, position);
    position += replyChannel.length;
    view.setInt32(position, frame.replyStreamId, true);
    position += 4;
  }
  view.setUint32(position, frame.payload.length, true);
  position += 4;
  bytes.set(frame.payload, position);
  return bytes;
}

export function decodeRpcFrame(bytes: Uint8Array): RpcFrame {
  const reader = new Reader(bytes);
  const version = reader.u8();
  if (version !== VERSION) {
    throw new Error(`Unsupported Wirespec Aeron protocol version: ${version}`);
  }
  const kind = reader.u8();
  const correlationId = reader.i64();
  const method = reader.utf8();
  switch (kind) {
    case KIND_REQUEST:
      return {
        kind: "request",
        correlationId,
        method,
        replyChannel: reader.utf8(),
        replyStreamId: reader.i32(),
        payload: reader.payload(),
      };
    case KIND_RESULT:
      return { kind: "result", correlationId, method, payload: reader.payload() };
    case KIND_ERROR:
      return { kind: "error", correlationId, method, payload: reader.payload() };
    default:
      throw new Error(`Unknown Wirespec Aeron frame kind: ${kind}`);
  }
}

class Reader {
  private readonly view: DataView;
  private position = 0;

  constructor(private readonly bytes: Uint8Array) {
    this.view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  }

  private take(count: number): number {
    const offset = this.position;
    if (offset + count > this.bytes.length) {
      throw new Error("Truncated Wirespec Aeron frame");
    }
    this.position += count;
    return offset;
  }

  u8(): number {
    return this.view.getUint8(this.take(1));
  }

  i32(): number {
    return this.view.getInt32(this.take(4), true);
  }

  i64(): bigint {
    return this.view.getBigInt64(this.take(8), true);
  }

  utf8(): string {
    const length = this.view.getUint16(this.take(2), true);
    const offset = this.take(length);
    return new TextDecoder("utf-8", { fatal: true }).decode(this.bytes.subarray(offset, offset + length));
  }

  payload(): Uint8Array {
    const length = this.view.getUint32(this.take(4), true);
    const offset = this.take(length);
    return this.bytes.slice(offset, offset + length);
  }
}
