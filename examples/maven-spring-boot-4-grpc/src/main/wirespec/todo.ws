type Todo {
  id: String,
  name: String,
  done: Boolean
}

type TodoInput {
  name: String,
  done: Boolean
}

type TodoList {
  todos: Todo[]
}

type Error {
  code: String,
  message: String
}

rpc CreateTodo(todo: TodoInput) -> Todo ! Error
rpc ListTodos() -> TodoList
