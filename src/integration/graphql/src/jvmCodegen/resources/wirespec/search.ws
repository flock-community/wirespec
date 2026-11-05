// The Scala emitter cannot compile a union whose members live in other files, so Scala is generated without this one.
type Note {
  text: String
}

type Tag {
  name: String
}

type SearchResult = Note | Tag

@GraphQLQuery
rpc Search {
  text: String
} -> SearchResult[]
