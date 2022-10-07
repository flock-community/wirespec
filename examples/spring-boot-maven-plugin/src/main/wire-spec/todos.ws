type TodoId {
    value: String
}

type Todo {
    id: TodoId,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}
