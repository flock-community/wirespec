package community.flock.wirespec.examples.spring_boot_integration;

import community.flock.wirespec.generated.*;
import community.flock.wirespec.integration.spring.annotations.WirespecController;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@WirespecController
class TodoController implements GetTodos, GetTodoById, CreateTodo, UpdateTodo, DeleteTodo {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

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
        service.create(todo);
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
        var res = new GetTodoById.Response200ApplicationJson(Map.of(), service.store.get(0));
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<UpdateTodo.Response<?>> updateTodo(UpdateTodo.Request<?> request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request<?> request) {
        var res = new GetTodos.Response200ApplicationJson(Map.of(), service.store);
        return CompletableFuture.completedFuture(res);
    }
}
