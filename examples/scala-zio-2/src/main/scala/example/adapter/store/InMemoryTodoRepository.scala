package example.adapter.store

import community.flock.wirespec.generated.model.{TodoDto, TodoId}
import example.port.TodoRepository
import zio.*

class InMemoryTodoRepository(store: Ref[Map[TodoId, TodoDto]]) extends TodoRepository:

  override def findAll(): Task[List[TodoDto]] =
    store.get.map(_.values.toList)

  override def save(todo: TodoDto): Task[TodoDto] =
    store.update(_ + (todo.id -> todo)).as(todo)

  override def findById(id: TodoId): Task[Option[TodoDto]] =
    store.get.map(_.get(id))

  override def delete(id: TodoId): Task[Option[TodoDto]] =
    store.modify { todos =>
      todos.get(id) match
        case Some(todo) => (Some(todo), todos - id)
        case None       => (None, todos)
    }

object InMemoryTodoRepository:
  val layer: ULayer[TodoRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[TodoId, TodoDto]).map(new InMemoryTodoRepository(_)))
