const { DiagnosticSeverity, TextDocuments, createConnection } = require("vscode-languageserver");
const { TextDocument } = require("vscode-languageserver-textdocument");
const Compiler = require("wire-spec-core").community.flock.wirespec.compiler.core.Compiler;

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

const connection = createConnection();
const documents = new TextDocuments(TextDocument);

connection.onInitialize(() => ({
  capabilities: {
    textDocumentSync: documents.syncKind,
  },
}));

documents.onDidChangeContent((change) => {
  connection.sendDiagnostics({
    uri: change.document.uri,
    diagnostics: getDiagnostics(change.document),
  });
});

documents.listen(connection);
connection.listen();
