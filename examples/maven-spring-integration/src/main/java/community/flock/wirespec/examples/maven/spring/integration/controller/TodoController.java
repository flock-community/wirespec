package community.flock.wirespec.examples.maven.spring.integration.controller;

import community.flock.wirespec.generated.examples.spring.endpoint.CreateTodo;
import community.flock.wirespec.generated.examples.spring.endpoint.DeleteTodo;
import community.flock.wirespec.generated.examples.spring.endpoint.GetTodoById;
import community.flock.wirespec.generated.examples.spring.endpoint.GetTodos;
import community.flock.wirespec.generated.examples.spring.endpoint.UploadAttachment;
import community.flock.wirespec.generated.examples.spring.model.Todo;
import community.flock.wirespec.generated.examples.spring.endpoint.UpdateTodo;
import org.springframework.web.bind.annotation.RestController;
import community.flock.wirespec.examples.maven.spring.integration.service.TodoService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.Integer.parseInt;

@RestController
class TodoController implements GetTodos.Handler, GetTodoById.Handler, CreateTodo.Handler, UpdateTodo.Handler, DeleteTodo.Handler, UploadAttachment.Handler {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<CreateTodo.Response<?>> createTodo(CreateTodo.Request request) {
        var todoInput = switch (request){
            case CreateTodo.Request req -> req.body();
        };
        var todo = new Todo(
                UUID.randomUUID().toString(),
                todoInput.name(),
                todoInput.done()
        );
        service.create(todo);
        var res = new CreateTodo.Response200(todo);
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<DeleteTodo.Response<?>> deleteTodo(DeleteTodo.Request request) {
        return null;
    }

    @Override
    public CompletableFuture<GetTodoById.Response<?>> getTodoById(GetTodoById.Request request) {
        var id = switch (request){
            case GetTodoById.Request req -> req.path().id();
        };
        var res = new GetTodoById.Response200(service.store.get(parseInt(id)));
        return CompletableFuture.completedFuture(res);
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

    @Override
    public CompletableFuture<UploadAttachment.Response<?>> uploadAttachment(UploadAttachment.Request request) {
        byte[] bytes = request.body().plain();
        byte[] csv = request.body().csv();
        Todo json = request.body().json();
        service.uploadFile("plain", bytes);
        service.uploadFile("csv", csv);
        service.uploadFile("json", json);
        return CompletableFuture.completedFuture(new UploadAttachment.Response201());
    }
}
