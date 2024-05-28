type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g

type TodoDto {
    id: TodoId,
    name: String,
    done: Boolean
}

type PotentialTodoDto {
    name: String,
    done: Boolean
}

type Error {
    code: Integer,
    description: String
}

endpoint GetTodos GET /todos -> {
    200 -> TodoDto[]
}

endpoint GetTodoById GET /todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> Error
}

endpoint PostTodo POST PotentialTodoDto /todos -> {
    200 -> TodoDto
}

endpoint DeleteTodoById DELETE /todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> Error
}
