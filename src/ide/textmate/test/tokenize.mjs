import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import oniguruma from "vscode-oniguruma";
import textmate from "vscode-textmate";

const require = createRequire(import.meta.url);

await oniguruma.loadWASM(readFileSync(require.resolve("vscode-oniguruma/release/onig.wasm")).buffer);

const registry = new textmate.Registry({
  onigLib: Promise.resolve({
    createOnigScanner: (patterns) => new oniguruma.OnigScanner(patterns),
    createOnigString: (string) => new oniguruma.OnigString(string),
  }),
  loadGrammar: async () => JSON.parse(readFileSync(new URL("../wirespec.tmLanguage.json", import.meta.url), "utf8")),
});

const grammar = await registry.loadGrammar("source.wirespec");

/** Tokenizes `source` into `{ text, scopes }` pairs, one per token, carrying the rule stack across lines. */
export const tokenize = (source) =>
  source.split(/\r?\n/).reduce(
    ({ tokens, ruleStack }, line) => {
      const result = grammar.tokenizeLine(line, ruleStack);
      return {
        tokens: [
          ...tokens,
          ...result.tokens.map(({ startIndex, endIndex, scopes }) => ({
            text: line.substring(startIndex, endIndex),
            scopes,
          })),
        ],
        ruleStack: result.ruleStack,
      };
    },
    { tokens: [], ruleStack: textmate.INITIAL },
  ).tokens;
