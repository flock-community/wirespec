type Todo { name: String }

rpc CreateTodo(todo: Todo) -> Todo
rpc DeleteTodo(id: String) -> Unit
