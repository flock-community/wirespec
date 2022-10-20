refined TodoId "^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$"
refined Name "^[0-9a-zA-Z]{1,50}$"
refined DutchPostalCode "^([0-9]{4}[A-Z]{2})$"
refined Date "^([0-9]{2}-[0-9]{2}-20[0-9]{2})$"

type Todos {
  id: TodoId,
  person: Person,
  done: Boolean,
  prio: Integer,
  date: Date
}

type TodoInput {
  name: String,
  done: Boolean
}

type Person {
  firstname: Name,
  lastName: Name,
  age: Integer,
  address: Address
}

type Address {
  street: Name,
  houseNumber: Integer,
  postalCode: DutchPostalCode
}


endpoint FindAll GET /todos?{done: Boolean} Todos[]
endpoint FindById GET /todos/{id: String} Todos?
endpoint Create POST /todos TodoInput -> Todos
endpoint Update PUT /todos TodoInput -> Todos
