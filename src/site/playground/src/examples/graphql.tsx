export const graphqlExample = `"A task to do"
type Todo implements Node {
  id: ID!
  "What needs doing"
  title: String!
  done: Boolean!
  priority: Priority!
  tags: [String!]
  assignee: User @deprecated(reason: "Use \`owners\`.")
  owners(first: Int = 10): [User!]!
}

type User implements Node {
  id: ID!
  name: String!
}

interface Node {
  id: ID!
}

enum Priority {
  LOW
  MEDIUM
  HIGH
}

union SearchResult = Todo | User

input NewTodo {
  title: String!
  priority: Priority = MEDIUM
  tags: [String!] = []
}

scalar DateTime @specifiedBy(url: "https://tools.ietf.org/html/rfc3339")

type Query {
  todos(done: Boolean): [Todo!]!
  todo(id: ID!): Todo
  search(term: String!): [SearchResult!]!
  now: DateTime!
}

type Mutation {
  addTodo(input: NewTodo!): Todo!
  completeTodo(id: ID!): Todo
}

type Subscription {
  todoAdded: Todo!
}
`;
