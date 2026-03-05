---
title: IR Model
sidebar_position: 4
---

# IR Model

Wirespec compiles `.ws` source files into code for multiple target languages. After parsing Wirespec source into an AST, the compiler uses an **Intermediate Representation (IR)** — a language-neutral tree that sits between the parser and code generation. This page explains the IR pipeline and the model each Wirespec definition is converted into.

## Pipeline Overview

The IR pipeline transforms parsed Wirespec definitions into target-language source code through three stages:

```
Wirespec AST (definitions)
        │
        ▼
   ┌─────────┐
   │ Convert  │  AST → IR (language-neutral element tree)
   └────┬─────┘
        │
        ▼
   ┌───────────┐
   │ Transform  │  IR → IR (reshape for a specific target language)
   └────┬──────┘
        │
        ▼
   ┌──────────┐
   │ Generate  │  IR → Source code string
   └──────────┘
```

**Convert** maps each Wirespec definition to a language-neutral IR tree. It also produces a shared model containing the base interfaces all generated code depends on.

**Transform** reshapes the IR to match the conventions of a specific target language (naming, types, patterns) without changing its structure.

**Generate** walks the final IR tree and emits the target-language source code as a string.

## Shared Model

The converter produces a **shared model** — a single file called `Wirespec` that contains the base interfaces and types all generated code depends on. This file is emitted once per target language and provides the common vocabulary that generated types, endpoints, and channels build upon.

The shared model is wrapped in a `Wirespec` namespace and defines:

- **Core interfaces** — `Model`, `Enum`, `Refined<T>`, `Endpoint`, and `Channel`. Every generated definition implements one of these. For example, all generated types implement `Wirespec.Model` which provides a `validate` function.
- **HTTP primitives** — A `Method` enum (`GET`, `POST`, etc.) and typed `Request<T>` / `Response<T>` interfaces that describe HTTP messages with path, queries, headers, and body.
- **Serialization contracts** — Serializer and deserializer interfaces for three layers: body (binary), path (string), and params (string lists). These combine into a unified `Serialization` interface.
- **Raw transport** — `RawRequest` and `RawResponse` structs for untyped HTTP messages, and a `Transportation` interface that users implement to plug in their HTTP client.

Each language emitter transforms the shared model through the same Transform pipeline and may inject additional language-specific interfaces (for example, Java adds `Client` and `Server` interfaces for framework integration).

## Type

A Wirespec `type` is converted into a struct that implements `Wirespec.Model`. Each field in the Wirespec shape becomes a field in the struct, and a `validate` function is generated that returns a list of validation errors.

```wirespec
type Address {
    street: String,
    number: Integer,
    tags: String[]
}
```

```
File("Address") {
    Struct("Address") implements Wirespec.Model {
        Field("street", String)
        Field("number", Integer)
        Field("tags",   Array(String))
        Function("validate") → Array(String)
    }
}
```

When a type has fields that reference other types or refined types, the `validate` function is extended with nested validation — calling `validate` on those fields and collecting the results with field path prefixes.

## Enum

A Wirespec `enum` is converted into an enum that extends `Wirespec.Enum`. Each entry becomes an enum member.

```wirespec
enum Role { ADMIN, USER, GUEST }
```

```
File("Role") {
    Enum("Role") extends Wirespec.Enum {
        Entry("ADMIN")
        Entry("USER")
        Entry("GUEST")
    }
}
```

## Union

A Wirespec `union` is converted into a union with members referencing each entry type.

```wirespec
union Animal { Cat | Dog | Bird }
```

```
File("Animal") {
    Union("Animal") {
        Member("Cat")
        Member("Dog")
        Member("Bird")
    }
}
```

## Refined

A Wirespec `refined` type is converted into a struct that implements `Wirespec.Refined<T>`. It holds the underlying value and a `validate` function that checks the constraint (regex or bounds).

```wirespec
refined Email /^[^@]+@[^@]+$/g
```

```
File("Email") {
    Struct("Email") implements Wirespec.Refined<String> {
        Field("value", String)
        Function("validate") → Boolean  // checks regex
    }
}
```

## Channel

A Wirespec `channel` is converted into an interface that extends `Wirespec.Channel` with an `invoke` function accepting the message type.

```wirespec
channel OrderEvents -> OrderEvent
```

```
File("OrderEvents") {
    Interface("OrderEvents") extends Wirespec.Channel {
        Function("invoke")(message: OrderEvent) → Unit
    }
}
```

## Endpoint

A Wirespec `endpoint` produces the most complex IR structure. It is wrapped in a namespace and contains everything needed for typed, serializable HTTP communication.

```wirespec
endpoint GetTodos GET /todos?{done: Boolean} -> {
    200 -> Todo[]
}
```

```
File("GetTodos") {
    Namespace("GetTodos") extends Wirespec.Endpoint {
        Struct("Path") implements Wirespec.Path {}
        Struct("Queries") implements Wirespec.Queries {
            Field("done", Boolean)
        }
        Struct("RequestHeaders") implements Wirespec.Request.Headers {}
        Struct("Request") implements Wirespec.Request<Unit> {
            Field("path", Path)
            Field("method", Wirespec.Method)
            Field("queries", Queries)
            Field("headers", RequestHeaders)
            Field("body", Unit)
        }
        Union("Response<T>") extends Wirespec.Response<T> {
            Member("Response2XX")
            Member("ResponseTodo")
        }
        Union("Response2XX<T>") extends Response<T> {
            Member("Response200")
        }
        Struct("Response200") {
            Field("status", Integer)
            Field("headers", Headers)
            Field("body", Array(Todo))
        }
        Function("toRawRequest")(serialization, request) → Wirespec.RawRequest
        Function("fromRawRequest")(serialization, request) → Request
        Function("toRawResponse")(serialization, response) → Wirespec.RawResponse
        Function("fromRawResponse")(serialization, response) → Response
        Interface("Handler") {
            Function("getTodos")(request: Request) → Response<*>
        }
    }
}
```

The key parts of an endpoint IR model are:

- **Path, Queries, RequestHeaders** — structs for the typed request components, implementing the corresponding shared interfaces
- **Request** — a struct implementing `Wirespec.Request<T>` with a constructor that assembles path, method, queries, headers, and body
- **Response union hierarchy** — a `Response` union with intermediate unions grouped by status prefix (`Response2XX`) and content type (`ResponseTodo`), with concrete response structs for each status code
- **Conversion functions** — `toRawRequest`, `fromRawRequest`, `toRawResponse`, and `fromRawResponse` that bridge between the typed request/response structs and `Wirespec.RawRequest` / `Wirespec.RawResponse` using the serialization interfaces
- **Handler** — an interface with a function matching the endpoint name, representing the server-side handler contract

## Transform

The transform stage is where language-specific adaptation happens. Each language emitter reshapes the generic IR into a form that generates idiomatic code for its target.

Transforms work by walking the IR tree and replacing nodes. A **Transformer** provides override points for each node kind (types, elements, statements, expressions, fields, parameters). Each override defaults to a recursive traversal — you only override what you want to change.

Language emitters use a high-level `transform { }` block that chains multiple transformations. Common operations include renaming types, replacing type patterns (e.g., mapping `Array` to a language-specific list type), transforming fields conditionally, and injecting elements before or after containers.

Because transforms recurse automatically, a single type rename propagates through every struct field, function parameter, return type, and nested expression in the tree.

## Example: Transform and Generate

Given the IR for the `Address` type shown above, a Java emitter transforms it to match Java conventions:

```
File("Address") {
    Struct("Address") implements Wirespec.Model {
        Field("street", Custom("String"))
        Field("number", Custom("int"))
        Field("tags",   Custom("java.util.List", [Custom("String")]))
        Function("validate") → Custom("java.util.List", [Custom("String")])
    }
}
```

The generator then walks this transformed IR and emits:

```java
public record Address(
    String street,
    int number,
    java.util.List<String> tags
) implements Wirespec.Model {
    public java.util.List<String> validate() {
        return java.util.Collections.emptyList();
    }
}
```

The same IR — after different transforms — produces equivalent code in Kotlin, TypeScript, Python, or Rust.
