import {parse, emit, Emitters, Shared} from "@flock/wirespec"
import CodeBlock from '@theme/CodeBlock';

// language=ws
const example = `
/**
  * A UUID v4 type
  */
type UUID /^[0-9a-f]{8}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{12}$/g
type Date /^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g

type Todo {
  id: UUID,
  name: String,
  description: String,
  done: Boolean,
  prio: Integer,
  date: Date
}

type Error {
  reason: String
}

endpoint GetTodos GET /todos ? {done: Boolean} -> {
    200 -> Todo[] # {count:Integer, page:Integer}
    500 -> Error
}

endpoint PutTodo PUT Todo /todos/{id: UUID} -> {
    200 -> Todo
    404 -> Error
    500 -> Error
}

endpoint PostTodo POST Todo /todos -> {
    200 -> Todo
    500 -> Error
}

endpoint DeleteTodo DELETE /todos/{id: UUID} # {soft:Boolean} -> {
    200 -> Todo
    500 -> Error
}

channel SyncTodo -> Todo
`

export const WirespecExample = () => {
    return <CodeBlock language="wirespec">{example}</CodeBlock>

}

type WirespecProps = {
    emitter: Emitters,
}
export const WirespecShared = ({emitter}:WirespecProps ) => {
    switch (emitter){
        case Emitters.KOTLIN : return  <CodeBlock language="kotlin">{Shared.KOTLIN.source}</CodeBlock>
        case Emitters.JAVA : return  <CodeBlock language="kotlin">{Shared.JAVA.source}</CodeBlock>
        case Emitters.TYPESCRIPT : return  <CodeBlock language="kotlin">{Shared.TYPESCRIPT.source}</CodeBlock>
    }
}

export const WirespecCompile = ({emitter}:WirespecProps) => {
    const ast = parse(example)

    if(ast.result){
        const source = emit(ast.result, emitter, "")
        return <CodeBlock language="kotlin">{source[0].result}</CodeBlock>
    }else{
        return <pre>{ast.errors.join("\n")}</pre>
    }
}
