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
