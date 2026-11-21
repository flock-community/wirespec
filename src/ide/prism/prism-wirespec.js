(function (register) {
  if (typeof module === "object" && module.exports) module.exports = register;
  if (typeof Prism !== "undefined") register(Prism);
})(function wirespec(Prism) {
  const methods = /\b(?:GET|POST|PUT|DELETE|OPTIONS|HEAD|PATCH|TRACE)\b/;
  const field = /`[\w-]+`|\b[a-z][\w-]*/;
  // The path characters of RFC 3986. A segment cannot start with `*`, because `/*` opens a comment, and a bare `/`
  // is an empty segment, as in `/` and `/todos/`.
  const pathCharacter = /[\w.~!$&'()*+,;=:@-]|%[\dA-Fa-f]{2}/;
  const segment = RegExp(`\\/(?:(?!\\*)(?:${pathCharacter.source})+|\\{[^{}\\n]*\\})?`);

  const string = { pattern: /"(?:\\.|[^"\\])*"/, greedy: true };
  const regex = {
    pattern: /(\(\s*)\/(?:\\\/|[^/\n])*\/[gimsuy]*/,
    lookbehind: true,
    greedy: true,
    inside: {
      "regex-delimiter": /^\/|\/(?=[gimsuy]*$)/,
      "regex-flags": /[gimsuy]+$/,
    },
  };
  const boolean = /\b(?:true|false)\b/;
  const nullValue = { pattern: /\bnull\b/, alias: "keyword" };
  const builtin = /\b(?:String|Integer32|Integer|Number32|Number|Boolean|Bytes|Unit|Any)\b/;
  const className = /\b[A-Z]\w*\b/;
  const number = { pattern: /(^|[^\w.-])-?\d+(?:\.\d+)?(?![\w.])/, lookbehind: true };
  const operator = /->|\.\.\.|\[\]|[=|!?#]/;
  const punctuation = /[{}()[\],:]/;
  // In a path only a parameter, between braces, holds names, types and operators: the rest is literal segments.
  const inParameter = (pattern) => RegExp(`(?:${pattern.source})(?=[^{}\\n]*\\})`);

  Prism.languages.wirespec = {
    comment: { pattern: /\/\*[\s\S]*?\*\/|\/\/.*/, greedy: true },
    string,
    annotation: {
      pattern: /@(?:(?:spec|file):)?[A-Z]\w*(?:\s*\((?:[^()"]|"(?:\\.|[^"\\])*")*\))?/,
      greedy: true,
      inside: {
        "annotation-name": {
          pattern: /^@(?:(?:spec|file):)?[A-Z]\w*/,
          alias: "attr-name",
          inside: { "annotation-target": { pattern: /(^@)(?:spec|file):/, lookbehind: true, alias: "keyword" } },
        },
        string,
        regex,
        property: /\b[A-Za-z_][\w-]*(?=\s*:)/,
        boolean,
        null: nullValue,
        number,
        constant: /`[\w-]+`|\b[A-Za-z_][\w-]*/,
        punctuation,
      },
    },
    regex,
    format: {
      pattern: /(\bString\s*\??\s*\(\s*)(?:date-time|date|time|email|uuid(?:-v[1-8])?)(?![\w-])/,
      lookbehind: true,
      alias: "constant",
    },
    enum: {
      pattern: /\benum\b[^{}]*\{[^{}]*\}/,
      inside: {
        "class-name": { pattern: /(^enum\s+)\w+/, lookbehind: true },
        keyword: /^enum\b/,
        punctuation,
        constant: /`[\w-]+`|-?\d+|\b[A-Za-z_][\w-]*/,
      },
    },
    path: {
      pattern: RegExp(`(${methods.source}[^/\\n]*?)(?:${segment.source})+`),
      lookbehind: true,
      alias: "string",
      inside: {
        parameter: { pattern: RegExp(`(\\{\\s*)(?:${field.source})`), lookbehind: true, alias: "variable" },
        builtin: inParameter(builtin),
        "class-name": inParameter(className),
        operator: inParameter(operator),
        punctuation: RegExp(`[{}]|${inParameter(/[:,]/).source}`),
      },
    },
    function: { pattern: /(\b(?:endpoint|rpc)\s+)[A-Za-z_]\w*/, lookbehind: true },
    "http-method": { pattern: methods, alias: "constant" },
    "status-code": { pattern: /(^|[\s{])\d+(?=\s*->)/m, lookbehind: true, alias: "number" },
    property: RegExp(`(?:${field.source})(?=\\s*:)`),
    "enum-value": {
      pattern:
        /(:[^=:,{}()\n]*(?:\([^()\n]*\))?[^=:,{}()\n]*=\s*)(?!(?:true|false|null)\b)(?:`[\w-]+`|\b[A-Za-z_][\w-]*)/,
      lookbehind: true,
      alias: "constant",
    },
    keyword: /\b(?:type|enum|endpoint|channel|rpc|part)\b/,
    boolean,
    null: nullValue,
    builtin,
    "class-name": className,
    number,
    unbounded: { pattern: /(^|[^\w-])_(?![\w-])/, lookbehind: true, alias: "constant" },
    operator,
    punctuation,
  };

  Prism.languages.ws = Prism.languages.wirespec;
});
