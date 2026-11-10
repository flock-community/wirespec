package community.flock.wirespec.integration.graphql.docs

// The examples of the GraphQL client docs page, which shows the regions between the region comments.

// region create-client
import community.flock.wirespec.scala.Wirespec
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
// endregion create-client
import community.flock.wirespec.integration.graphql.scala.generated.GraphQLClient
import community.flock.wirespec.integration.graphql.scala.generated.graphql.{CompleteTodoGraphQL, QueryTodoGraphQL, SearchGraphQL}
import community.flock.wirespec.integration.graphql.scala.generated.model.{ID, NewTodo, Priority, Todo}
import community.flock.wirespec.integration.graphql.scala.generated.rpc.{CompleteTodo, QueryTodo}

object ScalaGraphQLExamples {

  def createClient(serialization: Wirespec.Serialization): GraphQLClient = {
    // region create-client
    val http = HttpClient.newHttpClient()
    val transportation: Wirespec.Transportation = request => {
      val response = http.send(
        HttpRequest.newBuilder(URI.create("https://api.example.com/" + request.path.mkString("/")))
          .method(request.method, HttpRequest.BodyPublishers.ofByteArray(request.body.getOrElse(Array.emptyByteArray)))
          .header("Content-Type", "application/json")
          .build(),
        HttpResponse.BodyHandlers.ofByteArray())
      Wirespec.RawResponse(response.statusCode, Map.empty, Some(response.body))
    }

    val client = GraphQLClient(serialization, transportation)
    // endregion create-client
    client
  }

  def operationClient(serialization: Wirespec.Serialization, transportation: Wirespec.Transportation): QueryTodo.Service = {
    // region operation-client
    val queryTodo: QueryTodo.Service = QueryTodoGraphQL.GraphQLClient(serialization, transportation)
    // endregion operation-client
    queryTodo
  }

  def calls(client: GraphQLClient): List[Any] = {
    // region calls
    val todo: Option[Todo] = client.queryTodo(ID("1"))

    val open: List[Todo] = client.todos(Some(false))

    val added: Todo = client.addTodo(NewTodo("Groceries", Some(Priority.HIGH)))

    client.clearTodos()
    // endregion calls
    List(todo, open, added)
  }

  def errors(client: GraphQLClient): String = {
    // region errors
    val message = client.completeTodo(ID("1")) match {
      case CompleteTodo.Result(todo) => s"Done: ${todo.title}"
      case CompleteTodo.Error(error) => s"Failed: ${error.message}"
    }
    // endregion errors
    message
  }

  def selection(client: GraphQLClient): List[Any] = {
    // region selection
    val todo: Option[QueryTodoGraphQL.TodoResult] = client.queryTodo(ID("1"), t => t
      .title()
      .owner(owner => owner.name()))

    val title: Option[String] = todo.flatMap(_.title)
    val owner: Option[String] = todo.flatMap(_.owner).flatMap(_.name)
    val done: Option[Boolean] = todo.flatMap(_.done) // None: not selected
    // endregion selection
    List(title, owner, done)
  }

  def union(client: GraphQLClient): List[Option[String]] = {
    // region union
    val results: List[SearchGraphQL.SearchResultResult] = client.search("milk", result => result
      .onNote(note => note.text())
      .onTag(tag => tag.name()))

    val kinds: List[Option[String]] = results.map(_.__typename) // "Note" or "Tag"
    // endregion union
    kinds
  }

  def requests(client: Wirespec.GraphQLClient): List[Any] = {
    // region requests
    val request: Wirespec.GraphQLRequest[Option[QueryTodoGraphQL.TodoResult]] =
      QueryTodoGraphQL.request(ID("1"), todo => todo.title())

    val todo: Option[QueryTodoGraphQL.TodoResult] = client.send(request)
    val response: CompleteTodo.Response = client.send(CompleteTodoGraphQL.request(ID("1")))
    // endregion requests
    List(todo, response)
  }
}
