package community.flock.wirespec.example.maven.custom.app.todo;

import community.flock.wirespec.generated.java.Todo;
import community.flock.wirespec.generated.java.TodoId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoService {

    private final TodoRepository repository;

    public TodoService(final TodoRepository repository) {
        this.repository = repository;
    }

    public List<Todo> getAllTodos() {
        return repository.getAllTodos();
    }

    public Todo getTodoById(final TodoId id) {
        return repository.getTodoById(id);
    }

    public Todo saveTodo(final Todo todo) {
        return repository.saveTodo(todo);
    }

    public Todo deleteTodoById(final TodoId id) {
        return repository.deleteTodoById(id);
    }
}
