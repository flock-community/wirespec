# Wirespec → gRPC on Spring Boot 4

This example implements a **gRPC server whose contract is defined in Wirespec**. The Wirespec
`rpc` definitions in [`todo.ws`](src/main/wirespec/todo.ws) are the single source of truth; a
dependency-free `.proto` schema is generated from them, `protoc` turns that into gRPC stubs, and
those stubs are implemented as Spring beans on top of the official
[Spring gRPC](https://docs.spring.io/spring-grpc/reference/) starter.

## The contract

```wirespec
type Todo      { id: String, name: String, done: Boolean }
type TodoInput { name: String, done: Boolean }
type TodoList  { todos: Todo[] }

rpc CreateTodo(todo: TodoInput) -> Todo
rpc ListTodos() -> TodoList
```

## The build pipeline

Everything runs in Maven's `generate-sources` phase, in order:

```
todo.ws
   │  ① wirespec-maven-plugin  (-l Protobuf)
   ▼
target/generated-sources/proto/todo.proto      ← dependency-free proto3
   │  ② protobuf-maven-plugin  (protoc + protoc-gen-grpc-java)
   ▼
protobuf message classes + *Grpc service stubs
   │  ③ your code
   ▼
@Service beans extending the generated *ImplBase  ← Spring gRPC auto-registers them
```

Wirespec maps each construct to proto3 like so:

| Wirespec | proto3 |
|---|---|
| `type` | `message` |
| `enum` | `enum` (with an injected zero value) |
| `union` | `message` with a `oneof` |
| `rpc Name(params) -> Resp` | a `NameRequest` message wrapping the params + a `service Name { rpc Name (NameRequest) returns (Resp); }` |

A response that is a collection, scalar, or `Unit` is wrapped in a generated `NameResponse`
message, because a gRPC method must return a single message type.

The generated [`todo.proto`](#) therefore contains `message Todo / TodoInput / TodoList`, a
`CreateTodoRequest` / `ListTodosRequest`, and `service CreateTodo` / `service ListTodos`.

## Running

Prerequisites: a JDK (17+) and the Wirespec `0.0.0-SNAPSHOT` artifacts in your local Maven
repository (from the repo root: `./gradlew publishToMavenLocal`).

```shell
./mvnw verify          # generates proto, compiles stubs, runs the gRPC integration test
./mvnw spring-boot:run # starts the gRPC server on localhost:9090
```

[`TodoGrpcTest`](src/test/java/community/flock/wirespec/examples/grpc/TodoGrpcTest.java) loads the
Spring context (proving the gRPC auto-configuration and service beans wire up) and calls the
services over gRPC's in-process transport.

## Implementing a service

Each generated service is a normal gRPC `*ImplBase`; annotate the implementation with `@Service`
and Spring gRPC registers it automatically:

```java
@Service
public class CreateTodoService extends CreateTodoGrpc.CreateTodoImplBase {
    @Override
    public void createTodo(CreateTodoRequest request, StreamObserver<Todo> responseObserver) {
        ...
    }
}
```
