package community.flock.wirespec.examples.maven.spring.integration.client;

import community.flock.wirespec.generated.examples.spring.endpoint.GetTodos;
import community.flock.wirespec.integration.spring.java.client.WirespecWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class TodoWebClient implements GetTodos.Handler {

  private final WirespecWebClient wirespecWebClient;

  @Autowired
  public TodoWebClient(WirespecWebClient wirespecWebClient) {
    this.wirespecWebClient = wirespecWebClient;
  }

  @Override
  public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request request) {
    return wirespecWebClient.send(request);
  }
}
