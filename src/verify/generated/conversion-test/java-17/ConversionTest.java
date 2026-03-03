import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.generated.endpoint.GetTodos;
import community.flock.wirespec.generated.model.TodoDto;
public class ConversionTest {
  public static void main(String[] args) {
  Wirespec.Serialization serialization = new Wirespec.Serialization() {
                      private final java.util.Map<String, Object> store = new java.util.HashMap<>();
                      private String randomKey() { return java.util.UUID.randomUUID().toString(); }
                      @Override public <T> byte[] serializeBody(T t, java.lang.reflect.Type type) { String key = randomKey(); store.put(key, t); return key.getBytes(); }
                      @Override public <T> T deserializeBody(byte[] raw, java.lang.reflect.Type type) { return (T) store.get(new String(raw)); }
                      @Override public <T> String serializePath(T t, java.lang.reflect.Type type) { return t.toString(); }
                      @Override public <T> T deserializePath(String raw, java.lang.reflect.Type type) { return (T) raw; }
                      @Override public <T> java.util.List<String> serializeParam(T value, java.lang.reflect.Type type) { return java.util.List.of(value.toString()); }
                      @Override public <T> T deserializeParam(java.util.List<String> values, java.lang.reflect.Type type) { return (T) values.get(0); }
                  };
  final var request = new GetTodos.Request();
  final var rawRequest = GetTodos.toRawRequest(serialization, request);
  assert rawRequest.method().equals("GET") : "Method should be GET";
  assert (rawRequest.path().equals(java.util.List.of("todos"))) : "Path should be [todos]";
  final var fromRaw = GetTodos.fromRawRequest(serialization, rawRequest);
  assert fromRaw != null : "fromRawRequest should return non-null";
  final var response200 = new GetTodos.Response200(java.util.List.of(new TodoDto("test")));
  final var rawResponse = GetTodos.toRawResponse(serialization, response200);
  assert rawResponse.statusCode() == 200 : "Status should be 200";
  assert rawResponse.body().isPresent() : "Body should be present";
  final var fromRawResp = GetTodos.fromRawResponse(serialization, rawResponse);
  assert fromRawResp instanceof GetTodos.Response200 : "Should be Response200";
  assert fromRawResp.status() == 200 : "Status should be 200";
  }
}
