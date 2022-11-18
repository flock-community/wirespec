import { TextDocument } from "vscode-languageserver-textdocument";
import { createConnection, DiagnosticSeverity, SemanticTokensBuilder, TextDocuments } from "vscode-languageserver";
import { WsToken, WsToTypeScript } from "wirespec-lib";
import { ServerCapabilities } from "vscode-languageserver-protocol/lib/common/protocol";

import { Range, SemanticTokensLegend } from "vscode-languageserver-types";

const wsToTs = new WsToTypeScript();

const getCompilerErrors = (text) => {
  const error = wsToTs.compile(text).error;

  if (error) {
    const { index, length, value } = error;
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
    source: "WirespecCheck",
  });

const tokenType = {
  KEYWORD: { name: "keyword", value: 0 },
  TYPE: { name: "type", value: 1 },
  VARIABLE: { name: "variable", value: 2 },
};

const tokenModifier = {
  DECLARATION: { name: "declaration", value: 1 },
};

const tokenTypeMapping = {
  WsTypeDef: tokenType.KEYWORD,
  CustomType: tokenType.TYPE,
  CustomValue: tokenType.VARIABLE,
  WsString: tokenType.TYPE,
  WsInteger: tokenType.TYPE,
  WsBoolean: tokenType.TYPE,
};

const tokenModifierMapping = {
  CustomValue: tokenModifier.DECLARATION,
};

const initialize = () => {
  const tokenTypes = [];
  Object.values(tokenType).forEach(({ name, value }) => (tokenTypes[value] = name));
  const tokenModifiers = [];
  Object.values(tokenModifier).forEach(({ name, value }) => (tokenModifiers[value] = name));
  const legend: SemanticTokensLegend = {
    tokenTypes,
    tokenModifiers,
  };

  const capabilities: ServerCapabilities = {
    // @ts-ignore
    textDocumentSync: documents.syncKind,
    semanticTokensProvider: {
      legend,
      range: false,
      full: true,
    },
    definitionProvider: true,
  };

  return { capabilities };
};

// @ts-ignore
const connection = createConnection();
const documents = new TextDocuments(TextDocument);

connection.onInitialize(initialize);

documents.onDidChangeContent(async (change) => {
  const doc = documents.get(change.document.uri);
  if (doc.getText()) {
    const errors = getCompilerErrors(doc.getText());
    await connection.sendDiagnostics({
      uri: change.document.uri,
      diagnostics: errors.map(toDiagnostic(doc)),
    });
  }
});

documents.onDidOpen(async (change) => {
  const doc = documents.get(change.document.uri);
  if (doc.getText()) {
    const errors = getCompilerErrors(doc.getText());
    await connection.sendDiagnostics({
      uri: change.document.uri,
      diagnostics: errors.map(toDiagnostic(doc)),
    });
  }
});

connection.onRequest(async (method, params) => {
  // @ts-ignore
  const doc = documents.get(params.textDocument.uri);
  const tokens = wsToTs.tokenize(doc.getText()).tokens;

  if (
    tokens &&
    (method === "textDocument/semanticTokens/full" || method === "textDocument/semanticTokens/full/delta")
  ) {
    const builder = new SemanticTokensBuilder();
    tokens.value.map(mapCoordinates).forEach(({ line, position, length, type, modifier }) => {
      builder.push(line, position, length, type?.value, modifier?.value);
    });
    return builder.build();
  }
});

connection.onDefinition((params) => {
  const uri = params.textDocument.uri;
  const doc = documents.get(uri);

  if (params) {
    const tokens = wsToTs.tokenize(doc.getText()).tokens;
    const token = tokens.value
      .map(mapCoordinates)
      .find(
        (it) =>
          params.position.line === it.line &&
          params.position.character >= it.position &&
          params.position.character <= it.position + it.length
      );
    if (token) {
      if (token.type?.name === "type") {
        return tokens.value
          .map(mapCoordinates)
          .filter((it) => it.value === token.value)
          .map((it) => ({
            uri: uri,
            range: Range.create(
              { line: it.line, character: it.position },
              { line: it.line, character: it.position + it.length }
            ),
          }));
      }
    }
  }
  return undefined;
});

documents.listen(connection);
connection.listen();

const mapCoordinates = ({ coordinates, type, value }: WsToken) => ({
  line: coordinates.line - 1,
  position: coordinates.position - 1 - coordinates.idxAndLength.length,
  length: coordinates.idxAndLength.length,
  type: tokenTypeMapping[type],
  modifier: tokenModifierMapping[type],
  value,
});

export {};
