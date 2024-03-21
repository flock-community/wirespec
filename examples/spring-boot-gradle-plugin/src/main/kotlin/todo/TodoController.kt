package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoIdNotValidException
import community.flock.wirespec.examples.app.todo.TodoConsumer.consume
import community.flock.wirespec.examples.app.todo.TodoProducer.produce
import community.flock.wirespec.generated.kotlin.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.TodoDto
import community.flock.wirespec.generated.kotlin.validate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import community.flock.wirespec.generated.kotlin.TodoId as PotentialTodoId

@RestController
@RequestMapping("/todos")
class TodoController(private val service: TodoService) {

    @GetMapping
    fun getAllTodos(): List<TodoDto> = service.getAllTodos()
        .map { it.produce() }

    @GetMapping("/{id}")
    fun getTodoById(@PathVariable id: String): TodoDto = PotentialTodoId(id)
        .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
        .let { Todo.Id(it.value) }
        .let(service::getTodoById)
        .produce()

    @PostMapping
    fun postTodo(@RequestBody input: PotentialTodoDto): TodoDto = input.consume().let(service::saveTodo).produce()

    @DeleteMapping("/{id}")
    fun deleteTodoById(@PathVariable id: String): TodoDto = PotentialTodoId(id)
        .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
        .let { Todo.Id(it.value) }
        .let(service::deleteTodoById)
        .produce()
}
