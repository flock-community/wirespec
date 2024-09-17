package community.flock.wirespec.examples.app.todo;

import community.flock.wirespec.examples.app.exception.NotFound;
import community.flock.wirespec.generated.java.Error;
import community.flock.wirespec.generated.java.Todo;
import community.flock.wirespec.generated.java.TodoId;
import community.flock.wirespec.generated.java.TodoInput;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/todos")
class TodoController {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Todo> getAllTodos() {
        return service.getAllTodos();
    }

    @GetMapping("/{id}")
    public Todo getTodoById(@PathVariable final String id) {
        final var todoId = new TodoId(id);
        final var maybeTodo = service.getTodoById(todoId);
        if (maybeTodo == null) {
            final var errorMap = Map.of("404", "Not Found");
            final var error = new Error(errorMap).toString();
            throw new NotFound(error);
        }
        return maybeTodo;
    }

    @PostMapping
    public Todo postTodo(@RequestBody final TodoInput input) {
        final var todoId = new TodoId(UUID.randomUUID().toString());
        final var todo = new Todo(todoId, input.name(), input.done(), emptyList());
        return service.saveTodo(todo);
    }

    @DeleteMapping("/{id}")
    public Todo deleteById(@PathVariable final String id) {
        return service.deleteTodoById(new TodoId(id));
    }
}
