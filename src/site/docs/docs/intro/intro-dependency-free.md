---
sidebar_position: 2
---

# Dependency free

The core concept of Wirespec is that generated code is pure functional code which does not rely on external dependencies.

To achieve this, two concepts are introduced: **Serialization** and **Transportation**. These are always implemented outside of the generated code.

### Serialization

Serialization is the process of converting an object to a serialized format (like JSON or XML) and back to an object. Wirespec defines the interfaces for serialization but does not provide the implementation in the generated code.

Every language has specific libraries for this. For example:
- **Java**: Jackson, GSON, or Kotlinx Serialization.
- **JavaScript**: Built-in `JSON.parse()` and `JSON.stringify()`.

By keeping serialization outside the generated code, Wirespec remains agnostic to the serialization library you choose to use in your project.

### Transportation

Transportation is the process of sending or receiving raw requests and responses over the wire (e.g., via HTTP, WebSockets, or a message queue). 

Wirespec is agnostic about the implementation used for transportation. Whether you use:
- **Java**: `HttpClient`, `OkHttp`, or `Spring WebClient`.
- **Kotlin**: `Ktor` or `Retrofit`.
- **JavaScript**: `fetch` or `axios`.

The generated code only defines what needs to be transported and how it should look, while the actual delivery is handled by the transportation layer you provide.

---

## How it Works

To bridge the gap between pure functional code and real-world side effects, Wirespec uses **Emitters** to generate code and **Integration Packages** to provide the glue.

### Emitters

Emitters are responsible for generating the language-specific code from Wirespec specifications. They ensure that the generated code follows the "dependency-free" principle by:

1.  **Defining Shared Interfaces**: Each emitter (e.g., Kotlin, Java, TypeScript) includes a set of shared interfaces (often within a `Wirespec` object or namespace). These interfaces define the contract for `Request`, `Response`, `Serialization`, and `Transportation`.
2.  **Generating Data Models**: Emitters generate simple data classes or records for the types defined in the specification.
3.  **Generating Edges**: For each endpoint, the emitter generates "Edge" classes (e.g., `ClientEdge`, `ServerEdge`). These classes contain the logic to:
    -   Transform high-level request objects into `RawRequest` (serialization).
    -   Transform `RawResponse` back into high-level response objects (deserialization).

This ensures that the generated logic for an endpoint is completely contained and only depends on the shared interfaces provided by Wirespec.

### Integration Packages

Integration packages are the implementation of the shared interfaces. They provide the "glue" that connects the generated dependency-free code to specific libraries and frameworks.

1.  **Serialization Implementations**: Packages like `wirespec-jackson` or `wirespec-kotlinx-serialization` implement the `Wirespec.Serialization` interface, allowing you to use your preferred library to handle the actual byte-to-object conversion.
2.  **Transportation Implementations**: Packages like `wirespec-spring` or `wirespec-ktor` provide the implementation for sending `RawRequest` and receiving `RawResponse`.

For example, the `WirespecWebClient` in the Spring integration package takes a generated `Request`, uses a `Serialization` implementation to convert it to a `RawRequest`, and then uses Spring's `WebClient` to perform the actual HTTP call.

By separating the **Definition** (generated code) from the **Implementation** (integration packages), Wirespec allows you to swap out frameworks or libraries without changing your interface logic.
