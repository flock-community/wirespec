// A minimal, dependency-free CBOR codec (RFC 8949), covering what the
// Wirespec models on this wire need: maps with text keys, arrays, text
// strings, numbers, booleans and null. The decoder additionally accepts
// indefinite-length containers and half/single-precision floats, because
// Jackson's CBORMapper (the backend) streams objects as indefinite maps.

const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder("utf-8", { fatal: true });

export function encodeCbor(value: unknown): Uint8Array {
  const out: number[] = [];
  writeValue(out, value);
  return Uint8Array.from(out);
}

function writeValue(out: number[], value: unknown): void {
  if (value === null || value === undefined) {
    out.push(0xf6);
  } else if (typeof value === "boolean") {
    out.push(value ? 0xf5 : 0xf4);
  } else if (typeof value === "number") {
    writeNumber(out, value);
  } else if (typeof value === "bigint") {
    writeInteger(out, value);
  } else if (typeof value === "string") {
    const bytes = textEncoder.encode(value);
    writeHead(out, 3, bytes.length);
    out.push(...bytes);
  } else if (value instanceof Uint8Array) {
    writeHead(out, 2, value.length);
    out.push(...value);
  } else if (Array.isArray(value)) {
    writeHead(out, 4, value.length);
    for (const element of value) {
      writeValue(out, element);
    }
  } else if (typeof value === "object") {
    const entries = Object.entries(value).filter(([, v]) => v !== undefined);
    writeHead(out, 5, entries.length);
    for (const [key, entryValue] of entries) {
      writeValue(out, key);
      writeValue(out, entryValue);
    }
  } else {
    throw new Error(`Cannot encode ${typeof value} as CBOR`);
  }
}

function writeNumber(out: number[], value: number): void {
  if (Number.isSafeInteger(value)) {
    writeInteger(out, BigInt(value));
  } else {
    out.push(0xfb);
    const bytes = new Uint8Array(8);
    new DataView(bytes.buffer).setFloat64(0, value);
    out.push(...bytes);
  }
}

function writeInteger(out: number[], value: bigint): void {
  if (value >= 0n) {
    writeHead(out, 0, value);
  } else {
    writeHead(out, 1, -1n - value);
  }
}

function writeHead(out: number[], major: number, length: number | bigint): void {
  const value = BigInt(length);
  const type = major << 5;
  if (value < 24n) {
    out.push(type | Number(value));
  } else if (value < 0x100n) {
    out.push(type | 24, Number(value));
  } else if (value < 0x10000n) {
    out.push(type | 25, Number(value >> 8n), Number(value & 0xffn));
  } else if (value < 0x100000000n) {
    out.push(type | 26);
    for (let shift = 24n; shift >= 0n; shift -= 8n) {
      out.push(Number((value >> shift) & 0xffn));
    }
  } else {
    out.push(type | 27);
    for (let shift = 56n; shift >= 0n; shift -= 8n) {
      out.push(Number((value >> shift) & 0xffn));
    }
  }
}

export function decodeCbor(bytes: Uint8Array): unknown {
  const reader = new CborReader(bytes);
  const value = reader.readValue();
  if (!reader.done()) {
    throw new Error("Trailing bytes after CBOR value");
  }
  return value;
}

const INDEFINITE = -1n;
const BREAK = Symbol("break");

class CborReader {
  private readonly view: DataView;
  private position = 0;

  constructor(private readonly bytes: Uint8Array) {
    this.view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  }

  done(): boolean {
    return this.position >= this.bytes.length;
  }

  readValue(): unknown {
    const value = this.readItem();
    if (value === BREAK) {
      throw new Error("Unexpected CBOR break");
    }
    return value;
  }

  private readItem(): unknown {
    const initial = this.u8();
    const major = initial >> 5;
    const info = initial & 0x1f;
    switch (major) {
      case 0:
        return asNumber(this.argument(info));
      case 1:
        return asNumber(-1n - this.argument(info));
      case 2:
        return this.readChunked(initial, 2, (length) => this.take(length).slice());
      case 3:
        return this.readChunked(initial, 3, (length) => textDecoder.decode(this.take(length)));
      case 4:
        return this.readArray(info);
      case 5:
        return this.readMap(info);
      case 6:
        this.argument(info); // tag: skip, decode the tagged value itself
        return this.readValue();
      default:
        return this.readSimple(info);
    }
  }

  private readChunked<T extends string | Uint8Array>(
    initial: number,
    major: number,
    read: (length: number) => T,
  ): T {
    const info = initial & 0x1f;
    if (info !== 0x1f) {
      return read(Number(this.argument(info)));
    }
    const chunks: T[] = [];
    for (;;) {
      const next = this.u8();
      if (next === 0xff) {
        break;
      }
      if (next >> 5 !== major || (next & 0x1f) === 0x1f) {
        throw new Error("Malformed indefinite-length CBOR string");
      }
      chunks.push(read(Number(this.argument(next & 0x1f))));
    }
    return (
      typeof chunks[0] === "string" || chunks.length === 0
        ? (chunks as string[]).join("")
        : concat(chunks as Uint8Array[])
    ) as T;
  }

  private readArray(info: number): unknown[] {
    const elements: unknown[] = [];
    this.readContainer(info, () => elements.push(this.readValue()));
    return elements;
  }

  private readMap(info: number): Record<string, unknown> {
    const map: Record<string, unknown> = {};
    this.readContainer(info, () => {
      const key = this.readValue();
      if (typeof key !== "string") {
        throw new Error(`Unsupported non-text CBOR map key: ${String(key)}`);
      }
      map[key] = this.readValue();
    });
    return map;
  }

  private readContainer(info: number, readEntry: () => void): void {
    if (info === 0x1f) {
      while (this.peek() !== 0xff) {
        readEntry();
      }
      this.position += 1;
      return;
    }
    const length = Number(this.argument(info));
    for (let i = 0; i < length; i++) {
      readEntry();
    }
  }

  private readSimple(info: number): unknown {
    switch (info) {
      case 20:
        return false;
      case 21:
        return true;
      case 22:
      case 23:
        return null;
      case 25:
        return halfToNumber(this.view.getUint16(this.takeAt(2)));
      case 26:
        return this.view.getFloat32(this.takeAt(4));
      case 27:
        return this.view.getFloat64(this.takeAt(8));
      case 0x1f:
        return BREAK;
      default:
        if (info === 24) {
          this.u8();
          return null; // unassigned simple value
        }
        return null;
    }
  }

  private argument(info: number): bigint {
    if (info < 24) {
      return BigInt(info);
    }
    switch (info) {
      case 24:
        return BigInt(this.u8());
      case 25:
        return BigInt(this.view.getUint16(this.takeAt(2)));
      case 26:
        return BigInt(this.view.getUint32(this.takeAt(4)));
      case 27:
        return this.view.getBigUint64(this.takeAt(8));
      case 0x1f:
        return INDEFINITE;
      default:
        throw new Error(`Malformed CBOR additional info: ${info}`);
    }
  }

  private peek(): number {
    if (this.position >= this.bytes.length) {
      throw new Error("Truncated CBOR value");
    }
    return this.bytes[this.position];
  }

  private u8(): number {
    const byte = this.peek();
    this.position += 1;
    return byte;
  }

  private take(count: number): Uint8Array {
    return this.bytes.subarray(this.takeAt(count), this.position);
  }

  private takeAt(count: number): number {
    const offset = this.position;
    if (offset + count > this.bytes.length) {
      throw new Error("Truncated CBOR value");
    }
    this.position += count;
    return offset;
  }
}

function asNumber(value: bigint): number | bigint {
  return value >= BigInt(Number.MIN_SAFE_INTEGER) && value <= BigInt(Number.MAX_SAFE_INTEGER)
    ? Number(value)
    : value;
}

function halfToNumber(half: number): number {
  const sign = half & 0x8000 ? -1 : 1;
  const exponent = (half >> 10) & 0x1f;
  const fraction = half & 0x3ff;
  if (exponent === 0) {
    return sign * fraction * 2 ** -24;
  }
  if (exponent === 0x1f) {
    return fraction ? NaN : sign * Infinity;
  }
  return sign * (1 + fraction / 1024) * 2 ** (exponent - 15);
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
