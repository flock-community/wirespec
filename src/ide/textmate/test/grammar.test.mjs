import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { corpus } from "./corpus.mjs";
import { tokenize } from "./tokenize.mjs";

const scopeOf = (source, text, occurrence = 0) => {
  const token = tokenize(source).filter((it) => it.text === text)[occurrence];
  assert.ok(token, `no token ${JSON.stringify(text)} (occurrence ${occurrence}) in ${JSON.stringify(source)}`);
  return token.scopes.at(-1);
};

const expectScopes = (source, expected) =>
  expected.forEach(([text, scope, occurrence]) =>
    assert.equal(scopeOf(source, text, occurrence), scope, `scope of ${JSON.stringify(text)}`),
  );

describe("comments", () => {
  it("highlights line, block and doc comments", () => {
    const source = "// line\n/* block */\n/** doc */\ntype A {}";
    expectScopes(source, [
      [" line", "comment.line.double-slash.wirespec"],
      [" block ", "comment.block.wirespec"],
      [" doc ", "comment.block.documentation.wirespec"],
    ]);
  });

  it("continues a block comment over several lines", () => {
    expectScopes("/*\n  type Foo {}\n*/", [["  type Foo {}", "comment.block.wirespec"]]);
  });
});

describe("types", () => {
  const source = `type Todo {
  id: TodoId,
  name: String?,
  tags: String[],
  matrix: Integer32[][]?,
  labels: { String },
  \`X-Name\`: Number32,
  bytes: Bytes,
  flag: Boolean,
  nothing: Unit,
  anything: Any
}`;

  it("highlights the definition", () => {
    expectScopes(source, [
      ["type", "keyword.declaration.type.wirespec"],
      ["Todo", "entity.name.type.wirespec"],
      ["{", "punctuation.section.block.begin.wirespec"],
      ["}", "punctuation.section.block.end.wirespec", 1],
    ]);
  });

  it("highlights fields and their types", () => {
    expectScopes(source, [
      ["id", "variable.other.property.wirespec"],
      [":", "punctuation.separator.key-value.wirespec"],
      ["TodoId", "entity.name.type.wirespec"],
      [",", "punctuation.separator.comma.wirespec"],
      ["String", "support.type.primitive.wirespec"],
      ["?", "keyword.operator.nullable.wirespec"],
      ["[]", "keyword.operator.array.wirespec"],
      ["Integer32", "support.type.primitive.wirespec"],
      ["{", "punctuation.section.block.begin.wirespec", 1],
      ["`X-Name`", "variable.other.property.wirespec"],
      ["Number32", "support.type.primitive.wirespec"],
      ["Bytes", "support.type.primitive.wirespec"],
      ["Boolean", "support.type.primitive.wirespec"],
      ["Unit", "support.type.builtin.wirespec"],
      ["Any", "support.type.builtin.wirespec"],
    ]);
  });

  it("highlights default values", () => {
    const defaults = `type Config {
  name: String = "anon\\"ymous",
  retries: Integer = 3,
  port: Integer32(0, 65535) = 8080,
  ratio: Number = -1.5,
  active: Boolean = true,
  nickname: String? = null,
  status: Status = ACTIVE,
  order: Order = \`asc\`
}`;
    expectScopes(defaults, [
      ["=", "keyword.operator.assignment.wirespec"],
      ['"anon', "string.quoted.double.wirespec"],
      ['\\"', "constant.character.escape.wirespec"],
      ["3", "constant.numeric.integer.wirespec"],
      ["0", "constant.numeric.integer.wirespec"],
      ["65535", "constant.numeric.integer.wirespec"],
      ["8080", "constant.numeric.integer.wirespec"],
      ["-1.5", "constant.numeric.decimal.wirespec"],
      ["true", "constant.language.boolean.wirespec"],
      ["null", "constant.language.null.wirespec"],
      ["ACTIVE", "variable.other.enummember.wirespec"],
      ["`asc`", "variable.other.enummember.wirespec"],
    ]);
  });

  it("highlights refined types", () => {
    const source = `type Name = String(/^[0-9a-z]{1,50}\\/x$/g)
type Age = Integer(0, _)
type Score = Number(-1.0, 10.0)
type Id = String(uuid-v7)
type At {
  at: String?(date-time)
}`;
    expectScopes(source, [
      ["Name", "entity.name.type.wirespec"],
      ["=", "keyword.operator.assignment.wirespec"],
      ["String", "support.type.primitive.wirespec"],
      ["(", "punctuation.section.constraint.begin.wirespec"],
      ["/", "punctuation.definition.string.begin.wirespec"],
      ["^", "keyword.control.anchor.regexp.wirespec"],
      ["[0-9a-z]", "constant.other.character-class.set.regexp.wirespec"],
      ["{1,50}", "keyword.operator.quantifier.regexp.wirespec"],
      ["\\/", "constant.character.escape.regexp.wirespec"],
      ["x", "string.regexp.wirespec"],
      ["g", "keyword.other.regex-flags.wirespec"],
      [")", "punctuation.section.constraint.end.wirespec"],
      ["_", "constant.language.unbounded.wirespec"],
      ["-1.0", "constant.numeric.decimal.wirespec"],
      ["uuid-v7", "constant.language.format.wirespec"],
      ["date-time", "constant.language.format.wirespec"],
    ]);
  });

  it("highlights unions", () => {
    expectScopes("type Result = Win | Loss", [
      ["Result", "entity.name.type.wirespec"],
      ["Win", "entity.name.type.wirespec"],
      ["|", "keyword.operator.union.wirespec"],
      ["Loss", "entity.name.type.wirespec"],
    ]);
  });
});

describe("enums", () => {
  it("highlights the entries", () => {
    const source = "enum Status {\n  ACTIVE, `in-active`, Pending, 42, snake_case\n}\ntype A {}";
    expectScopes(source, [
      ["enum", "keyword.declaration.enum.wirespec"],
      ["Status", "entity.name.type.enum.wirespec"],
      ["{", "punctuation.section.block.begin.wirespec"],
      ["ACTIVE", "variable.other.enummember.wirespec"],
      ["`in-active`", "variable.other.enummember.wirespec"],
      ["Pending", "variable.other.enummember.wirespec"],
      ["42", "variable.other.enummember.wirespec"],
      ["snake_case", "variable.other.enummember.wirespec"],
      ["}", "punctuation.section.block.end.wirespec"],
      ["A", "entity.name.type.wirespec"],
    ]);
  });
});

describe("endpoints", () => {
  const source = `endpoint PutTodo PUT TodoInput /todos/{id: TodoId}/items ? {done: Boolean?, ...Paging} # {\`X-Token\`: String} -> {
  @Deprecated
  200 -> Todo[] # {total: Integer}
  404 -> { String }
  500 -> Unit
}
type After {}`;

  it("highlights the signature", () => {
    expectScopes(source, [
      ["endpoint", "keyword.declaration.endpoint.wirespec"],
      ["PutTodo", "entity.name.function.endpoint.wirespec"],
      ["PUT", "support.constant.http-method.wirespec"],
      ["TodoInput", "entity.name.type.wirespec"],
      ["/", "punctuation.separator.path.wirespec"],
      ["todos", "string.unquoted.path.wirespec"],
      ["{", "punctuation.section.parameter.begin.wirespec"],
      ["id", "variable.parameter.path.wirespec"],
      ["TodoId", "entity.name.type.wirespec"],
      ["}", "punctuation.section.parameter.end.wirespec"],
      ["items", "string.unquoted.path.wirespec"],
      ["?", "keyword.operator.query.wirespec"],
      ["done", "variable.parameter.query.wirespec"],
      ["?", "keyword.operator.nullable.wirespec", 1],
      ["...", "keyword.operator.spread.wirespec"],
      ["Paging", "entity.name.type.wirespec"],
      ["#", "keyword.operator.headers.wirespec"],
      ["`X-Token`", "variable.parameter.header.wirespec"],
      ["->", "keyword.operator.arrow.wirespec"],
      ["{", "punctuation.section.responses.begin.wirespec", 3],
    ]);
  });

  it("highlights the responses", () => {
    expectScopes(source, [
      ["Deprecated", "storage.type.annotation.wirespec"],
      ["200", "constant.numeric.status-code.wirespec"],
      ["->", "keyword.operator.arrow.wirespec", 1],
      ["Todo", "entity.name.type.wirespec"],
      ["total", "variable.parameter.header.wirespec"],
      ["404", "constant.numeric.status-code.wirespec"],
      ["{", "punctuation.section.block.begin.wirespec", 5],
      ["String", "support.type.primitive.wirespec", 1],
      ["500", "constant.numeric.status-code.wirespec"],
      ["}", "punctuation.section.responses.end.wirespec", 5],
    ]);
  });

  it("closes the endpoint after its responses", () => {
    const after = tokenize(source).find((it) => it.text === "After");
    assert.deepEqual(after.scopes, ["source.wirespec", "entity.name.type.wirespec"]);
  });

  it("highlights every HTTP method", () => {
    ["GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE"].forEach((method) =>
      assert.equal(scopeOf(`endpoint E ${method} /e -> {}`, method), "support.constant.http-method.wirespec"),
    );
  });

  it("does not take a dictionary response for the responses block", () => {
    expectScopes("endpoint E GET /e -> {\n  200 -> {String}\n}\ntype A {}", [
      ["{", "punctuation.section.responses.begin.wirespec"],
      ["{", "punctuation.section.block.begin.wirespec", 1],
      ["}", "punctuation.section.responses.end.wirespec", 1],
      ["A", "entity.name.type.wirespec"],
    ]);
  });
});

describe("channels", () => {
  it("highlights the definition", () => {
    expectScopes("channel Queue -> String", [
      ["channel", "keyword.declaration.channel.wirespec"],
      ["Queue", "entity.name.type.channel.wirespec"],
      ["->", "keyword.operator.arrow.wirespec"],
      ["String", "support.type.primitive.wirespec"],
    ]);
  });
});

describe("rpcs", () => {
  it("highlights the definition", () => {
    expectScopes("rpc GetTodo {\n  id: Integer,\n  ...Paging\n} -> Todo ! TodoError\nrpc Ping {} -> String", [
      ["rpc", "keyword.declaration.rpc.wirespec"],
      ["GetTodo", "entity.name.function.rpc.wirespec"],
      ["id", "variable.other.property.wirespec"],
      ["...", "keyword.operator.spread.wirespec"],
      ["Paging", "entity.name.type.wirespec"],
      ["->", "keyword.operator.arrow.wirespec"],
      ["Todo", "entity.name.type.wirespec"],
      ["!", "keyword.operator.error.wirespec"],
      ["TodoError", "entity.name.type.wirespec"],
      ["Ping", "entity.name.function.rpc.wirespec"],
    ]);
  });
});

describe("parts", () => {
  it("highlights the definition", () => {
    expectScopes("part Audited {\n  ...Identified,\n  createdAt: String\n}", [
      ["part", "keyword.declaration.part.wirespec"],
      ["Audited", "entity.name.type.part.wirespec"],
      ["...", "keyword.operator.spread.wirespec"],
      ["Identified", "entity.name.type.wirespec"],
      ["createdAt", "variable.other.property.wirespec"],
    ]);
  });
});

describe("annotations", () => {
  it("highlights annotations and their arguments", () => {
    const source = `@Deprecated
@Name("users")
@Timeout(seconds: 5, ratio: 0.5, enabled: false)
@Tags(["a", "b"])
@Meta(info: {"owner": "team", level: High}, values: [1, 2])
type User {
  @JsonProperty("user_name")
  name: String
}`;
    expectScopes(source, [
      ["@", "punctuation.definition.annotation.wirespec"],
      ["Deprecated", "storage.type.annotation.wirespec"],
      ["Name", "storage.type.annotation.wirespec"],
      ["(", "punctuation.section.arguments.begin.wirespec"],
      ['"', "punctuation.definition.string.begin.wirespec"],
      ["users", "string.quoted.double.wirespec"],
      [")", "punctuation.section.arguments.end.wirespec"],
      ["seconds", "variable.parameter.annotation.wirespec"],
      ["5", "constant.numeric.integer.wirespec"],
      ["0.5", "constant.numeric.decimal.wirespec"],
      ["false", "constant.language.boolean.wirespec"],
      ["[", "punctuation.section.array.begin.wirespec"],
      ["]", "punctuation.section.array.end.wirespec"],
      ["{", "punctuation.section.dictionary.begin.wirespec"],
      ['"owner"', "string.quoted.double.wirespec"],
      ["level", "variable.parameter.annotation.wirespec"],
      ["High", "constant.other.wirespec"],
      ["}", "punctuation.section.dictionary.end.wirespec"],
      ["JsonProperty", "storage.type.annotation.wirespec"],
      ["name", "variable.other.property.wirespec"],
    ]);
  });
});

describe("recovery", () => {
  it("does not let an unclosed block swallow the next definition", () => {
    const after = tokenize("type A {\n  a: String\n\ntype B {}").find((it) => it.text === "B");
    assert.deepEqual(after.scopes, ["source.wirespec", "entity.name.type.wirespec"]);
  });
});

describe("corpus", () => {
  const sources = corpus();

  it("finds the Wirespec sources of the repository", () => assert.ok(sources.length > 50));

  sources.forEach(({ name, source }) =>
    it(`scopes every token of ${name}`, () => {
      const unscoped = tokenize(source).filter(
        ({ text, scopes }) => text.trim() !== "" && scopes.every((scope) => /^(source|meta)\./.test(scope)),
      );
      assert.deepEqual(unscoped, []);
    }),
  );
});
