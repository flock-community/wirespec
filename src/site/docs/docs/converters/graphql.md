# GraphQL

Wirespec converts GraphQL schemas (SDL, `.graphql`) into Wirespec. The conversion is lossless: every type system
definition and extension of the [GraphQL specification](https://spec.graphql.org/) is kept, either as a native Wirespec
construct or, where Wirespec has no syntax for it, as an annotation. Descriptions, directives, field arguments, default
values, interface implementations and extensions can all be read back from the Wirespec definitions.

Only schema documents can be converted. Executable documents (queries, mutations, subscriptions and fragments) are
rejected.

Wirespec also emits GraphQL: Wirespec converted from GraphQL becomes the exact schema it came from, and any other
Wirespec becomes the closest GraphQL schema. See [Emitting GraphQL](#emitting-graphql).

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
| `interface`                                    | `part` with the fields, spread into a `type` with `@GraphQLInterface("<part>")` |
| `input`                                        | `type` with `@GraphQLInput`                               |
| `enum`                                         | `enum`                                                    |
| `union`                                        | `type X = A \| B`                                         |
| `scalar`                                       | refined `type X = String`                                 |
| fields of `Query`, `Mutation`, `Subscription`  | [`rpc`](../language/rpc.mdx), one per field               |
| `schema`                                       | `type Schema` with `@GraphQLSchema(query: "...", ...)`    |
| `directive @x`                                 | `type XDirective` with `@GraphQLDirectiveDefinition`      |
| `extend ...`                                   | a definition named `...Extension` with `@GraphQLExtend`   |

The root operation types are taken from the `schema` definition, or default to `Query`, `Mutation` and `Subscription`
when there is none. Each of their fields becomes an `rpc` named after the root type and the field, whose parameters are
the field arguments and whose result is the field type. `@GraphQLField` names the root type each `rpc` belongs to, so
the root type is rebuilt from its rpcs and needs no Wirespec definition of its own. It only gets one, an empty `type`
holding its annotations, when it has a description, directives or interfaces, has no fields, or is referenced as a type.

### Interfaces

The fields of an interface become a `part`, named after the interface with a `Fields` suffix.
The interface itself stays a `type` that spreads the part, so other definitions can still refer to it, and is marked
with `@GraphQLInterface` naming the part. A type that
implements the interface spreads the part as well, as long as it declares the interface fields exactly as the interface
does: in the same order and with the same type, description, arguments and directives. Otherwise it keeps its own fields.

```graphql
interface Node {
  id: ID!
}

type User implements Node {
  id: ID!
  email: String
}
```

```wirespec
part NodeFields {
  id: ID
}

@GraphQLInterface("NodeFields")
type Node {
  ...NodeFields
}

@GraphQLImplements(["Node"])
type User {
  ...NodeFields,
  email: String?
}
```

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
| `@GraphQLField(parent: "Query", name: "user")`                                | the root type and field name an `rpc` came from; `extend: "1"` marks the first `extend type` block of a root type without a Wirespec type of its own, and so on |
| `@GraphQLEnumValue(value: "ADMIN", description: "...", directives: [...])`    | the description and directives of an enum value                                            |
| `@GraphQLInterface("NodeFields")`                                             | an interface, naming the part that holds its fields (bare when the interface has no fields) |
| `@GraphQLInput`                                                               | an input object                                                                            |
| `@GraphQLUnion`, `@GraphQLEnum`                                               | a union or enum without members, which Wirespec cannot write as `union` or `enum`          |
| `@GraphQLSchema(query: "Root")`                                               | a `schema` definition, mapping each operation to its root type                              |
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

@GraphQLField(parent: "Query", name: "todo")
rpc QueryTodo {
  id: ID
} -> Todo?

@GraphQLField(parent: "Mutation", name: "addTodo")
rpc MutationAddTodo {
  input: NewTodo
} -> Todo

@GraphQLBuiltIn
type ID = String
```

## Emitting GraphQL

The `GraphQL` emitter writes one `schema.graphql` for all modules together:

```shell
wirespec compile -i ./wirespec -l GraphQL
```

Wirespec converted from GraphQL carries the annotations above, and the emitter turns it back into the exact schema it
came from. Any other Wirespec gets the closest GraphQL:

| Wirespec                                | GraphQL                                                                   |
|-----------------------------------------|---------------------------------------------------------------------------|
| `type`                                  | `type`, or `input` when an rpc takes it                                   |
| a `type` an rpc both takes and returns  | `type X` and `input XInput`                                               |
| `enum`, `type U = A \| B`               | `enum`, `union`                                                           |
| refined `type X = String(...)`          | `scalar X` (the constraint is left out)                                   |
| `rpc`                                   | a field of `Query`, its parameters as arguments                           |
| `Integer32` / `Number` / `String` / `Boolean` | `Int` / `Float` / `String` / `Boolean`                              |
| `Integer`, `Bytes`, `Any` and dictionaries, `Unit` | the custom scalars `Long`, `Bytes`, `JSON`, `Void`, declared in the schema |
| a comment on a definition               | its description                                                           |
| field defaults                          | default values of input fields                                            |

Field names and enum values that are not valid GraphQL names, such as `due-date`, have the invalid characters
replaced by `_`. Endpoints, channels and rpc error types have no GraphQL counterpart: they are left out with a warning.

```wirespec
type User {
  name: String
}

rpc CreateUser {
  user: User
} -> User
```

```graphql
type User {
  name: String!
}

input UserInput {
  name: String!
}

type Query {
  createUser(
    user: UserInput!
  ): User!
}
```

## Limitations

- **Formatting**: `#` comments, commas and the optional leading `&` and `|` separators are insignificant in GraphQL and
  are not kept. Descriptions are kept by value, so a block string description comes back as a regular string.
- **Interfaces**: a part shares the interface fields, but Wirespec has no inheritance, so which types implement an
  interface is recorded in `@GraphQLImplements` rather than expressed as subtypes.
- **Multiple files**: each file is converted on its own. A schema split over several files references types declared in
  the other files; convert all of them to compile the result.
