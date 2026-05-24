type TodoDto {
    id: String,
    name: String,
    done: Boolean
}

type Error {
    code: Integer,
    description: String
}

endpoint GetTodos GET /api/todos -> {
    200 -> TodoDto[]
}

endpoint GetTodoById GET /api/todos/{id: String} -> {
    200 -> TodoDto
    404 -> Error
}
