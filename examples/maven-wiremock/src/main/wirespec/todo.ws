type Todo {
    id: String,
    name: String,
    done: Boolean
}

type Error {
    code: Integer,
    description: String
}

endpoint GetTodos GET /api/todos -> {
    200 -> Todo[]
}

endpoint GetTodoById GET /api/todos/{id: String} -> {
    200 -> Todo
    404 -> Error
}
