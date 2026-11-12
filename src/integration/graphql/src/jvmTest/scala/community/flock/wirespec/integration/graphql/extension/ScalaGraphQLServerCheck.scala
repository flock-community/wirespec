package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.integration.graphql.scala.generated.GraphQLServer
import community.flock.wirespec.integration.graphql.scala.generated.graphql.{AddTodoGraphQL, CompleteTodoGraphQL, InboxGraphQL, LatestGraphQL, MarkReadGraphQL, QueryTodoGraphQL, SearchGraphQL, TodosGraphQL}
import community.flock.wirespec.integration.graphql.scala.generated.model.{ID, NewTodo, Note, Notification, NotificationsMutation, NotificationsQuery, Priority, SearchResult, Tag, Todo, TodoError, User}
import community.flock.wirespec.integration.graphql.scala.generated.rpc.CompleteTodo
import community.flock.wirespec.scala.Wirespec

import scala.reflect.ClassTag

// Drives the generated Scala server; GraphQLServerTest calls these at runtime.
object ScalaGraphQLServerCheck {

  def answersAQuery(): String = handle(
    """query ($id: ID!) {
      |  first: todo(id: $id) { ...Summary }
      |  search(text: "milk") { __typename ... on Note { text } }
      |  failing: search(text: "boom") { __typename }
      |}
      |fragment Summary on Todo { name: title owner { name } done @skip(if: true) }
      |""".stripMargin,
    Map("id" -> "1"),
  )

  def answersAMutation(): String = handle("""mutation { clearTodos completeTodo(id: "404") { id } }""", Map.empty)

  def givesTheSelection(): String = {
    val selections = List.newBuilder[GraphQLServer.Selection]
    val serialization = Fake(GraphQLServer.Body("""{ first: todo(id: "1") { title ... on Todo { owner { name } } } }""", None, None))
    new GraphQLServer(serialization, new Handler(selections)).handle(Wirespec.RawRequest("POST", List("graphql"), Map.empty, Map.empty, Some(Array.emptyByteArray)))
    selections.result().toString
  }

  private def handle(query: String, variables: Map[String, Any]): String = {
    val serialization = Fake(GraphQLServer.Body(query, None, Some(variables)))
    new GraphQLServer(serialization, new Handler()).handle(Wirespec.RawRequest("POST", List("graphql"), Map.empty, Map.empty, Some(Array.emptyByteArray)))
    serialization.written.toString
  }

  private class Handler(selections: collection.mutable.Builder[GraphQLServer.Selection, List[GraphQLServer.Selection]] = List.newBuilder) extends GraphQLServer.Handler {
    def queryTodo(request: GraphQLServer.Request[QueryTodoGraphQL.GraphQLVariables]): Option[Todo] = {
      selections += request.selection
      Some(Todo(request.arguments.id, "Groceries", false, Some(Priority.HIGH), User("Ann")))
    }
    def todos(request: GraphQLServer.Request[TodosGraphQL.GraphQLVariables]): List[Todo] = Nil
    def search(request: GraphQLServer.Request[SearchGraphQL.GraphQLVariables]): List[SearchResult] =
      if (request.arguments.text == "boom") throw new IllegalStateException("Search is down") else List(Note(request.arguments.text), Tag("home"))
    def addTodo(request: GraphQLServer.Request[AddTodoGraphQL.GraphQLVariables]): Todo = Todo(ID("3"), request.arguments.input.title, false, request.arguments.input.priority, User("Ann"))
    def completeTodo(request: GraphQLServer.Request[CompleteTodoGraphQL.GraphQLVariables]): CompleteTodo.Response = CompleteTodo.Error(TodoError("Unknown todo " + request.arguments.id.value))
    def clearTodos(request: GraphQLServer.Request[Unit]): Unit = ()
    def notifications(request: GraphQLServer.Request[Unit]): NotificationsQuery = NotificationsQuery()
    def inbox(request: GraphQLServer.Request[InboxGraphQL.GraphQLVariables]): List[Notification] = Nil
    def inboxCount(request: GraphQLServer.Request[Unit]): Int = 3
    def latest(request: GraphQLServer.Request[LatestGraphQL.GraphQLVariables]): List[Notification] = Nil
    def mutateNotifications(request: GraphQLServer.Request[Unit]): NotificationsMutation = NotificationsMutation()
    def markRead(request: GraphQLServer.Request[MarkReadGraphQL.GraphQLVariables]): Unit = ()
  }

  // Reads the request it is given and the arguments the server wrote last, and keeps the response, so no JSON library is needed.
  private case class Fake(request: GraphQLServer.Body) extends Wirespec.Serialization {
    var written: Any = null

    def serializeBody[T](t: T, `type`: ClassTag[?]): Array[Byte] = {
      written = t
      Array.emptyByteArray
    }

    def deserializeBody[T](raw: Array[Byte], `type`: ClassTag[?]): T = {
      val arguments = Option(written).map(_.asInstanceOf[Map[String, Any]])
      val read: Any = `type`.runtimeClass match {
        case request if request == classOf[GraphQLServer.Body] => this.request
        case variables if variables == classOf[QueryTodoGraphQL.GraphQLVariables] => QueryTodoGraphQL.GraphQLVariables(ID(arguments.get("id").toString))
        case variables if variables == classOf[CompleteTodoGraphQL.GraphQLVariables] => CompleteTodoGraphQL.GraphQLVariables(ID(arguments.get("id").toString))
        case variables if variables == classOf[SearchGraphQL.GraphQLVariables] => SearchGraphQL.GraphQLVariables(arguments.get("text").toString)
      }
      read.asInstanceOf[T]
    }

    def serializePath[T](t: T, `type`: ClassTag[?]): String = ???

    def deserializePath[T](raw: String, `type`: ClassTag[?]): T = ???

    def serializeParam[T](value: T, `type`: ClassTag[?]): List[String] = ???

    def deserializeParam[T](values: List[String], `type`: ClassTag[?]): T = ???
  }
}
