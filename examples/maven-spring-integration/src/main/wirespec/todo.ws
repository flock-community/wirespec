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