refined TodoId /^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g

type Todo {
    id: TodoId,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}
