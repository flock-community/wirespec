package community.flock.wirespec.examples.spring_boot_integration;

import community.flock.wirespec.generated.*;
import community.flock.wirespec.integration.spring.annotations.WirespecController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@WirespecController
class PetstoreController implements GetTodos, GetTodoById, CreateTodo, UpdateTodo, DeleteTodo {

    private List<Todo> store = List.of(
            new Todo(UUID.randomUUID().toString(), "First todo", false),
            new Todo(UUID.randomUUID().toString(), "Second todo", false),
            new Todo(UUID.randomUUID().toString(), "Third todo", false)
    );

    @Override
    public CompletableFuture<CreateTodo.Response<?>> createTodo(CreateTodo.Request<?> request) {
        var todoInput = switch (request){
            case CreateTodo.RequestApplicationJson req -> req.getContent().body();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        store.add(todo);
        var res = new CreateTodo.Response200ApplicationJson(Map.of(), todo);
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<DeleteTodo.Response<?>> deleteTodo(DeleteTodo.Request<?> request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodoById.Response<?>> getTodoById(GetTodoById.Request<?> request) {
        var id = switch (request){
            case GetTodoById.RequestVoid req -> req.getPath();
        };
        System.out.println(id);
        var res = new GetTodoById.Response200ApplicationJson(Map.of(), store.get(0));
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<UpdateTodo.Response<?>> updateTodo(UpdateTodo.Request<?> request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request<?> request) {
        var res = new GetTodos.Response200ApplicationJson(Map.of(), store);
        return CompletableFuture.completedFuture(res);
    }
}
