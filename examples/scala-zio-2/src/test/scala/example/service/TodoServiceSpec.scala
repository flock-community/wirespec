package example.service

import community.flock.wirespec.generated.model.*
import example.adapter.store.InMemoryTodoRepository
import example.port.TodoService
import zio.*
import zio.test.*

object TodoServiceSpec extends ZIOSpecDefault:

  private val potential = PotentialTodoDto(
    name     = "Buy milk",
    done     = false,
    testInt0 = IntRefinedNoBound(0L),
    testInt1 = IntRefinedLowerBound(0L),
    testInt2 = IntRefinedLowerAndUpper(3L),
    testNum0 = NumberRefinedNoBound(0.0),
    testNum1 = NumberRefinedUpperBound(1.0),
    testNum2 = NumberRefinedLowerAndUpper(3.5)
  )

  def spec = suite("TodoServiceLive")(
    test("getTodos returns all todos when done is None") {
      for
        svc   <- ZIO.service[TodoService]
        _     <- svc.createTodo(potential)
        todos <- svc.getTodos(None)
      yield assertTrue(todos.size == 1)
    },
    test("getTodos filters by done flag") {
      for
        svc  <- ZIO.service[TodoService]
        _    <- svc.createTodo(potential)
        _    <- svc.createTodo(potential.copy(done = true))
        done <- svc.getTodos(Some(true))
        open <- svc.getTodos(Some(false))
      yield assertTrue(done.size == 1) && assertTrue(open.size == 1)
    },
    test("createTodo returns saved todo with generated id") {
      for
        svc  <- ZIO.service[TodoService]
        todo <- svc.createTodo(potential)
      yield assertTrue(todo.name == "Buy milk") && assertTrue(todo.id.value.nonEmpty)
    },
    test("getTodoById returns Some when todo exists") {
      for
        svc    <- ZIO.service[TodoService]
        saved  <- svc.createTodo(potential)
        result <- svc.getTodoById(saved.id)
      yield assertTrue(result.contains(saved))
    },
    test("getTodoById returns None when todo does not exist") {
      for
        svc    <- ZIO.service[TodoService]
        result <- svc.getTodoById(TodoId("nonexistent"))
      yield assertTrue(result.isEmpty)
    },
    test("deleteTodoById returns Some and removes the todo") {
      for
        svc     <- ZIO.service[TodoService]
        saved   <- svc.createTodo(potential)
        deleted <- svc.deleteTodoById(saved.id)
        after   <- svc.getTodoById(saved.id)
      yield assertTrue(deleted.contains(saved)) && assertTrue(after.isEmpty)
    },
    test("deleteTodoById returns None when todo does not exist") {
      for
        svc    <- ZIO.service[TodoService]
        result <- svc.deleteTodoById(TodoId("nonexistent"))
      yield assertTrue(result.isEmpty)
    },
  ).provide(TodoServiceLive.layer, InMemoryTodoRepository.layer)
