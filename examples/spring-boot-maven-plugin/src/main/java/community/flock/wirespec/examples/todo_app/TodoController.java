package community.flock.wirespec.examples.todo_app;

import community.flock.wirespec.generated.Todo;
import community.flock.wirespec.generated.TodoId;
import community.flock.wirespec.generated.TodoInput;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/todos")
class TodoController {

    private List<Todo> todos = new ArrayList();

    @GetMapping("/")
    public List<Todo> list(){
        return todos;
    }

    @GetMapping("/:id")
    public Todo get(@PathVariable String id) {
       return todos.stream()
               .filter (todo -> todo.getId().getValue().equals(id))
               .findAny()
               .orElse(null);
    }

    @PostMapping("/")
    public void post(TodoInput input) {
        TodoId todoId = new TodoId(UUID.randomUUID().toString());
        Todo todo = new Todo(todoId, input.getName(), input.getDone());
        todos.add(todo);
    }
}
