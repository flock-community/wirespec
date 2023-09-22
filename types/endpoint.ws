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

type Todos {
  id: TodoIdentifier,
  person: Person,
  done: Boolean,
  prio: Integer,
  date: Date
}

endpoint GetTodos GET /todos {
    200 -> Todos[]
}
