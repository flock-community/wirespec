import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { corpus } from "./corpus.mjs";
import { tokenize } from "./tokenize.mjs";

const typeOf = (source, text, occurrence = 0) => {
  const token = tokenize(source).filter((it) => it.text === text)[occurrence];
  assert.ok(token, `no token ${JSON.stringify(text)} (occurrence ${occurrence}) in ${JSON.stringify(source)}`);
  return token.types.map((it) => it.join("/")).join(" > ");
};

const expectTypes = (source, expected) =>
  expected.forEach(([text, type, occurrence]) =>
    assert.equal(typeOf(source, text, occurrence), type, `type of ${JSON.stringify(text)}`),
  );

describe("registration", () => {
  it("registers the ws alias", () => assert.deepEqual(tokenize("type A {}", "ws"), tokenize("type A {}")));
});

describe("comments", () => {
  it("highlights line and block comments", () => {
    expectTypes("// line\n/* block\n type A {} */\ntype B {}", [
      ["// line", "comment"],
      ["/* block\n type A {} */", "comment"],
      ["B", "class-name"],
    ]);
  });
});

describe("types", () => {
  const source = `type Todo {
  id: TodoId,
  name: String?,
  matrix: Integer32[][]?,
  labels: { String },
  \`X-Name\`: Number32,
  nothing: Unit
}`;

  it("highlights the definition and its fields", () => {
    expectTypes(source, [
      ["type", "keyword"],
      ["Todo", "class-name"],
      ["{", "punctuation"],
      ["id", "property"],
      [":", "punctuation"],
      ["TodoId", "class-name"],
      [",", "punctuation"],
      ["String", "builtin"],
      ["?", "operator"],
      ["Integer32", "builtin"],
      ["[]", "operator"],
      ["`X-Name`", "property"],
      ["Number32", "builtin"],
      ["Unit", "builtin"],
    ]);
  });

  it("highlights default values", () => {
    expectTypes(
      `type Config {
  name: String = "anonymous",
  port: Integer32(0, 65535) = 8080,
  ratio: Number = -1.5,
  active: Boolean = true,
  nickname: String? = null,
  status: Status = ACTIVE,
  order: Order = \`asc\`
}`,
      [
        ['"anonymous"', "string"],
        ["=", "operator"],
        ["0", "number"],
        ["8080", "number"],
        ["-1.5", "number"],
        ["true", "boolean"],
        ["null", "null/keyword"],
        ["ACTIVE", "enum-value/constant"],
        ["`asc`", "enum-value/constant"],
      ],
    );
  });

  it("highlights refined types", () => {
    expectTypes(
      "type Name = String(/^[a-z]\\/x$/g)\ntype Age = Integer(0, _)\ntype Score = Number(-1.0, _)\ntype Id = String(uuid-v7)\ntype At {\n  at: String?(date-time),\n  by: String(email)\n}",
      [
        ["Name", "class-name"],
        ["/", "regex > regex-delimiter"],
        ["^[a-z]\\/x$", "regex"],
        ["g", "regex > regex-flags"],
        ["_", "unbounded/constant"],
        ["-1.0", "number"],
        ["uuid-v7", "format/constant"],
        ["date-time", "format/constant"],
        ["email", "format/constant"],
      ],
    );
  });

  it("highlights unions", () => {
    expectTypes("type Result = Win | Loss", [
      ["Win", "class-name"],
      ["|", "operator"],
      ["Loss", "class-name"],
    ]);
  });
});

describe("enums", () => {
  it("highlights the entries", () => {
    expectTypes("enum Status {\n  ACTIVE, `in-active`, 42, snake_case\n}", [
      ["enum", "enum > keyword"],
      ["Status", "enum > class-name"],
      ["ACTIVE", "enum > constant"],
      ["`in-active`", "enum > constant"],
      ["42", "enum > constant"],
      ["snake_case", "enum > constant"],
    ]);
  });
});

describe("endpoints", () => {
  const source = `endpoint PutTodo PUT TodoInput /todos/{id: TodoId}/items ? {done: Boolean?, ...Paging} # {\`X-Token\`: String} -> {
  @Deprecated
  200 -> Todo[] # {total: Integer}
  404 -> { String }
}`;

  it("highlights the signature", () => {
    expectTypes(source, [
      ["endpoint", "keyword"],
      ["PutTodo", "function"],
      ["PUT", "http-method/constant"],
      ["TodoInput", "class-name"],
      ["/todos/", "path/string"],
      ["id", "path/string > parameter/variable"],
      ["TodoId", "path/string > class-name"],
      ["/items", "path/string"],
      ["?", "operator"],
      ["done", "property"],
      ["...", "operator"],
      ["Paging", "class-name"],
      ["#", "operator"],
      ["`X-Token`", "property"],
      ["->", "operator"],
    ]);
  });

  it("highlights the responses", () => {
    expectTypes(source, [
      ["@Deprecated", "annotation > annotation-name/attr-name"],
      ["200", "status-code/number"],
      ["Todo", "class-name"],
      ["total", "property"],
      ["404", "status-code/number"],
    ]);
  });

  it("highlights every HTTP method", () => {
    ["GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE"].forEach((method) =>
      assert.equal(typeOf(`endpoint E ${method} /e -> {}`, method), "http-method/constant"),
    );
  });
});

describe("channels, rpcs and parts", () => {
  it("highlights the definitions", () => {
    expectTypes(
      "channel Queue -> String\nrpc GetTodo {\n  ...Paging,\n  id: Integer\n} -> Todo ! TodoError\npart Paging {\n  page: Integer\n}",
      [
        ["channel", "keyword"],
        ["Queue", "class-name"],
        ["rpc", "keyword"],
        ["GetTodo", "function"],
        ["...", "operator"],
        ["!", "operator"],
        ["TodoError", "class-name"],
        ["part", "keyword"],
        ["Paging", "class-name", 1],
        ["page", "property"],
      ],
    );
  });
});

describe("annotations", () => {
  it("highlights annotations and their arguments", () => {
    expectTypes(
      `@Name("users")
@Meta(info: {"owner": "team", level: High}, values: [1, 2.5], enabled: false, none: null)
type User {}`,
      [
        ["@Name", "annotation > annotation-name/attr-name"],
        ['"users"', "annotation > string"],
        ["info", "annotation > property"],
        ['"owner"', "annotation > string"],
        ["High", "annotation > constant"],
        ["2.5", "annotation > number"],
        ["false", "annotation > boolean"],
        ["null", "annotation > null/keyword"],
        ["[", "annotation > punctuation"],
      ],
    );
  });
});

describe("corpus", () => {
  const sources = corpus();

  it("finds the Wirespec sources of the repository", () => assert.ok(sources.length > 50));

  sources.forEach(({ name, source }) =>
    it(`tokenizes every token of ${name}`, () => {
      const untokenized = tokenize(source).filter(({ text, types }) => text.trim() !== "" && types.length === 0);
      assert.deepEqual(untokenized, []);
    }),
  );
});
