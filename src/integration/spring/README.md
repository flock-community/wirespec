# Spring integration lib

This module offers Spring configuration which can bind Wirespec endpoints as request mappings.

## Install

```xml
<dependency>
    <groupId>community.flock.wirespec.integration</groupId>
    <artifactId>spring-jvm</artifactId>
    <version>{VERSION}</version>
</dependency>
```

## Usage
Use the custom Java or Kotlin spring emitters to generate the endpoint classes
- [SpringJavaEmitter.kt](src%2FjvmMain%2Fkotlin%2Fcommunity%2Fflock%2Fwirespec%2Fintegration%2Fspring%2Femit%2FSpringJavaEmitter.kt)
- [SpringJavaEmitter.kt](src%2FjvmMain%2Fkotlin%2Fcommunity%2Fflock%2Fwirespec%2Fintegration%2Fspring%2Femit%2FSpringJavaEmitter.kt)

Load the wirspec spring configuration 
- [WirespecConfiguration.kt](src%2FjvmMain%2Fkotlin%2Fcommunity%2Fflock%2Fwirespec%2Fintegration%2Fspring%2Fconfiguration%2FWirespecConfiguration.kt)

```java
@SpringBootApplication
@Import(WirespecConfiguration.class)
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}

```

```java
@RestController
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
