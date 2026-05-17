package example.port

import community.flock.wirespec.generated.model.{PotentialTodoDto, TodoDto, TodoId}
import zio.*

trait TodoService:
  def getTodos(done: Option[Boolean]): Task[List[TodoDto]]
  def createTodo(potential: PotentialTodoDto): Task[TodoDto]
  def getTodoById(id: TodoId): Task[Option[TodoDto]]
  def deleteTodoById(id: TodoId): Task[Option[TodoDto]]
