type Todo {
  id: ID,
  title: String,
  done: Boolean,
  priority: Priority?,
  owner: User
}

type User {
  name: String
}

enum Priority {
  LOW, MEDIUM, HIGH
}

@GraphQLInput
type NewTodo {
  title: String,
  priority: Priority?
}

type Note {
  text: String
}

type Tag {
  name: String
}

type SearchResult = Note | Tag

type TodoError {
  message: String
}

@GraphQLBuiltIn
type ID = String

@GraphQLQuery
@GraphQLName("todo")
rpc QueryTodo {
  id: ID
} -> Todo?

@GraphQLQuery
rpc Todos {
  done: Boolean?
} -> Todo[]

@GraphQLQuery
rpc Search {
  text: String
} -> SearchResult[]

@GraphQLMutation
rpc AddTodo {
  input: NewTodo
} -> Todo

@GraphQLMutation
rpc CompleteTodo {
  id: ID
} -> Todo ! TodoError

@GraphQLMutation
rpc ClearTodos {} -> Unit

@GraphQLSubscription
rpc TodoAdded {} -> Todo

rpc CountTodos {} -> Integer
