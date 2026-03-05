package com.example.todo.service;

import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TodoService {

	private final List<Todo> todos = new ArrayList<>();
	private final AtomicLong idCounter = new AtomicLong(1);

	public TodoService() {
		todos.add(new Todo(idCounter.getAndIncrement(), "Buy groceries", false));
		todos.add(new Todo(idCounter.getAndIncrement(), "Walk the dog", true));
		todos.add(new Todo(idCounter.getAndIncrement(), "Finish project report", false));
	}

	public List<Todo> findAll() {
		return List.copyOf(todos);
	}

	public Optional<Todo> findById(Long id) {
		return todos.stream().filter(t -> t.id().equals(id)).findFirst();
	}

	public Todo create(TodoInput input) {
		var todo = new Todo(idCounter.getAndIncrement(), input.title(), input.completed());
		todos.add(todo);
		return todo;
	}

	public Optional<Todo> update(Long id, TodoInput input) {
		for (int i = 0; i < todos.size(); i++) {
			if (todos.get(i).id().equals(id)) {
				var updated = new Todo(id, input.title(), input.completed());
				todos.set(i, updated);
				return Optional.of(updated);
			}
		}
		return Optional.empty();
	}

	public boolean delete(Long id) {
		return todos.removeIf(t -> t.id().equals(id));
	}

}
