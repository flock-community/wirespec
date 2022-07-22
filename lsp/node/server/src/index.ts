import { TextDocument } from "vscode-languageserver-textdocument";
import { createConnection, DiagnosticSeverity, TextDocuments } from "vscode-languageserver";
import { community } from "wire-spec-lib";

const wsToTs = new community.flock.wirespec.compiler.lib.WsToTypeScript();

const getCompilerErrors = (text) => {
  const error = wsToTs.compile(text).error;
  if (error) {
    const { index, value, length } = error;
    return [{ index, length, value }];
  } else return [];
};

const toDiagnostic =
  (textDocument) =>
  ({ index, length, value }) => ({
    severity: DiagnosticSeverity.Error,
    range: {
      start: textDocument.positionAt(index),
      end: textDocument.positionAt(index + length),
    },
    message: value,
    source: "WireSpecCheck",
  });

const getDiagnostics = (textDocument) => getCompilerErrors(textDocument.getText()).map(toDiagnostic(textDocument));

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
