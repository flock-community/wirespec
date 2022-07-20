import { ExtensionContext } from "vscode";
import { LanguageClient, ServerOptions, TransportKind } from "vscode-languageclient/node";
import * as path from "path";

let client: LanguageClient;

export function activate(context: ExtensionContext) {
  console.log("Activating...");

  const serverModule = context.asAbsolutePath(path.join("..", "server", "index.js"));

  console.log(serverModule);

  const serverOptions: ServerOptions = {
    run: {
      module: serverModule,
      transport: TransportKind.ipc,
      options: { execArgv: ["--nolazy", "--inspect=6009"] },
    },
    debug: {
      module: serverModule,
      transport: TransportKind.ipc,
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

  client.start();

  console.log("Done.");
}

export function deactivate(): Thenable<void> | undefined {
  return client ? client.stop() : undefined;
}
