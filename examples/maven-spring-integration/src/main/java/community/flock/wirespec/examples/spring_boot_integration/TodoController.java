package community.flock.wirespec.examples.spring_boot_integration;

import community.flock.wirespec.generated.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
class TodoController implements GetTodosEndpoint.Handler, GetTodoByIdEndpoint.Handler, CreateTodoEndpoint.Handler, UpdateTodoEndpoint.Handler, DeleteTodoEndpoint.Handler {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<CreateTodoEndpoint.Response<?>> createTodo(CreateTodoEndpoint.Request request) {
        var todoInput = switch (request){
            case CreateTodoEndpoint.Request req -> req.getBody();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        service.create(todo);
        var res = new CreateTodoEndpoint.Response200(todo);
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<DeleteTodoEndpoint.Response<?>> deleteTodo(DeleteTodoEndpoint.Request request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodoByIdEndpoint.Response<?>> getTodoById(GetTodoByIdEndpoint.Request request) {
        var id = switch (request){
            case GetTodoByIdEndpoint.Request req -> req.getPath();
        };
        System.out.println(id);
        var res = new GetTodoByIdEndpoint.Response200(service.store.get(0));
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<UpdateTodoEndpoint.Response<?>> updateTodo(UpdateTodoEndpoint.Request request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodosEndpoint.Response<?>> getTodos(GetTodosEndpoint.Request request) {
        var res = new GetTodosEndpoint.Response200(service.store);
        return CompletableFuture.completedFuture(res);
    }
}
