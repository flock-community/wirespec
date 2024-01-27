# Spring integration lib

This module offers Spring configuration which can bind Wirespec endpoints as request mappings.

## Install

```xml
<dependency>
    <groupId>community.flock.wirespec.integration</groupId>
    <artifactId>spring</artifactId>
    <version>{VERSION}</version>
</dependency>
```

## Usage

The generated Wirespec Endpoint needs to be implemented a Controller class. By annotating this class with `@WirespecController` all the endpoints are automatically added as request mappings.  

```java
@WirespecController
class TodoController implements GetTodosEndpoint {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<CreateTodoEndpoint.Response<?>> createTodo(CreateTodoEndpoint.Request<?> request) {
        var todoInput = switch (request) {
            case CreateTodoEndpoint.RequestApplicationJson req -> req.getContent().body();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        service.create(todo);
        var res = new CreateTodoEndpoint.Response200ApplicationJson(Map.of(), todo);
        return CompletableFuture.completedFuture(res);
    }
}
```

For a more extensive example go to [Spring integration example](examples/spring-boot-integration)
