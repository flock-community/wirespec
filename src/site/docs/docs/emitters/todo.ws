/*
  Identifier of a todo, a UUID
*/
type TodoId = String(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/g)

/*
  How urgent a todo is
*/
enum Priority {
  LOW, MEDIUM, HIGH
}

/*
  A todo item
*/
type Todo {
  id: TodoId,
  title: String,
  done: Boolean = false,
  priority: Priority = MEDIUM,
  tags: String[],
  dueDate: String? = null
}

/*
  The fields of a todo a client can change
*/
type TodoInput {
  title: String,
  priority: Priority
}

/*
  Returned when a call fails
*/
type ApiError {
  code: Integer,
  message: String
}

type TodoCreated {
  todo: Todo
}

type TodoDeleted {
  id: TodoId
}

/*
  Something that happened to a todo
*/
type TodoEvent = TodoCreated | TodoDeleted

/*
  Update the title and priority of a todo
*/
endpoint UpdateTodo PUT TodoInput /todos/{id: TodoId}
  ? {notify: Boolean?}
  # {requestId: String} -> {
  200 -> Todo # {version: Integer}
  404 -> ApiError
}

/*
  Stream of todo events
*/
channel TodoEvents -> TodoEvent

/*
  Mark a todo as done
*/
rpc CompleteTodo {
  id: TodoId
} -> Todo ! ApiError
