type StoreMutations {
  @GraphQLArgument(name: "input", type: "CreateInput!")
  create: Item,
  @GraphQLArgument(name: "id", type: "ID!")
  @GraphQLArgument(name: "reason", type: "String")
  remove: Boolean
}

@GraphQLInput
type CreateInput {
  name: String
}

type Item {
  id: String,
  @GraphQLArgument(name: "first", type: "Int!")
  related: Item[]?
}

@GraphQLMutation
@GraphQLName("store")
rpc Store {} -> StoreMutations

@GraphQLQuery
@GraphQLName("item")
rpc QueryItem {
  id: String
} -> Item
