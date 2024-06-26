import * as vscode from "vscode";
import { ExtensionContext } from "vscode";
import { LanguageClient, ServerOptions, TransportKind, LanguageClientOptions } from "vscode-languageclient/node";
import * as path from "path";

let client: LanguageClient;

export const activate = (context: ExtensionContext) => {
  console.log("Activating...");

  const serverModule = context.asAbsolutePath(path.join("build", "server.js"));

  const nodeModule = { module: serverModule, transport: TransportKind.ipc };

  const serverOptions: ServerOptions = {
    run: nodeModule,
    debug: {
      ...nodeModule,
      options: { execArgv: ["--nolazy", "--inspect=6009"] }
    }
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      {
        scheme: "file",
        language: "wirespec",
      }
    ],
  };

  client = new LanguageClient("wirespec-extension-id", "WirespecChecker", serverOptions, clientOptions);

  client.start().catch(console.error);

  console.log("Done.");
};

export const deactivate = (): Thenable<void> | undefined => (client ? client.stop() : undefined);
