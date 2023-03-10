type TodoIdAAAAA {
    id: String
}

type Todo {
    id: TodoIdAAAAA,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}
