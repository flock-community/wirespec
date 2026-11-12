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
// region abstract-client
import java.util.concurrent.CompletableFuture;
// endregion abstract-client
import community.flock.wirespec.integration.graphql.java.generated.GraphQLClient;
import community.flock.wirespec.integration.graphql.java.generated.graphql.GraphQLMutation;
import community.flock.wirespec.integration.graphql.java.generated.graphql.GraphQLQuery;
import community.flock.wirespec.integration.graphql.java.generated.graphql.QueryTodoGraphQL;
import community.flock.wirespec.integration.graphql.java.generated.graphql.SearchGraphQL;
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
        QueryTodo.Service queryTodo = new QueryTodoGraphQL.GraphQLClient(serialization, transportation);
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
        Optional<QueryTodoGraphQL.TodoResult> todo = client.queryTodo(new ID("1"), t -> t
            .title()
            .owner(owner -> owner.name())).join();

        Optional<String> title = todo.flatMap(QueryTodoGraphQL.TodoResult::title);
        Optional<String> owner = todo.flatMap(QueryTodoGraphQL.TodoResult::owner).flatMap(QueryTodoGraphQL.UserResult::name);
        Optional<Boolean> done = todo.flatMap(QueryTodoGraphQL.TodoResult::done); // empty: not selected
        // endregion selection
        return List.of(title, owner, done);
    }

    public static List<Optional<String>> union(GraphQLClient client) {
        // region union
        List<SearchGraphQL.SearchResultResult> results = client.search("milk", result -> result
            .onNote(note -> note.text())
            .onTag(tag -> tag.name())).join();

        List<Optional<String>> kinds = results.stream().map(SearchGraphQL.SearchResultResult::__typename).toList(); // "Note" or "Tag"
        // endregion union
        return kinds;
    }

    public static List<Object> several(GraphQLClient client) {
        // region several
        GraphQLQuery.Result result = client.query(query -> query
            .queryTodo(new ID("1"), todo -> todo.title())
            .todos(Optional.of(false), todo -> todo.id())).join();

        Optional<String> title = result.queryTodo().flatMap(GraphQLQuery.TodoResult::title);
        Optional<List<Optional<ID>>> open = result.todos().map(todos -> todos.stream().map(GraphQLQuery.TodoResult::id).toList());
        Optional<List<GraphQLQuery.SearchResultResult>> found = result.search(); // empty: not selected
        // endregion several
        return List.of(title, open, found);
    }

    public static List<Object> severalMutations(GraphQLClient client) {
        // region several-mutations
        GraphQLMutation.Result result = client.mutation(mutation -> mutation
            .addTodo(new NewTodo("Groceries", Optional.empty()), todo -> todo.id())
            .clearTodos()).join();

        Optional<ID> added = result.addTodo().flatMap(GraphQLMutation.TodoResult::id);
        Optional<Boolean> cleared = result.clearTodos();
        // endregion several-mutations
        return List.of(added, cleared);
    }
    // region abstract-client

    public record AbstractClient(Wirespec.Serialization serialization, Wirespec.Transportation transportation) {
        public <Req extends Wirespec.GraphQLRequest, Res extends Wirespec.GraphQLResponse<?, ?>> CompletableFuture<Res> send(Wirespec.GraphQLClient<Req, Res> api, Req request) {
            var edge = api.getClient(serialization);
            return transportation.transport(edge.to(request)).thenApply(edge::from);
        }
    }
    // endregion abstract-client

    public static List<Object> abstractCalls(AbstractClient client) {
        // region abstract-calls
        QueryTodoGraphQL.Response response = client.send(new QueryTodoGraphQL.Api(), new QueryTodoGraphQL().request(new ID("1"))).join();
        Optional<Todo> todo = response.data();
        Optional<List<QueryTodoGraphQL.GraphQLError>> errors = response.errors();

        Optional<QueryTodoGraphQL.TodoResult> selected = client
            .send(new QueryTodoGraphQL.SelectedApi(), new QueryTodoGraphQL().request(new ID("1"), t -> t.title()))
            .join()
            .data();
        // endregion abstract-calls
        return List.of(todo, errors, selected);
    }

    public static GraphQLQuery.Response abstractSeveral(AbstractClient client) {
        // region abstract-several
        GraphQLQuery.Response response = client.send(new GraphQLQuery.Api(), new GraphQLQuery().request(query -> query
            .queryTodo(new ID("1"), todo -> todo.title())
            .todos(Optional.of(false), todo -> todo.id()))).join();
        // endregion abstract-several
        return response;
    }
}
