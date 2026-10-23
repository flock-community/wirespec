type TodoId = String(/^[0-9]+$/g)

type Todo {
    id: TodoId,
    name: String,
    final: Boolean,
    owner: Owner,
    tags: String[],
    houseNumber: Integer,
    description: String?
}

type Owner {
    name: String
}

endpoint GetTodo GET /todos/{id: TodoId} -> {
    200 -> Todo
}

enum TodoCategory {
    WORK,
    LIFE
}

type Archive {
    category: TodoCategory,
    empty: Empty
}

type Empty {}
