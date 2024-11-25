# Wirespec Spring Integration

A Spring integration library that enables:
- Spring RestController endpoint generation
- Spring WebClient integration

## Installation

Add the Maven plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>{VERSION}</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <id>java</id>
            <goals>
                <goal>custom</goal>
            </goals>
            <configuration>
                <input>${maven.multiModuleProjectDirectory}/wirespec</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>generated.community.flock.wirespec.api</packageName>
                <emitterClass>community.flock.wirespec.integration.spring.kotlin.emit.SpringJavaEmitter</emitterClass>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.integration</groupId>
            <artifactId>spring-jvm</artifactId>
            <version>{VERSION}</version>
        </dependency>
    </dependencies>
</plugin>
```

Choose your emitter:
- [SpringJavaEmitter.kt](src/jvmMain/kotlin/community/flock/wirespec/integration/spring/java/emit/SpringJavaEmitter.kt)
- [SpringKotlinEmitter.kt](src/jvmMain/kotlin/community/flock/wirespec/integration/spring/kotlin/emit/SpringKotlinEmitter.kt)

## RestController Integration

1. Enable the controller in your Spring Boot application:

```java
@SpringBootApplication
@EnableWirespecController
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

2. Implement the generated endpoint interface:

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
        return CompletableFuture.completedFuture(
            new CreateTodoEndpoint.Response200ApplicationJson(Map.of(), todo)
        );
    }
}
```

## WebClient Integration

1. Enable the client in your Spring Boot application:

```java
@SpringBootApplication
@EnableWirespecClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

2. Configure the WebClient:

```java
@Configuration
public class WirespecClientConfig {

  @Bean("wirespecSpringWebClient")
  public WebClient webClient(
      WebClient.Builder builder
  ) {
    return builder.baseUrl("http://localhost:8080").build();
  }
}

```

3. Use the WirespecWebClient in your API client:

```java
@Component
public class TodoWebClient implements GetTodosEndpoint.Handler {

  private final WirespecWebClient wirespecWebClient;

  @Autowired
  public TodoWebClient(WirespecWebClient wirespecWebClient) {
    this.wirespecWebClient = wirespecWebClient;
  }

  @Override
  public CompletableFuture<GetTodosEndpoint.Response<?>> getTodos(GetTodosEndpoint.Request request) {
    return wirespecWebClient.send(request);
  }
}
```

## Both Controller and WebClient support

Use the following annotation to enable both Wirespec Controller and WebClient support:

```java
@SpringBootApplication
@EnableWirespec
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
```

For more examples, see the:

- [WebClientIntegrationTest](src/jvmTest/kotlin/community/flock/wirespec/integration/spring/kotlin/it/client/WebClientIntegrationTest.kt)
- [RestControllerIntegrationTest](src/jvmTest/kotlin/community/flock/wirespec/integration/spring/kotlin/it/controller/RestControllerIntegrationTest.kt)
