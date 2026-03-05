type Todo {
  id: Integer,
  title: String,
  completed: Boolean
}

type TodoInput {
  title: String,
  completed: Boolean
}

type Error {
  message: String
}

endpoint GetTodos GET /api/todos -> {
  200 -> Todo[]
}

endpoint GetTodoById GET /api/todos/{id: Integer} -> {
  200 -> Todo
  404 -> Error
}

endpoint CreateTodo POST TodoInput /api/todos -> {
  200 -> Todo
}

endpoint UpdateTodo PUT TodoInput /api/todos/{id: Integer} -> {
  200 -> Todo
  404 -> Error
}

endpoint DeleteTodo DELETE /api/todos/{id: Integer} -> {
  204 -> Unit
  404 -> Error
}
