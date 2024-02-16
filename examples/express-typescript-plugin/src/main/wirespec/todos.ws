refined TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g

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
    description: String
}

endpoint GetAllTodos GET /todos -> {
    200 -> Todo[]
}

endpoint GetTodoById GET /todos/{id: String} -> {
    200 -> Todo
    404 -> Unit
}

endpoint SaveTodo POST TodoInput /todos -> {
    200 -> Todo
    400 -> Unit
}

endpoint DeleteTodo DELETE /todos/{id: String} -> {
    200 -> Todo
    404 -> Unit
}