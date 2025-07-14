import { parse, emit, Emitters, Shared } from "@flock/wirespec";
import CodeBlock from "@theme/CodeBlock";

// language=ws
const example = `
/*
  A UUID v4 type
*/
type UUID = String(/^[0-9a-f]{8}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{12}$/g)

/*
  Date format YYYY-MM-DD
*/
type Date = String(/^20[0-9]{2}-[0-9]{2}-[0-9]{2}$/g)

/*
  Todo shape
*/
type Todo {
  id: UUID,
  name: String,
  description: String,
  done: Boolean,
  prio: Integer,
  date: Date
}

/*
  Error shape
 */
type Error {
  reason: String
}

/*
  Get Todo endpoint
 */
endpoint GetTodos GET /todos ? {done: Boolean} -> {
    200 -> Todo[] # {count:Integer, page:Integer}
    500 -> Error
}

/*
  Update Todo endpoint
 */
endpoint PutTodo PUT Todo /todos/{id: UUID} -> {
    200 -> Todo
    404 -> Error
    500 -> Error
}

/*
  Create Todo endpoint
 */
endpoint PostTodo POST Todo /todos -> {
    200 -> Todo
    500 -> Error
}

/*
  Delete Todo by id
 */
endpoint DeleteTodo DELETE /todos/{id: UUID} # {soft:Boolean} -> {
    200 -> Todo
    500 -> Error
}

/*
  Sync Todo
 */
channel SyncTodo -> Todo
`;

export const WirespecExample = () => {
  return <CodeBlock language="wirespec">{example}</CodeBlock>;
};

type WirespecProps = {
  emitter: Emitters;
};
export const WirespecShared = ({ emitter }: WirespecProps) => {
  switch (emitter) {
    case Emitters.KOTLIN:
      return <CodeBlock language="kotlin">{Shared.KOTLIN.source}</CodeBlock>;
    case Emitters.JAVA:
      return <CodeBlock language="kotlin">{Shared.JAVA.source}</CodeBlock>;
    case Emitters.TYPESCRIPT:
      return (
        <CodeBlock language="kotlin">{Shared.TYPESCRIPT.source}</CodeBlock>
      );
  }
};

export const WirespecCompile = ({ emitter }: WirespecProps) => {
  const ast = parse(example);

  if (ast.result) {
    const source = emit(ast.result, emitter, "");
    return source.map((it) => {
      if (emitter === Emitters.OPENAPI_V2) {
        const json = JSON.parse(it.result);
        return (
          <CodeBlock title={`${it.typeName}.json`} language="json">
            {JSON.stringify(json, null, 4)}
          </CodeBlock>
        );
      }
      return (
        <CodeBlock title={`${it.typeName}.java`} language="kotlin">
          {it.result}
        </CodeBlock>
      );
    });
  } else {
    return <pre>{ast.errors.join("\n")}</pre>;
  }
};
