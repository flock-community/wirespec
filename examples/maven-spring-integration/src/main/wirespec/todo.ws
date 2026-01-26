type StringRefinedRegex = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
type StringRefined = String

type IntRefinedNoBound = Integer(_,_)
type IntRefinedLowerBound = Integer(-1,_)
type IntRefinedUpperound = Integer(_,2)
type IntRefinedLowerAndUpper = Integer(3,4)

type NumberRefinedNoBound = Number(_,_)
type NumberRefinedLowerBound = Number(-1.0,_)
type NumberRefinedUpperound = Number(_,2.0)
type NumberRefinedLowerAndUpper = Number(3.0,4.0)

endpoint GetTodos GET /todos ? {done: Boolean?} -> {
  200 -> Todo[]
  404 -> Error
}

endpoint CreateTodo POST TodoInput /todos -> {
  200 -> Todo
  404 -> Error
}

endpoint GetTodoById GET /todos/{id: String} -> {
  200 -> Todo
  404 -> Error
}

endpoint UpdateTodo PUT TodoInput /todos/{id: String} -> {
  200 -> Todo
  404 -> Error
}

endpoint DeleteTodo DELETE /todos/{id: String} -> {
  200 -> Todo
  404 -> Error
}

endpoint UploadAttachment POST Attachment /todos/{id: String}/upload -> {
  201 -> Unit
  500 -> Error
}

type Todo {
  id: String,
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
