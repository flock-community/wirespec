package community.flock.wirespec.examples.grpc.service;

import community.flock.wirespec.examples.grpc.proto.Todo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Trivial in-memory store of the protobuf {@link Todo} messages generated from todo.ws. */
@Component
public class TodoStore {

    private final Map<String, Todo> store = new ConcurrentHashMap<>();

    public Todo save(Todo todo) {
        store.put(todo.getId(), todo);
        return todo;
    }

    public List<Todo> findAll() {
        return List.copyOf(store.values());
    }
}
