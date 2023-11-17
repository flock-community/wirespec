refined TodoIdentifier /^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g
refined Name /^[0-9a-zA-Z]{1,50}$/g
refined DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g
refined Date /^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g

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
  id: TodoIdentifier,
  person: Person,
  done: Boolean,
  prio: Integer,
  date: Date
}

type Error {
  reason: String
}

endpoint GetTodos GET /todos -> {
    200 -> Todo[]
}

endpoint GetTodos POST Todo /todos -> {
    200 -> Todo
}

endpoint GetTodos PUT Todo /todos/{id: TodoIdentifier} -> {
    200 -> Todo
    404 -> Error
}

endpoint GetTodos DELETE /todos/{id: TodoIdentifier} -> {
    200 -> Todo
    404 -> Error
}
