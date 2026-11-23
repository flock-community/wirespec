type Todo {
    id: String,
    name: String,
    done: Boolean
}

type TodoError {
    reason: String
}

endpoint GetTodo GET /todos/{id: String} #{token: String} -> {
    200 -> Todo
    404 -> TodoError
}

channel TodoEvents -> Todo
