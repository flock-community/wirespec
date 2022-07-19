import { ExtensionContext } from "vscode";
import { LanguageClient } from "vscode-languageclient";

export function activate(context: ExtensionContext) {
  console.log("Activating...");

  const runConfig = {
    command: "wire-spec-server",
    args: ["--stdio"],
  };

  const serverOptions = {
    run: runConfig,
    debug: runConfig,
  };

  const clientOptions = {
    documentSelector: [
      {
        scheme: "file",
        language: "plaintext",
      },
    ],
  };

  const client = new LanguageClient(
    "wire-spec-extension-id",
    "WireSpecChecker",
    serverOptions,
    clientOptions
  );

  context.subscriptions.push(client.start());

  console.log("Done.");
}
