package community.flock.wirespec.integration.graphql.docs;

// The examples of the GraphQL client docs page, which shows the regions between the region comments.

// region create-client
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization;
import community.flock.wirespec.java.Wirespec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
// endregion create-client
import community.flock.wirespec.integration.graphql.java.generated.graphql.GraphQLClient;
import community.flock.wirespec.integration.graphql.java.generated.graphql.QueryTodoGraphQLClient;
import community.flock.wirespec.integration.graphql.java.generated.graphql.SearchResultResult;
import community.flock.wirespec.integration.graphql.java.generated.graphql.TodoResult;
import community.flock.wirespec.integration.graphql.java.generated.graphql.UserResult;
import community.flock.wirespec.integration.graphql.java.generated.model.ID;
import community.flock.wirespec.integration.graphql.java.generated.model.NewTodo;
import community.flock.wirespec.integration.graphql.java.generated.model.Priority;
import community.flock.wirespec.integration.graphql.java.generated.model.Todo;
import community.flock.wirespec.integration.graphql.java.generated.rpc.CompleteTodo;
import community.flock.wirespec.integration.graphql.java.generated.rpc.QueryTodo;
import java.util.List;

public final class JavaGraphQLExamples {

    private JavaGraphQLExamples() {
    }

    public static GraphQLClient createClient() {
        // region create-client
        var serialization = new WirespecSerialization(
            new ObjectMapper()
                .registerModule(new Jdk8Module())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

        var http = HttpClient.newHttpClient();
        Wirespec.Transportation transportation = request -> http
            .sendAsync(
                HttpRequest.newBuilder(URI.create("https://api.example.com/" + String.join("/", request.path())))
                    .method(request.method(), HttpRequest.BodyPublishers.ofByteArray(request.body().orElse(new byte[0])))
                    .header("Content-Type", "application/json")
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(response -> new Wirespec.RawResponse(response.statusCode(), response.headers().map(), Optional.of(response.body())));

        var client = new GraphQLClient(serialization, transportation);
        // endregion create-client
        return client;
    }

    public static QueryTodo.Service operationClient(Wirespec.Serialization serialization, Wirespec.Transportation transportation) {
        // region operation-client
        QueryTodo.Service queryTodo = new QueryTodoGraphQLClient(serialization, transportation);
        // endregion operation-client
        return queryTodo;
    }

    public static List<Object> calls(GraphQLClient client) {
        // region calls
        Optional<Todo> todo = client.queryTodo(new ID("1")).join();

        List<Todo> open = client.todos(Optional.of(false)).join();

        Todo added = client.addTodo(new NewTodo("Groceries", Optional.of(Priority.HIGH))).join();

        client.clearTodos().join();
        // endregion calls
        return List.of(todo, open, added);
    }

    public static String errors(GraphQLClient client) {
        // region errors
        var response = client.completeTodo(new ID("1")).join();
        String message;
        if (response instanceof CompleteTodo.Result result) {
            message = "Done: " + result.value().title();
        } else if (response instanceof CompleteTodo.Error error) {
            message = "Failed: " + error.value().message();
        } else {
            throw new IllegalStateException("Unknown response " + response);
        }
        // endregion errors
        return message;
    }

    public static List<Object> selection(GraphQLClient client) {
        // region selection
        Optional<TodoResult> todo = client.queryTodo(new ID("1"), t -> t
            .title()
            .owner(owner -> owner.name())).join();

        Optional<String> title = todo.flatMap(TodoResult::title);
        Optional<String> owner = todo.flatMap(TodoResult::owner).flatMap(UserResult::name);
        Optional<Boolean> done = todo.flatMap(TodoResult::done); // empty: not selected
        // endregion selection
        return List.of(title, owner, done);
    }

    public static List<Optional<String>> union(GraphQLClient client) {
        // region union
        List<SearchResultResult> results = client.search("milk", result -> result
            .onNote(note -> note.text())
            .onTag(tag -> tag.name())).join();

        List<Optional<String>> kinds = results.stream().map(SearchResultResult::__typename).toList(); // "Note" or "Tag"
        // endregion union
        return kinds;
    }
}
