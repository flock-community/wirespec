package community.flock.wirespec.examples.spring_boot_integration;


import community.flock.wirespec.generated.*;
import community.flock.wirespec.integration.spring.annotations.WirespecController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@WirespecController
class TodoController implements GetTodosEndpoint, GetTodoByIdEndpoint, CreateTodoEndpoint, UpdateTodoEndpoint, DeleteTodoEndpoint {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<CreateTodoEndpoint.Response<?>> createTodo(CreateTodoEndpoint.Request<?> request) {
        var todoInput = switch (request){
            case CreateTodoEndpoint.RequestApplicationJson req -> req.getContent().body();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        service.create(todo);
        var res = new CreateTodoEndpoint.Response200ApplicationJson(Map.of(), todo);
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<DeleteTodoEndpoint.Response<?>> deleteTodo(DeleteTodoEndpoint.Request<?> request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodoByIdEndpoint.Response<?>> getTodoById(GetTodoByIdEndpoint.Request<?> request) {
        var id = switch (request){
            case GetTodoByIdEndpoint.RequestVoid req -> req.getPath();
        };
        System.out.println(id);
        var res = new GetTodoByIdEndpoint.Response200ApplicationJson(Map.of(), service.store.get(0));
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<UpdateTodoEndpoint.Response<?>> updateTodo(UpdateTodoEndpoint.Request<?> request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodosEndpoint.Response<?>> getTodos(GetTodosEndpoint.Request<?> request) {
        var res = new GetTodosEndpoint.Response200ApplicationJson(Map.of(), service.store);
        return CompletableFuture.completedFuture(res);
    }
}
