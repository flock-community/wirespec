import { TextDocument } from "vscode-languageserver-textdocument";
import { createConnection, DiagnosticSeverity, TextDocuments } from "vscode-languageserver";
import { community } from "wire-spec-core";

const Compiler = community.flock.wirespec.compiler.core.Compiler;

const { log } = console;

const wsSource = "type Foo { bar: String, baz: Integer }";
const wsCompiler = new Compiler();

log(wsCompiler.toKotlin(wsSource));
log(wsCompiler.toTypeScript(wsSource));

const getBlacklisted = (text) => {
  const blacklist = ["foo", "bar", "baz", "bal"];
  const regex = new RegExp(`\\b(${blacklist.join("|")})\\b`, "gi");
  const results = [];
  regex.lastIndex = 0;
  let matches;
  while ((matches = regex.exec(text)) && results.length < 100) {
    results.push({
      value: matches[0],
      index: matches.index,
    });
  }
  return results;
};

const blacklistToDiagnostic =
  (textDocument) =>
  ({ index, value }) => ({
    severity: DiagnosticSeverity.Warning,
    range: {
      start: textDocument.positionAt(index),
      end: textDocument.positionAt(index + value.length),
    },
    message: `${value} is blacklisted.`,
    source: "WireSpecCheck",
  });

const getDiagnostics = (textDocument) =>
  getBlacklisted(textDocument.getText()).map(blacklistToDiagnostic(textDocument));

// @ts-ignore
const connection = createConnection();
const documents = new TextDocuments(TextDocument);

connection.onInitialize(() => ({
  capabilities: {
    // @ts-ignore
    textDocumentSync: documents.syncKind,
  },
}));

documents.onDidChangeContent((change) =>
  connection
    .sendDiagnostics({
      uri: change.document.uri,
      diagnostics: getDiagnostics(change.document),
    })
    .catch(console.error)
);

documents.listen(connection);
connection.listen();
