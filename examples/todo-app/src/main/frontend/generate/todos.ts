interface TodoId {
  value: string
}

interface Todo {
  id: TodoId,
  name: string,
  done: boolean
}

interface TodoInput {
  name: string,
  done: boolean
}

