refined TodoId /^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/g

type Todo {
    id: TodoId,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}

type Error {
    code: String,
    description: String
}
