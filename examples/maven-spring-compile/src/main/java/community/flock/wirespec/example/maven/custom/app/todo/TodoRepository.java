package community.flock.wirespec.example.maven.custom.app.todo;

import community.flock.wirespec.generated.java.Todo;
import community.flock.wirespec.generated.java.TodoId;

import java.util.List;

public interface TodoRepository {
    List<Todo> getAllTodos();

    Todo getTodoById(final TodoId id);

    Todo saveTodo(final Todo todo);

    Todo deleteTodoById(final TodoId id);
}
