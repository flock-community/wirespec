type StringRefinedRegex = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
type StringRefined = String

type IntRefinedNoBound = Integer(_,_)
type IntRefinedLowerBound = Integer(-1,_)
type IntRefinedUpperBound = Integer(_,2)
type IntRefinedLowerAndUpper = Integer(3,4)

type NumberRefinedNoBound = Number(_,_)
type NumberRefinedLowerBound = Number(-1.0,_)
type NumberRefinedUpperBound = Number(_,2.0)
type NumberRefinedLowerAndUpper = Number(3.0,4.0)

type TodoId = String

endpoint GetTodos GET /todos ? {done: Boolean?} -> {
  200 -> Todo[]
  404 -> Error
}

endpoint CreateTodo POST TodoInput /todos -> {
  200 -> Todo
  404 -> Error
}

endpoint GetTodoById GET /todos/{id: TodoId} -> {
  200 -> Todo
  404 -> Error
}

endpoint UpdateTodo PUT TodoInput /todos/{id: TodoId} -> {
  200 -> Todo
  404 -> Error
}

endpoint DeleteTodo DELETE /todos/{id: TodoId} -> {
  200 -> Todo
  404 -> Error
}

endpoint UploadAttachment POST Attachment /todos/{id: TodoId}/upload -> {
  201 -> Unit
  500 -> Error
}

type Todo {
  id: TodoId,
  name: String,
  done: Boolean
}

type TodoInput {
  name: String,
  done: Boolean
}

type Error {
  code: String,
  description: String?
}

type Attachment {
  plain: Bytes,
  csv: Bytes,
  json: Todo
}
