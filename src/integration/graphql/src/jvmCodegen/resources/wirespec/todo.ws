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

type TodoError {
  message: String
}

@GraphQLQuery
@GraphQLName("todo")
rpc QueryTodo {
  id: ID
} -> Todo?

@GraphQLQuery
rpc Todos {
  done: Boolean?
} -> Todo[]

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

type SearchResult = Todo | User

@GraphQLQuery
rpc Search {
  text: String
} -> SearchResult[]

@GraphQLSubscription
rpc TodoAdded {} -> Todo

rpc CountTodos {} -> Integer

@GraphQLBuiltIn
type ID = String
