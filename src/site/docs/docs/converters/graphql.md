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
| `schema`                                       | `type Schema` with `@GraphQLSchema(query: "...", ...)`    |
| `directive @x`                                 | `type XDirective` with `@GraphQLDirectiveDefinition`      |
| `extend ...`                                   | a definition named `...Extension` with `@GraphQLExtend`   |

The root operation types are taken from the `schema` definition, or default to `Query`, `Mutation` and `Subscription`
when there is none. Each of their fields becomes an `rpc` whose parameters are the field arguments and whose result is
the field type, marked with `@GraphQLQuery`, `@GraphQLMutation` or `@GraphQLSubscription`:

```wirespec
@GraphQLMutation
rpc AddTodo {
  input: NewTodo
} -> Todo
```

- The `rpc` is named after the field (`addTodo` becomes `AddTodo`). When that name is taken, for example by `type Todo`
  for a field `todo`, it gets the root type as prefix (`QueryTodo`) and `@GraphQLName("todo")` keeps the field name.
- A root type with another name than the default is named in the annotation: `@GraphQLQuery("QueryRoot")`.
- The rpcs of an `extend type Query` block are marked `@GraphQLQuery(extend: "1")`, the next block `"2"`, and so on.

The root type is rebuilt from its rpcs and needs no Wirespec definition of its own. It only gets one, an empty `type`
holding its annotations, when it has a description, directives or interfaces, has no fields, or is referenced as a type.

### Interfaces

An interface becomes a `type` holding the interface fields, marked with `@GraphQLInterface`. A type that implements
interfaces declares their fields itself, as it does in GraphQL, and lists the interfaces in `@GraphQLImplements`.

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
@GraphQLInterface
type Node {
  id: ID
}

@GraphQLImplements(["Node"])
type User {
  id: ID,
  email: String?
}
```

### Default values

A default value of an input field or a directive argument becomes a Wirespec [default value](../language/types.mdx)
when Wirespec can hold it exactly: a string, integer, float, boolean or `null` on a field of the matching type, or an
entry of an enum declared in the same document.

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
  order: Order? = ASC
}
```

Every other default is kept in `@GraphQLDefault`, in GraphQL syntax. That covers lists, input objects, defaults on
custom scalars and `ID`, entries of an enum declared in another document or added by `extend enum` (Wirespec checks an
enum default against the entries of the enum itself), and literals that Wirespec would write differently, such as `2` on
a `Float` or `1e3`. Arguments of root fields become `rpc` parameters, which cannot have a Wirespec default, so their defaults are
always kept in `@GraphQLDefault`.

## Annotations

| Annotation                                                                       | On                  | Carries                                                                                   |
|----------------------------------------------------------------------------------|---------------------|-------------------------------------------------------------------------------------------|
| `@Description("...")`                                                            | definitions, fields | the description of a definition, field or argument                                        |
| `@GraphQLDirective("@key(fields: \"id\")")`                                      | definitions, fields | an applied directive, in GraphQL syntax; repeated for every directive                     |
| `@GraphQLImplements(["Node"])`                                                   | types               | the interfaces an object or interface implements                                          |
| `@GraphQLArgument(name: "first", type: "Int", defaultValue: "10", ...)`          | fields              | an argument of a field that is not an `rpc`; also `description` and `directives`          |
| `@GraphQLDefault("MEDIUM")`                                                      | fields              | a default value that cannot be a Wirespec default (see above), in GraphQL syntax          |
| `@GraphQLQuery`, `@GraphQLMutation`, `@GraphQLSubscription`                     | rpcs                | an `rpc` that is a field of a root operation type; takes the root type when it is not the default (`@GraphQLQuery("QueryRoot")`) and `extend: "1"` for the fields of an `extend type` block |
| `@GraphQLName("todo")`                                                           | rpcs                | the field name of an `rpc` whose name does not read back as that field name              |
| `@GraphQLEnumValue(value: "ADMIN", description: "...", directives: [...])`       | enums               | the description and directives of an enum value; one for every such value                 |
| `@GraphQLInterface`                                                              | types               | an interface                                                                              |
| `@GraphQLInput`                                                                  | types               | an input object                                                                           |
| `@GraphQLUnion`, `@GraphQLEnum`                                                  | types               | a union or enum without members, which Wirespec cannot write as `union` or `enum`         |
| `@GraphQLSchema(query: "Root")`                                                  | `type Schema`       | a `schema` definition, mapping each operation to its root type                            |
| `@GraphQLDirectiveDefinition(name: "key", locations: [...], repeatable: "true")` | `type ...Directive` | a directive definition; its fields are the directive arguments                            |
| `@GraphQLExtend("User")`                                                         | definitions         | an extension of the named type (`@GraphQLExtend` alone for a schema extension)            |
| `@GraphQLName("_Service")`                                                       | definitions         | the GraphQL name of a definition whose name is not a valid Wirespec type name             |
| `@GraphQLType("[_Any!]!")`                                                       | rpcs, fields        | the exact type of a reference to an undeclared type whose name Wirespec cannot spell      |
| `@GraphQLUnionMembers([...])`                                                    | unions              | the same, for union members                                                               |
| `@GraphQLBuiltIn`                                                                | `type ID`           | the `ID` type the converter added                                                         |

_Definitions_ are types, enums, unions, refined types and rpcs, and _fields_ are the fields of a type and the
parameters of an rpc.

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
  priority: Priority? = MEDIUM
}

@GraphQLQuery
@GraphQLName("todo")
rpc QueryTodo {
  id: ID
} -> Todo?

@GraphQLMutation
rpc AddTodo {
  input: NewTodo
} -> Todo

@GraphQLBuiltIn
type ID = String
```

## Wirespec to GraphQL

Wirespec also converts back into a single GraphQL schema. Wirespec converted from GraphQL gives back the schema it came
from. Any other Wirespec gets the closest GraphQL counterpart:

- A `type` becomes an object type, and a refined type a custom `scalar`. A comment becomes the description.
- An `rpc` becomes a field of `Query`, unless it is marked `@GraphQLMutation` or `@GraphQLSubscription`. Its error type is
  not written, because GraphQL returns errors next to the data.
- GraphQL only takes input objects as arguments. A type that an `rpc` takes, directly or through its fields, becomes an
  `input`. When the type is also returned, it is written twice: once as an object type and once as an input object
  named `...Input`.
- `Integer` and `Number` become `Int` and `Float`, whatever their precision, and `Bytes` becomes `String`. `Any` and
  dictionaries become a `JSON` scalar, and `Unit` becomes `Boolean`.
- Endpoints and channels are left out, because GraphQL has nothing like them.

## Limitations

- **Formatting**: `#` comments, commas and the optional leading `&` and `|` separators are insignificant in GraphQL and
  are not kept. Descriptions are kept by value, so a block string description comes back as a regular string.
- **Interfaces**: Wirespec has no inheritance, so an interface is a type of its own and the types that implement it are
  recorded in `@GraphQLImplements` rather than expressed as subtypes.
- **Multiple files**: each file is converted on its own. A schema split over several files references types declared in
  the other files; convert all of them to compile the result.

## Playground

You can try the conversion online: [Playground convert GraphQL](https://playground.wirespec.io/?specification=graphql&emitter=wirespec),
and the other way around: [Playground convert to GraphQL](https://playground.wirespec.io/?specification=wirespec&emitter=graphql)
