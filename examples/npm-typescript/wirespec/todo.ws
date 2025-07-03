type TodoId -> String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)

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

endpoint GetTodos GET /api/todos ? {done:Boolean?} -> {
    200 -> TodoDto[] # {total:Integer}
}

endpoint GetTodoById GET /api/todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> Error
}

endpoint PostTodo POST PotentialTodoDto /api/todos -> {
    200 -> TodoDto
}

endpoint DeleteTodoById DELETE /api/todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> Error
}
