package community.flock.wirespec.examples.maven.spring.integration.controller;

import community.flock.wirespec.examples.maven.spring.integration.service.TodoService;
import community.flock.wirespec.generated.examples.spring.endpoint.*;
import community.flock.wirespec.generated.examples.spring.model.Todo;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.Integer.parseInt;

@RestController
class TodoController implements GetTodos.Handler, GetTodoById.Handler, CreateTodo.Handler, UpdateTodo.Handler, DeleteTodo.Handler {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<CreateTodo.Response<?>> createTodo(CreateTodo.Request request) {
        var todoInput = switch (request) {
            case CreateTodo.Request req -> req.body();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        return service.create(todo)
                .thenApply(CreateTodo.Response200::new);
    }

    @Override
    public CompletableFuture<DeleteTodo.Response<?>> deleteTodo(DeleteTodo.Request request) {
        return service.delete(request.path().id())
                .thenApply(DeleteTodo.Response200::new);
    }

    @Override
    public CompletableFuture<GetTodoById.Response<?>> getTodoById(GetTodoById.Request request) {
        var id = switch (request) {
            case GetTodoById.Request req -> req.path().id();
        };

        Todo todo = service.store.get(parseInt(id));
        return CompletableFuture.completedFuture(new GetTodoById.Response200(todo));
    }

    @Override
    public CompletableFuture<UpdateTodo.Response<?>> updateTodo(UpdateTodo.Request request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request request) {
        var res = new GetTodos.Response200(service.store);
        return CompletableFuture.completedFuture(res);
    }
}
