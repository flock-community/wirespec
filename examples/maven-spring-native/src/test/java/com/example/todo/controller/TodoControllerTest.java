package com.example.todo.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void getTodos_returnsList() {
		assertThat(mvc.get().uri("/api/todos").exchange())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$")
				.asArray()
				.isNotEmpty();
	}

	@Test
	void createTodo_returnsCreatedTodo() {
		var result = mvc.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Buy groceries", "completed": false}
						""")
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.id").isNotNull();
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Buy groceries");
		assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(false);
	}

	@Test
	void getTodoById_returnsExistingTodo() throws Exception {
		long id = createTodo("Read a book", false);

		var result = mvc.get().uri("/api/todos/{id}", id).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Read a book");
		assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(false);
	}

	@Test
	void getTodoById_returnsNotFoundForMissingTodo() {
		var result = mvc.get().uri("/api/todos/{id}", 99999).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Todo not found");
	}

	@Test
	void updateTodo_updatesExistingTodo() throws Exception {
		long id = createTodo("Exercise", false);

		var result = mvc.put().uri("/api/todos/{id}", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Exercise daily", "completed": true}
						""")
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Exercise daily");
		assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(true);
	}

	@Test
	void updateTodo_returnsNotFoundForMissingTodo() {
		var result = mvc.put().uri("/api/todos/{id}", 99999)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Nope", "completed": false}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Todo not found");
	}

	@Test
	void deleteTodo_deletesExistingTodo() throws Exception {
		long id = createTodo("Temporary task", false);

		assertThat(mvc.delete().uri("/api/todos/{id}", id).exchange())
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/api/todos/{id}", id).exchange())
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteTodo_returnsNotFoundForMissingTodo() {
		var result = mvc.delete().uri("/api/todos/{id}", 99999).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Todo not found");
	}

	private long createTodo(String title, boolean completed) throws Exception {
		var result = mvc.post().uri("/api/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "%s", "completed": %s}
						""".formatted(title, completed))
				.exchange();

		assertThat(result).hasStatusOk();
		String body = result.getResponse().getContentAsString();
		return ((Number) JsonPath.parse(body).read("$.id")).longValue();
	}
}
