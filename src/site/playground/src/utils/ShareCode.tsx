// Edited code goes into the link compressed, as URL-safe base64, so a link
// holds a whole specification and stays short enough to share.
const transform = (bytes: Uint8Array, stream: GenericTransformStream) =>
  new Response(new Blob([bytes]).stream().pipeThrough(stream)).arrayBuffer();

export const encodeCode = async (code: string): Promise<string> =>
  btoa(
    Array.from(
      new Uint8Array(
        await transform(
          new TextEncoder().encode(code),
          new CompressionStream("deflate-raw"),
        ),
      ),
      (byte) => String.fromCharCode(byte),
    ).join(""),
  )
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");

export const decodeCode = async (encoded: string): Promise<string> =>
  new TextDecoder().decode(
    await transform(
      Uint8Array.from(
        atob(encoded.replace(/-/g, "+").replace(/_/g, "/")),
        (char) => char.charCodeAt(0),
      ),
      new DecompressionStream("deflate-raw"),
    ),
  );
