package community.flock.wirespec.examples.maven.spring.integration.client;

import community.flock.wirespec.generated.examples.spring.model.Todo;
import java.util.List;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * A plain Spring Framework 7 declarative HTTP interface for the Todo API.
 *
 * <p>It exists so the {@code "todo"} HTTP service group is actually registered (via {@link
 * WirespecClientConfig}'s {@code @ImportHttpServices}). Registering the group is what activates the
 * {@code spring.http.client.service.group.todo.*} properties and the {@code
 * RestClientHttpServiceGroupConfigurer} bean. The Wirespec-generated client and this declarative
 * client therefore share the exact same group configuration (base url, default headers, timeouts).
 */
@HttpExchange
public interface TodoServiceClient {

    @GetExchange("/todos")
    List<Todo> getTodos();
}
