export const wsExample = `type TodoIdentifier = String(/^[0-9a-f]{8}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{4}\\b-[0-9a-f]{12}$/g)
type Name = String(/^[0-9a-zA-Z]{1,50}$/g)
type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)
type Date = String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g)

// A part is a named set of fields, spread into a shape with ...
part Identified {
  id: TodoIdentifier
}

// Parts can spread other parts
part Audited {
  ...Identified,
  createdAt: Date,
  updatedAt: Date?
}

part Paging {
  page: Integer,
  size: Integer
}

part Tracing {
  \`X-Request-Id\`: String
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
  ...Audited,
  person: Person,
  done: Boolean,
  prio: Integer
}

type TodoError {
  reason: String
}

endpoint GetTodos GET /todos ?{ ...Paging, done: Boolean? } #{ ...Tracing } -> {
    200 -> Todo[] #{ ...Tracing, total: Integer }
}

endpoint PostTodo POST Todo /todos #{ ...Tracing } -> {
    200 -> Todo
}

endpoint PutTodo PUT Todo /todos/{id: TodoIdentifier} -> {
    200 -> Todo
    404 -> TodoError
}

endpoint DeleteTodo DELETE /todos/{id: TodoIdentifier} -> {
    200 -> Todo
    404 -> TodoError
}

// An rpc is a request/response call without HTTP details
rpc GetTodo {
  id: TodoIdentifier
} -> Todo ! TodoError

rpc SearchTodos {
  ...Paging,
  query: String
} -> Todo[]
`;
