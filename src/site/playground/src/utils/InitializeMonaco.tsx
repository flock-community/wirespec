import { Monaco } from "@monaco-editor/react";

const KEYWORDS = ["Boolean", "String", "Integer"];

const AVDL_DECLARATIONS = [
  "protocol",
  "record",
  "error",
  "enum",
  "fixed",
  "union",
  "array",
  "map",
];
const AVDL_KEYWORDS = [
  "boolean",
  "int",
  "long",
  "float",
  "double",
  "bytes",
  "string",
  "null",
  "true",
  "false",
];

export function initializeMonaco(monaco: Monaco) {
  const model = monaco.editor.getModels();
  if (!model) {
    return;
  }

  monaco.languages.register({ id: "wirespec" });
  monaco.languages.register({ id: "avdl" });

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
      { token: "regExp", foreground: "#D54E96FF" },
      { token: "operator", foreground: "#B151CBFF", fontStyle: "bold" },
    ],
  });

  monaco.languages.setMonarchTokensProvider("wirespec", {
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
              refined: "customKeyword",
              enum: "customKeyword",
              type: "customKeyword",
              endpoint: "customKeyword",
              channel: "customKeyword",
              rpc: "customKeyword",
              part: "customKeyword",
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

  monaco.languages.setMonarchTokensProvider("avdl", {
    declarations: AVDL_DECLARATIONS,
    keywords: AVDL_KEYWORDS,
    tokenizer: {
      root: [
        [/\/\/.*$/, "comment"],
        [/\/\*/, "comment", "@comment"],
        [/".*?"/, "string"],
        [/-?\d+(\.\d+)?/, "number"],
        [
          /[a-zA-Z_]\w*/,
          {
            cases: {
              "@declarations": "customKeyword",
              "@keywords": "keyword",
              "@default": "variable",
            },
          },
        ],
      ],
      comment: [
        [/[^/*]+/, "comment"],
        [/\*\//, "comment", "@pop"],
        [/[/*]/, "comment"],
      ],
    },
  });
}
