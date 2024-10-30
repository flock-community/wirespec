# Example: How to use the spring integration lib

## Wirespec Maven Plugin Configuration
```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>0.0.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.integration</groupId>
            <artifactId>spring-jvm</artifactId>
            <version>0.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>java</id>
            <goals>
                <goal>custom</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources</output>
                <emitterClass>community.flock.wirespec.integration.spring.java.emit.SpringJavaEmitter</emitterClass>
                <shared>Java</shared>
                <extension>java</extension>
                <packageName>community.flock.wirespec.generated.examples.spring</packageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Find the [actual pom.xml](app/pom.xml) in the `app` module and the

## Usage

The custom emitter 'SpringJavaEmitter' will generate interfaces where the handle function is annotated with spring boot annotations. 

```java
public interface GetTodoByIdEndpoint extends Wirespec.Endpoint {
 ...
 interface Handler extends Wirespec.Handler {
     ...
     @org.springframework.web.bind.annotation.GetMapping("/todos/{id}")
     java.util.concurrent.CompletableFuture<Response<?>> getTodoById(Request request);
 }
}
```

These interface can be implemented as RestControllers. Spring will load the controllers and makes them available as request handlers

```java
@RestController
class TodoController implements GetTodosEndpoint.Handler{
    @Override
    public CompletableFuture<GetTodoByIdEndpoint.Response<?>> getTodoById(GetTodoByIdEndpoint.Request request) {
        ...
    }
}
```

By loading the 'WirespecConfiguration' on the application Wirespec request and response objects will be handled correctly.

```java
@SpringBootApplication
@Import(WirespecConfiguration.class)
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}

```