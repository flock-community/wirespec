package com.example.todo.controller;

import com.example.todo.generated.model.Todo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getTodos_returnsList() {
		webTestClient.get().uri("/api/todos")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Todo.class);
	}

	@Test
	void createTodo_returnsCreatedTodo() {
		webTestClient.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Buy groceries", "completed": false}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isNotEmpty()
				.jsonPath("$.title").isEqualTo("Buy groceries")
				.jsonPath("$.completed").isEqualTo(false);
	}

	@Test
	void getTodoById_returnsExistingTodo() {
		var created = webTestClient.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Read a book", "completed": false}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Todo.class)
				.returnResult()
				.getResponseBody();

		webTestClient.get().uri("/api/todos/{id}", created.id())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.title").isEqualTo("Read a book")
				.jsonPath("$.completed").isEqualTo(false);
	}

	@Test
	void getTodoById_returnsNotFoundForMissingTodo() {
		webTestClient.get().uri("/api/todos/{id}", 99999)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Todo not found");
	}

	@Test
	void updateTodo_updatesExistingTodo() {
		var created = webTestClient.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Exercise", "completed": false}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Todo.class)
				.returnResult()
				.getResponseBody();

		webTestClient.put().uri("/api/todos/{id}", created.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Exercise daily", "completed": true}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.title").isEqualTo("Exercise daily")
				.jsonPath("$.completed").isEqualTo(true);
	}

	@Test
	void updateTodo_returnsNotFoundForMissingTodo() {
		webTestClient.put().uri("/api/todos/{id}", 99999)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Nope", "completed": false}
						""")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Todo not found");
	}

	@Test
	void deleteTodo_deletesExistingTodo() {
		var created = webTestClient.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"title": "Temporary task", "completed": false}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Todo.class)
				.returnResult()
				.getResponseBody();

		webTestClient.delete().uri("/api/todos/{id}", created.id())
				.exchange()
				.expectStatus().isNoContent();

		webTestClient.get().uri("/api/todos/{id}", created.id())
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void deleteTodo_returnsNotFoundForMissingTodo() {
		webTestClient.delete().uri("/api/todos/{id}", 99999)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Todo not found");
	}
}
