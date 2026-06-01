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

rpc CreateTodo(todo: TodoInput) -> Todo
rpc ListTodos() -> TodoList
