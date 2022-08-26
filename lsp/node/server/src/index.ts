import { TextDocument } from "vscode-languageserver-textdocument";
import { createConnection, DiagnosticSeverity, SemanticTokensBuilder, TextDocuments } from "vscode-languageserver";
import { WsToTypeScript } from "wire-spec-lib";
import { ServerCapabilities } from "vscode-languageserver-protocol/lib/common/protocol";

import { SemanticTokensLegend } from "vscode-languageserver-types";

const wsToTs = new WsToTypeScript();

const getCompilerErrors = (text) => {
  const error = wsToTs.compile(text).error;

  if (error) {
    const { index, length, value } = error;
    return [{ index, length, value }];
  } else return [];
};

const toDiagnostic = (textDocument) => ({ index, length, value }) => ({
  severity: DiagnosticSeverity.Error,
  range: {
    start: textDocument.positionAt(index),
    end: textDocument.positionAt(index + length)
  },
  message: value,
  source: "WireSpecCheck"
});

const getDiagnostics = (textDocument) => getCompilerErrors(textDocument.getText()).map(toDiagnostic(textDocument));

const tokenType = {
  KEYWORD: { name: "keyword", value: 0 },
  TYPE: { name: "type", value: 1 },
  VARIABLE: { name: "variable", value: 2 }
};

const tokenModifier = {
  DECLARATION: { name: "declaration", value: 1 }
};

const tokenTypeMapping = {
  "WsTypeDef": tokenType.KEYWORD,
  "CustomType": tokenType.TYPE,
  "CustomValue": tokenType.VARIABLE,
  "WsString": tokenType.TYPE,
  "WsInteger": tokenType.TYPE
};

const tokenModifierMapping = {
  "CustomValue": tokenModifier.DECLARATION
};

const initialize = () => {
  const tokenTypes = [];
  Object.values(tokenType).forEach(({ name, value }) =>
    tokenTypes[value] = name
  );
  const tokenModifiers = [];
  Object.values(tokenModifier).forEach(({ name, value }) =>
    tokenModifiers[value] = name
  );
  const legend: SemanticTokensLegend = {
    tokenTypes,
    tokenModifiers
  };

  const capabilities: ServerCapabilities = {
    // @ts-ignore
    textDocumentSync: documents.syncKind,
    semanticTokensProvider: {
      legend,
      range: false,
      full: true
    }
  };

  return { capabilities };
};

// @ts-ignore
const connection = createConnection();
const documents = new TextDocuments(TextDocument);

connection.onInitialize(initialize);

documents.onDidChangeContent(async (change) => {
  connection
    .sendDiagnostics({
      uri: change.document.uri,
      diagnostics: getDiagnostics(change.document)
    })
    .catch(console.error);
});

connection.onRequest(async (method, params) => {
  const uri = params.textDocument.uri;
  const doc = documents.get(uri);

  const tokens = wsToTs.tokenize(doc.getText()).tokens;

  if (tokens && (method === "textDocument/semanticTokens/full" || method === "textDocument/semanticTokens/full/delta")) {

    const builder = new SemanticTokensBuilder();
    tokens.value
      .map(({ coordinates, type }) => ({
        line: coordinates.line - 1,
        position: coordinates.position - 1 - coordinates.idxAndLength.length,
        length: coordinates.idxAndLength.length,
        type: tokenTypeMapping[type],
        modifier: tokenModifierMapping[type]
      }))
      .forEach(({ line, position, length, type, modifier }) => {
        builder.push(line, position, length, type?.value, modifier?.value);
      });
    return builder.build();
  }
});

documents.listen(connection);
connection.listen();

export {};
