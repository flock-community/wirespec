import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const Prism = require("prismjs");
require("../prism-wirespec.js")(Prism);

const flatten = (stream, types = []) =>
  (Array.isArray(stream) ? stream : [stream]).flatMap((token) =>
    typeof token === "string"
      ? [{ text: token, types }]
      : flatten(token.content, [...types, [token.type, ...[token.alias ?? []].flat()]]),
  );

/** Tokenizes `source` into `{ text, types }` pairs; `types` holds the `[type, ...aliases]` of every enclosing token. */
export const tokenize = (source, language = "wirespec") => flatten(Prism.tokenize(source, Prism.languages[language]));
