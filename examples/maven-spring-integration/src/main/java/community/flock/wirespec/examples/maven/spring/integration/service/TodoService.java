package community.flock.wirespec.examples.maven.spring.integration.service;

import community.flock.wirespec.generated.examples.spring.model.Todo;
import community.flock.wirespec.generated.examples.spring.model.TodoId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

@Service
public class TodoService {

    public List<Todo> store = new ArrayList<>();
    public Map<String, Object> files = new HashMap<>();

    public TodoService(){
        store.add(new Todo(new TodoId(UUID.randomUUID().toString()), "First todo", false));
        store.add(new Todo(new TodoId(UUID.randomUUID().toString()), "Second todo", false));
        store.add(new Todo(new TodoId(UUID.randomUUID().toString()), "Third todo", false));
    }

    public CompletableFuture<Todo> create(Todo todo) {
        store.add(todo);
        return CompletableFuture.completedFuture(todo);
    }

    public CompletableFuture<Todo> update(String id, Todo todo) {
        store = store.stream()
                .map(it -> {
                    if (it.id().equals(id)) {
                        return todo;
                    } else {
                        return it;
                    }
                })
                .collect(toList());
        return CompletableFuture.completedFuture(todo);
    }

    public CompletableFuture<Todo> delete(String id) {
        return store.stream()
                .filter(todo -> todo.id().equals(id))
                .findFirst()
                .map(todo -> {
                    store.remove(todo);
                    return CompletableFuture.completedFuture(todo);
                })
                .orElseThrow();
    }

    public void uploadFile(String name, Object file){
        files.put(name, file);
    }
}
