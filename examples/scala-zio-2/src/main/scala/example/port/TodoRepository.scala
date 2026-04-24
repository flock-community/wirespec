package example.port

import community.flock.wirespec.generated.model.{TodoDto, TodoId}
import zio.*

trait TodoRepository:
  def findAll(): Task[List[TodoDto]]
  def save(todo: TodoDto): Task[TodoDto]
  def findById(id: TodoId): Task[Option[TodoDto]]
  def delete(id: TodoId): Task[Option[TodoDto]]
