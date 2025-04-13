type TodoId {
    id: String
}

type TodoResult {
    id: TodoId,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}
