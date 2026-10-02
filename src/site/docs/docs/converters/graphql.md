# GraphQL

Wirespec converts GraphQL schemas (SDL, `.graphql`) into Wirespec. The conversion is lossless: every type system
definition and extension of the [GraphQL specification](https://spec.graphql.org/) is kept, either as a native Wirespec
construct or, where Wirespec has no syntax for it, as an annotation. Descriptions, directives, field arguments, default
values, interface implementations and extensions can all be read back from the Wirespec definitions.

Only schema documents can be converted. Executable documents (queries, mutations, subscriptions and fragments) are
rejected.

## Type Conversion

### Scalars

| GraphQL   | Wirespec                                          |
|-----------|---------------------------------------------------|
| `Int`     | `Integer32`                                       |
| `Float`   | `Number`                                          |
| `String`  | `String`                                          |
| `Boolean` | `Boolean`                                         |
| `ID`      | `ID`, a refined `type ID = String`                |
| `scalar X`| `type X = String`, a refined type                 |

Because `ID` is built into GraphQL it is not declared in the schema, so the converter adds `type ID = String` marked
with `@GraphQLBuiltIn` when the schema uses it.

### Nullability and lists

GraphQL types are nullable unless marked with `!`; Wirespec references are required unless marked with `?`.

| GraphQL      | Wirespec          |
|--------------|-------------------|
| `String!`    | `String`          |
| `String`     | `String?`         |
| `[String!]!` | `String[]`        |
| `[String]`   | `String?[]?`      |
| `[[Int!]]!`  | `Integer32[]?[]`  |

### Definitions

| GraphQL                                        | Wirespec                                                  |
|------------------------------------------------|-----------------------------------------------------------|
| `type`                                         | `type`                                                    |
| `interface`                                    | `type` with `@GraphQLInterface`                           |
| `input`                                        | `type` with `@GraphQLInput`                               |
| `enum`                                         | `enum`                                                    |
| `union`                                        | `type X = A \| B`                                         |
| `scalar`                                       | refined `type X = String`                                 |
| fields of `Query`, `Mutation`, `Subscription`  | [`rpc`](../language/rpc.mdx), one per field               |
| `schema`                                       | `type Schema` with `@GraphQLSchema`                       |
| `directive @x`                                 | `type XDirective` with `@GraphQLDirectiveDefinition`      |
| `extend ...`                                   | a definition named `...Extension` with `@GraphQLExtend`   |

The root operation types are taken from the `schema` definition, or default to `Query`, `Mutation` and `Subscription`
when there is none. Each of their fields becomes an `rpc` named after the root type and the field, whose parameters are
the field arguments and whose result is the field type. The root type itself stays as an empty `type`, so it keeps its
own annotations and can still be referenced.

### Default values

A default value of an input field or a directive argument becomes a Wirespec [default value](../language/types.mdx)
when Wirespec can hold it exactly: a string, integer, float, boolean or `null` on a field of the matching type.

```graphql
input Filter {
  limit: Int = 10
  query: String = "*"
  order: Order = ASC
}
```

```wirespec
@GraphQLInput
type Filter {
  limit: Integer32? = 10,
  query: String? = "*",
  @GraphQLDefault("ASC")
  order: Order?
}
```

Every other default is kept in `@GraphQLDefault`, in GraphQL syntax. That covers enum values, lists, input objects,
defaults on custom scalars and `ID`, and literals that Wirespec would write differently, such as `2` on a `Float` or
`1e3`. Arguments of root fields become `rpc` parameters, which cannot have a Wirespec default, so their defaults are
always kept in `@GraphQLDefault`.

## Annotations

| Annotation                                                                    | Carries                                                                                    |
|-------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `@Description("...")`                                                         | the description of a definition, field or argument                                         |
| `@GraphQLDirective("@key(fields: \"id\")")`                                   | an applied directive, in GraphQL syntax; repeated for every directive                       |
| `@GraphQLImplements(["Node"])`                                                | the interfaces an object or interface implements                                           |
| `@GraphQLArgument(name: "first", type: "Int", defaultValue: "10", ...)`       | an argument of a field that is not an `rpc`; also `description` and `directives`            |
| `@GraphQLDefault("MEDIUM")`                                                   | a default value that cannot be a Wirespec default (see below), in GraphQL syntax            |
| `@GraphQLField(parent: "Query", name: "user")`                                | the root type (its Wirespec name) and field name an `rpc` came from                        |
| `@GraphQLEnumValue(value: "ADMIN", description: "...", directives: [...])`    | the description and directives of an enum value                                            |
| `@GraphQLInterface`, `@GraphQLInput`                                          | the kind of a `type`                                                                       |
| `@GraphQLUnion`, `@GraphQLEnum`                                               | a union or enum without members, which Wirespec cannot write as `union` or `enum`          |
| `@GraphQLSchema`                                                              | a `schema` definition; its fields map operations to root types                              |
| `@GraphQLDirectiveDefinition(name: "key", locations: [...], repeatable: "true")` | a directive definition; its fields are the directive arguments                          |
| `@GraphQLExtend("User")`                                                      | an extension of the named type (`@GraphQLExtend` alone for a schema extension)             |
| `@GraphQLName("_Service")`                                                    | the GraphQL name of a definition whose name is not a valid Wirespec type name               |
| `@GraphQLType("[_Any!]!")`                                                    | the exact type of a reference to an undeclared type whose name Wirespec cannot spell        |
| `@GraphQLUnionMembers([...])`                                                 | the same, for union members                                                                |
| `@GraphQLBuiltIn`                                                             | the `ID` type the converter added                                                          |

Wirespec type names start with an uppercase letter and may not clash with its primitives, so a GraphQL type such as
`_Service` or `String` is renamed (`Service`, `GraphQLString`) and keeps its name in `@GraphQLName`. Field names and enum
values that are not plain Wirespec identifiers are written between backticks, such as `` `type` `` or `` `_internal` ``.

## Example

```graphql
"A task to do"
type Todo {
  id: ID!
  "What needs doing"
  title: String!
  done: Boolean!
  priority: Priority
}

enum Priority {
  LOW
  MEDIUM
  HIGH
}

input NewTodo {
  title: String!
  priority: Priority = MEDIUM
}

type Query {
  todo(id: ID!): Todo
}

type Mutation {
  addTodo(input: NewTodo!): Todo!
}
```

converts to

```wirespec
@Description("A task to do")
type Todo {
  id: ID,
  @Description("What needs doing")
  title: String,
  done: Boolean,
  priority: Priority?
}

enum Priority {
  LOW, MEDIUM, HIGH
}

@GraphQLInput
type NewTodo {
  title: String,
  @GraphQLDefault("MEDIUM")
  priority: Priority?
}

type Query {}

@GraphQLField(parent: "Query", name: "todo")
rpc QueryTodo {
  id: ID
} -> Todo?

type Mutation {}

@GraphQLField(parent: "Mutation", name: "addTodo")
rpc MutationAddTodo {
  input: NewTodo
} -> Todo

@GraphQLBuiltIn
type ID = String
```

## Limitations

- **Formatting**: `#` comments, commas and the optional leading `&` and `|` separators are insignificant in GraphQL and
  are not kept. Descriptions are kept by value, so a block string description comes back as a regular string.
- **Interfaces**: an interface becomes a `type` holding the interface fields. Wirespec has no inheritance, so the
  implementations are recorded in `@GraphQLImplements` rather than expressed as subtypes.
- **Multiple files**: each file is converted on its own. A schema split over several files references types declared in
  the other files; convert all of them to compile the result.
