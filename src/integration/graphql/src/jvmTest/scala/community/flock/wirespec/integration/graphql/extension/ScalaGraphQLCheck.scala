package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.integration.graphql.docs.ScalaGraphQLExamples
import community.flock.wirespec.integration.graphql.scala.generated.GraphQLClient
import community.flock.wirespec.integration.graphql.scala.generated.graphql.{CompleteTodoGraphQL, InboxCountGraphQL, QueryTodoGraphQL, SearchGraphQL}
import community.flock.wirespec.integration.graphql.scala.generated.model.{ID, Note, Priority, SearchResult, Tag, Todo, TodoError, User}
import community.flock.wirespec.integration.graphql.scala.generated.rpc.CompleteTodo
import community.flock.wirespec.scala.Wirespec

import scala.reflect.ClassTag

// Drives the generated Scala client; GraphQLClientExtensionTest calls these at runtime.
object ScalaGraphQLCheck {

  def queriesTheFullResult(): String = {
    val todo = Todo(ID("1"), "Groceries", false, Some(Priority.HIGH), User("Ann"))
    val serialization = Recording(QueryTodoGraphQL.GraphQLResponse(Some(QueryTodoGraphQL.GraphQLData(Some(todo))), None))

    val result = GraphQLClient(serialization, transportation).queryTodo(ID("1"))

    check(result == Some(todo), s"unexpected $result")
    serialization.query
  }

  def readsTheSelectedFields(): String = {
    val expected = QueryTodoGraphQL.TodoResult(None, Some("Groceries"), None, None, Some(QueryTodoGraphQL.UserResult(Some("Ann"))))
    val serialization = Recording(QueryTodoGraphQL.GraphQLSelectedResponse(Some(QueryTodoGraphQL.GraphQLSelectedData(Some(expected))), None))

    val result = GraphQLClient(serialization, transportation).queryTodo(ID("1"), todo => todo.title().owner(owner => owner.name()))

    check(result == Some(expected), s"unexpected $result")
    serialization.query
  }

  def returnsTheTypedError(): Unit = {
    val serialization = Recording(CompleteTodoGraphQL.GraphQLResponse(None, Some(List(TodoError("Unknown todo")))))

    val result = GraphQLClient(serialization, transportation).completeTodo(ID("3"))

    check(result == CompleteTodo.Error(TodoError("Unknown todo")), s"unexpected $result")
  }

  def readsAUnion(): String = {
    val results: List[SearchResult] = List(Note("Milk"), Tag("home"))
    val serialization = Recording(SearchGraphQL.GraphQLResponse(Some(SearchGraphQL.GraphQLData(Some(results))), None))

    val result = GraphQLClient(serialization, transportation).search("milk")

    check(result == results, s"unexpected $result")
    serialization.query
  }

  def readsAUnionSelection(): String = {
    val expected = List(SearchGraphQL.SearchResultResult(Some("Note"), Some("Milk"), None))
    val serialization = Recording(SearchGraphQL.GraphQLSelectedResponse(Some(SearchGraphQL.GraphQLSelectedData(Some(expected))), None))

    val result = GraphQLClient(serialization, transportation).search("milk", result => result.onNote(note => note.text()).onTag(tag => tag.name()))

    check(result == expected, s"unexpected $result")
    serialization.query
  }

  def readsFromANamespace(): String = {
    val data = InboxCountGraphQL.GraphQLData(Some(InboxCountGraphQL.GraphQLDataNotifications(Some(3))))
    val serialization = Recording(InboxCountGraphQL.GraphQLResponse(Some(data), None))

    val result = GraphQLClient(serialization, transportation).inboxCount()

    check(result == 3, s"unexpected $result")
    serialization.query
  }

  def sendsThroughAClientOfYourOwn(): String = {
    val expected = QueryTodoGraphQL.TodoResult(None, Some("Groceries"), None, None, None)
    val serialization = Recording(QueryTodoGraphQL.GraphQLSelectedResponse(Some(QueryTodoGraphQL.GraphQLSelectedData(Some(expected))), None))

    val result = ScalaGraphQLExamples.AbstractClient(serialization, transportation)
      .send(QueryTodoGraphQL.SelectedApi, QueryTodoGraphQL.request(ID("1"), todo => todo.title()))

    check(result == Some(expected), s"unexpected $result")
    serialization.query
  }

  private val transportation: Wirespec.Transportation = _ => Wirespec.RawResponse(200, Map.empty, Some(Array.emptyByteArray))

  // Keeps the request it is given and answers with a response that is already read, so no JSON library is needed.
  private case class Recording(response: Any) extends Wirespec.Serialization {
    private var sent: Any = null

    def query: String = sent.asInstanceOf[Product].productElement(0).asInstanceOf[String]

    def serializeBody[T](t: T, `type`: ClassTag[?]): Array[Byte] = {
      sent = t
      Array.emptyByteArray
    }

    def deserializeBody[T](raw: Array[Byte], `type`: ClassTag[?]): T = response.asInstanceOf[T]

    def serializePath[T](t: T, `type`: ClassTag[?]): String = ???

    def deserializePath[T](raw: String, `type`: ClassTag[?]): T = ???

    def serializeParam[T](value: T, `type`: ClassTag[?]): List[String] = ???

    def deserializeParam[T](values: List[String], `type`: ClassTag[?]): T = ???
  }

  private def check(condition: Boolean, message: => String): Unit =
    if (!condition) throw new AssertionError(message)
}
