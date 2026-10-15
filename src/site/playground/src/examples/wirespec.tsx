export const wsExample = `endpoint GetTodos GET /todos ?{...Paging, done: Boolean?} -> {
    200 -> Todo[]
}

endpoint PostTodo POST Todo /todos -> {
    200 -> Todo
}

endpoint PutTodo PUT Todo /todos/{id: TodoId} -> {
    200 -> Todo
    404 -> TodoError
}

endpoint DeleteTodo DELETE /todos/{id: TodoId} -> {
    200 -> Todo
    404 -> TodoError
}

// A channel describes one-way messages, for example on a message broker
channel TodoUpdates -> Todo

// An rpc is a request/response call that does not prescribe a transport
rpc SearchTodos {
  ...Paging,
  query: String
} -> Todo[] ! TodoError

rpc CompleteTodo {
  id: TodoId
} -> Todo ! TodoError

type TodoId = String(/^[0-9a-f]{8}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{12}$/g)
type Name = String(/^[0-9a-zA-Z]{1,50}$/g)
type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)
type Date = String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g)

part Audited {
  createdAt: Date,
  updatedAt: Date?
}

part Paging {
  page: Integer,
  size: Integer
}

type Address {
  street: Name,
  houseNumber: Integer,
  postalCode: DutchPostalCode
}

type Person {
  firstname: Name,
  lastName: Name,
  age: Integer,
  address: Address
}

type Todo {
  id: TodoId,
  person: Person,
  done: Boolean,
  prio: Integer,
  ...Audited
}

type TodoError {
  reason: String
}
`;

// The example with the annotations that steer the GraphQL output. Annotations come before a comment.
export const wsGraphQLExample = wsExample
  .replace("type Todo {", '@Description("A task to do")\ntype Todo {')
  .replace(
    "  done: Boolean,\n",
    '  @Description("Whether the todo is finished")\n  done: Boolean,\n',
  )
  .replace(
    "// An rpc is",
    '@GraphQLQuery\n@Description("Finds the todos that match the query")\n// An rpc is',
  )
  .replace("rpc CompleteTodo", "@GraphQLMutation\nrpc CompleteTodo");
