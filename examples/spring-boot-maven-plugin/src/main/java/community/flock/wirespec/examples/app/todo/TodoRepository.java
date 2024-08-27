package community.flock.wirespec.examples.app.todo;

import community.flock.wirespec.generated.java.Todo;
import community.flock.wirespec.generated.java.TodoId;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Repository
public class TodoRepository {

    private static final String UUID = "f20ad876-c6a8-48b8-9a23-71787c1ae34a";
    private final HashMap<String, Todo> todos = new HashMap<>(
            Map.of(
                    UUID,
                    new Todo(
                            new TodoId(UUID),
                            "Name",
                            true,
                            emptyList()
                    )
            )
    );

    public List<Todo> getAllTodos() {
        return todos.values().stream().toList();
    }

    public Todo getTodoById(TodoId id) {
        return todos.get(id.value());
    }

    public Todo saveTodo(Todo todo) {
        todos.put(todo.id().value(), todo);
        return todo;
    }

    public Todo deleteTodoById(TodoId id) {
        return todos.remove(id.value());
    }

}
