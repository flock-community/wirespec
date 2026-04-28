package com.example.todo.controller;

import com.example.todo.generated.endpoint.*;
import com.example.todo.generated.model.Error;
import com.example.todo.service.TodoService;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class TodoController implements
		GetTodos.Handler,
		GetTodoById.Handler,
		CreateTodo.Handler,
		UpdateTodo.Handler,
		DeleteTodo.Handler {

	private static final String TODO_NOT_FOUND = "Todo not found";

	private final TodoService todoService;

	public TodoController(TodoService todoService) {
		this.todoService = todoService;
	}

	@Override
	public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request request) {
		var todos = todoService.findAll();
		return CompletableFuture.completedFuture(new GetTodos.Response200(todos));
	}

	@Override
	public CompletableFuture<GetTodoById.Response<?>> getTodoById(GetTodoById.Request request) {
		return CompletableFuture.completedFuture(
				todoService.findById(request.path().id())
						.<GetTodoById.Response<?>>map(GetTodoById.Response200::new)
						.orElse(new GetTodoById.Response404(new Error(TODO_NOT_FOUND)))
		);
	}

	@Override
	public CompletableFuture<CreateTodo.Response<?>> createTodo(CreateTodo.Request request) {
		var todo = todoService.create(request.body());
		return CompletableFuture.completedFuture(new CreateTodo.Response200(todo));
	}

	@Override
	public CompletableFuture<UpdateTodo.Response<?>> updateTodo(UpdateTodo.Request request) {
		return CompletableFuture.completedFuture(
				todoService.update(request.path().id(), request.body())
						.<UpdateTodo.Response<?>>map(UpdateTodo.Response200::new)
						.orElse(new UpdateTodo.Response404(new Error(TODO_NOT_FOUND)))
		);
	}

	@Override
	public CompletableFuture<DeleteTodo.Response<?>> deleteTodo(DeleteTodo.Request request) {
		if (todoService.delete(request.path().id())) {
			return CompletableFuture.completedFuture(new DeleteTodo.Response204());
		}
		return CompletableFuture.completedFuture(new DeleteTodo.Response404(new Error(TODO_NOT_FOUND)));
	}

}
