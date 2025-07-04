type TodoId -> String(/email/)
type Address -> String

type TodoDto {
    id: TodoId,
    name: String,
    done: Boolean @Mock('adress')
}

type Error {
    @Desctripnog("dgsg")
    @Mock("")
    code: Interger(1,6),
    description: String
}

endpoint GetTodos GET /api/todos ? {done:Boolean?} -> {
    200 -> TodoDto[] # {total:Integer}
    500 -> Error
}
