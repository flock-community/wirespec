type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)

type TodoDto {
    id: TodoId,
    name: String,
    done: Boolean
}

type TodoDtoPatch {
    name: String?,
    done: Boolean?
}

type Error {
    code: Integer,
    description: String
}

endpoint GetTodos GET /api/todos ? {done:Boolean?} -> {
    200 -> TodoDto[] # {total:Integer}
    500 -> Error
}

endpoint PatchTodos PATCH TodoDtoPatch /api/todos/{id:String} #{X-Request-ID: String, Content-Type: String} -> {
    200 -> TodoDto
    500 -> Error
}
