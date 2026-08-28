type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)

type Todo {
    id: TodoId,
    name: String,
    final: Boolean,
    category: TodoCategory,
    eMail:String
}

type TodoInput {
    name: String,
    done: Boolean
}

type Error {
    code: String,
    description: String
}

enum TodoCategory {
    WORK,
    LIFE
}

graphql GetTodo Query {id: String} -> Todo?

graphql AddTodo Mutation {input: TodoInput} -> Todo

graphql OnTodoAdded Subscription {} -> Todo
