package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.integration.graphql.scala.generated.graphql.{CompleteTodoClient, GraphQLClient, QueryTodoClient, TodoResult, UserResult}
import community.flock.wirespec.integration.graphql.scala.generated.model.{ID, Priority, Todo, TodoError, User}
import community.flock.wirespec.integration.graphql.scala.generated.rpc.{CompleteTodo, QueryTodo}
import community.flock.wirespec.scala.Wirespec

import scala.reflect.ClassTag

// Drives the generated Scala client; GraphQLClientExtensionTest calls these at runtime.
object ScalaGraphQLCheck {

  def queriesTheFullResult(): String = {
    val todo = Todo(ID("1"), "Groceries", false, Some(Priority.HIGH), User("Ann"))
    val serialization = Recording(QueryTodoClient.GraphQLResponse(Some(QueryTodoClient.GraphQLData(Some(todo))), None))

    val result = GraphQLClient(serialization, transportation).queryTodo(ID("1"))

    check(result == Some(todo), s"unexpected $result")
    serialization.query
  }

  def readsTheSelectedFields(): String = {
    val expected = TodoResult(None, Some("Groceries"), None, None, Some(UserResult(Some("Ann"))))
    val serialization = Recording(QueryTodoClient.GraphQLSelectedResponse(Some(QueryTodoClient.GraphQLSelectedData(Some(expected))), None))

    val result = GraphQLClient(serialization, transportation).queryTodo(ID("1"), todo => todo.title().owner(owner => owner.name()))

    check(result == Some(expected), s"unexpected $result")
    serialization.query
  }

  def returnsTheTypedError(): Unit = {
    val serialization = Recording(CompleteTodoClient.GraphQLResponse(None, Some(List(TodoError("Unknown todo")))))

    val result = GraphQLClient(serialization, transportation).completeTodo(ID("3"))

    check(result == CompleteTodo.Error(TodoError("Unknown todo")), s"unexpected $result")
  }

  def buildsAQuery(): String = QueryTodo.graphQL.request(ID("1"), todo => todo.id().owner(owner => owner.name())).query

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
