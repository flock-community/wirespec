type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g

enum Status {
    PUBLIC,
    PRIVATE
}

type Left {
    left: String
}

type Right {
    right: String
}

type Either = Left | Right

type Todo {
    id: TodoId,
    name: String?,
    done: Boolean,
    tags: String[],
    status: Status,
    either: Either
}

channel TodoChannel -> Todo