type Todo {
  id: Integer,
  title: String,
  done: Boolean
}

type TodoInput {
  title: String
}

type ApiError {
  message: String
}

endpoint GetTodos GET /todos -> {
  200 -> Todo[]
}

endpoint GetTodo GET /todos/{id: Integer} -> {
  200 -> Todo
  404 -> ApiError
}

endpoint CreateTodo POST TodoInput /todos -> {
  201 -> Todo
  400 -> ApiError
}
