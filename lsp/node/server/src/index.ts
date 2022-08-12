import {TextDocument} from "vscode-languageserver-textdocument";
import {createConnection, DiagnosticSeverity, SemanticTokensBuilder, TextDocuments} from "vscode-languageserver";
import {community} from "wire-spec-lib";
import {ServerCapabilities} from "vscode-languageserver-protocol/lib/common/protocol";

import {SemanticTokensLegend} from "vscode-languageserver-types";

const wsToTs = new community.flock.wirespec.compiler.lib.WsToTypeScript();

const getCompilerErrors = (text) => {
    const error = wsToTs.compile(text).error;
    if (error) {
        const {index, length, value} = error;
        return [{index, length, value}];
    } else return [];
};

const toDiagnostic = (textDocument) => ({index, length, value}) => ({
    severity: DiagnosticSeverity.Error,
    range: {
        start: textDocument.positionAt(index),
        end: textDocument.positionAt(index + length),
    },
    message: value,
    source: "WireSpecCheck",
});

const getDiagnostics = (textDocument) => getCompilerErrors(textDocument.getText()).map(toDiagnostic(textDocument));

const initialize = () => {
    const legend: SemanticTokensLegend = {
        tokenTypes: ["keyword", "type", "variable"],
        tokenModifiers: ["", "declaration"],
    };

    const capabilities: ServerCapabilities = {
        // @ts-ignore
        textDocumentSync: documents.syncKind,
        semanticTokensProvider: {
            legend,
            range: false,
            full: true,
        },
    };

    return {capabilities};
}

// @ts-ignore
const connection = createConnection();
const documents = new TextDocuments(TextDocument);

connection.onInitialize(initialize);

documents.onDidChangeContent(async (change) => {
    connection
        .sendDiagnostics({
            uri: change.document.uri,
            diagnostics: getDiagnostics(change.document),
        })
        .catch(console.error);
});

connection.onRequest(async (method, params) => {
    const uri = params.textDocument.uri
    const doc = documents.get(uri)

    const tokens = wsToTs.tokenize(doc.getText())

    connection.console.log(tokens)
    if (method === "textDocument/semanticTokens/full" || method === "textDocument/semanticTokens/full/delta") {
        const builder = new SemanticTokensBuilder();
        builder.push(0, 0, 4, 0, 1);
        builder.push(0, 5, 3, 1, 0);
        builder.push(1, 4, 3, 2, 0);
        builder.push(1, 9, 6, 1, 0);
        builder.push(2, 4, 3, 2, 0);
        builder.push(2, 9, 6, 1, 0);
        return builder.build();
    }
});

documents.listen(connection);
connection.listen();

export {};
