import { ExtensionContext } from "vscode";
import { LanguageClient, ServerOptions, TransportKind } from "vscode-languageclient/node";
import * as path from "path";

let client: LanguageClient;

export const activate = (context: ExtensionContext) => {
  console.log("Activating...");

  const serverModule = context.asAbsolutePath(path.join("..", "server", "build", "index.js"));

  const nodeModule = { module: serverModule, transport: TransportKind.ipc };

  const serverOptions: ServerOptions = {
    run: nodeModule,
    debug: {
      ...nodeModule,
      options: { execArgv: ["--nolazy", "--inspect=6009"] },
    },
  };

  const clientOptions = {
    documentSelector: [
      {
        scheme: "file",
        language: "plaintext",
      },
    ],
  };

  client = new LanguageClient("wire-spec-extension-id", "WireSpecChecker", serverOptions, clientOptions);

  client.start().catch(console.error);

  console.log("Done.");
};

export const deactivate = (): Thenable<void> | undefined => (client ? client.stop() : undefined);
