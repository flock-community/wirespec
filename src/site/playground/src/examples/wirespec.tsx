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

// Refined with a built-in format: date, date-time, email, uuid or uuid-v1 to uuid-v8
type TodoId = String(uuid-v4)
type Date = String(date)
type Timestamp = String(date-time)
type Email = String(email)

// Refined with a regular expression of your own
type Name = String(/^[0-9a-zA-Z]{1,50}$/g)
type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)

part Audited {
  createdAt: Timestamp,
  updatedAt: Timestamp?
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
  email: Email,
  age: Integer,
  address: Address
}

type Todo {
  id: TodoId,
  person: Person,
  done: Boolean,
  prio: Integer,
  dueDate: Date?,
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

// The example with the annotations that steer the AsyncAPI output. Annotations come before a comment.
export const wsAsyncAPIExample = wsExample.replace(
  "// A channel describes",
  '@AsyncAPIChannel(address: "todos/updates")\n@AsyncAPISend("publishTodoUpdate")\n// A channel describes',
);
