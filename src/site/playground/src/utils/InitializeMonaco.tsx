import { Monaco } from "@monaco-editor/react";

const DEFINITIONS = ["type", "enum", "endpoint", "channel", "rpc", "part"];

const KEYWORDS = [
  "Any",
  "Boolean",
  "Bytes",
  "Integer",
  "Integer32",
  "Number",
  "Number32",
  "String",
  "Unit",
];

export function initializeMonaco(monaco: Monaco) {
  const model = monaco.editor.getModels();
  if (!model) {
    return;
  }

  monaco.languages.register({ id: "wirespec" });

  monaco.editor.defineTheme("vs-dark", {
    colors: {
      "editor.background": "#000000",
    },
    inherit: true,
    base: "vs-dark",
    rules: [
      { token: "keyword", foreground: "#FFC300", fontStyle: "bold" },
      { token: "variable", foreground: "#55f1ba" },
      { token: "customKeyword", foreground: "#B151CBFF", fontStyle: "bold" },
      { token: "operator", foreground: "#B151CBFF", fontStyle: "bold" },
      { token: "regExp", foreground: "#D54E96FF" },
    ],
  });

  monaco.languages.setMonarchTokensProvider("wirespec", {
    definitions: DEFINITIONS,
    keywords: KEYWORDS,
    tokenizer: {
      root: [
        [/\/\/.*$/, "comment"],
        [/\/\*/, "comment", "@comment"],
        [/\/.*?\/g/, "regExp"],
        [/\.\.\./, "operator"],
        [
          /@?[a-zA-Z][\w$]*/,
          {
            cases: {
              "@definitions": "customKeyword",
              "@keywords": "keyword",
              "@default": "variable",
            },
          },
        ],
        [/".*?"/, "string"],
      ],
      comment: [
        [/[^/*]+/, "comment"],
        [/\*\//, "comment", "@pop"],
        [/[/*]/, "comment"],
      ],
    },
  });
}
