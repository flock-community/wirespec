package example.service

import community.flock.wirespec.generated.model.{PotentialTodoDto, TodoDto, TodoId}
import example.port.{TodoRepository, TodoService}
import zio.*

import java.util.UUID

class TodoServiceLive(repo: TodoRepository) extends TodoService:

  override def getTodos(done: Option[Boolean]): Task[List[TodoDto]] =
    repo.findAll().map(_.filter(t => done.forall(_ == t.done)))

  override def createTodo(potential: PotentialTodoDto): Task[TodoDto] =
    for
      id  <- ZIO.succeed(TodoId(UUID.randomUUID().toString))
      dto  = TodoDto(
               id       = id,
               name     = potential.name,
               done     = potential.done,
               testInt0 = potential.testInt0,
               testInt1 = potential.testInt1,
               testInt2 = potential.testInt2,
               testNum0 = potential.testNum0,
               testNum1 = potential.testNum1,
               testNum2 = potential.testNum2,
             )
      saved <- repo.save(dto)
    yield saved

  override def getTodoById(id: TodoId): Task[Option[TodoDto]] =
    repo.findById(id)

  override def deleteTodoById(id: TodoId): Task[Option[TodoDto]] =
    repo.delete(id)

object TodoServiceLive:
  val layer: URLayer[TodoRepository, TodoService] =
    ZLayer.fromZIO(ZIO.service[TodoRepository].map(new TodoServiceLive(_)))
