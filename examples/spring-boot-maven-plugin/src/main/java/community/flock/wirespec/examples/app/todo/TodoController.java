package community.flock.wirespec.examples.app.todo;

import community.flock.wirespec.generated.java.Todo;
import community.flock.wirespec.generated.java.TodoId;
import community.flock.wirespec.generated.java.TodoInput;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/todos")
class TodoController {

    private final TodoRepository repository;

    TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Todo> getAllTodos() {
        return repository.getAllTodos();
    }

    @GetMapping("/{id}")
    public Todo getTodoById(@PathVariable String id) {
        return repository.getTodoById(new TodoId(id));
    }

    @PostMapping
    public Todo postTodo(@RequestBody TodoInput input) {
        TodoId todoId = new TodoId(UUID.randomUUID().toString());
        Todo todo = new Todo(todoId, input.name(), input.done());
        return repository.saveTodo(todo);
    }

    @DeleteMapping("/{id}")
    public Todo deleteById(@PathVariable String id) {
        return repository.deleteTodoById(new TodoId(id));
    }
}
