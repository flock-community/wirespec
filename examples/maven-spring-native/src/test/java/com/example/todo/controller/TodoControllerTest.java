package com.example.todo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void getTodos_returnsList() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/todos"))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void createTodo_returnsCreatedTodo() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/todos")
						.contentType(APPLICATION_JSON_VALUE)
						.content("""
								{"title": "Buy groceries", "completed": false}
								"""))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.title", equalTo("Buy groceries")))
				.andExpect(jsonPath("$.completed", equalTo(false)));
	}

	@Test
	void getTodoById_returnsExistingTodo() throws Exception {
		long id = createTodo("Read a book", false);

		MvcResult getResult = mockMvc.perform(get("/api/todos/{id}", id))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(getResult))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title", equalTo("Read a book")))
				.andExpect(jsonPath("$.completed", equalTo(false)));
	}

	@Test
	void getTodoById_returnsNotFoundForMissingTodo() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/todos/{id}", 99999))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", equalTo("Todo not found")));
	}

	@Test
	void updateTodo_updatesExistingTodo() throws Exception {
		long id = createTodo("Exercise", false);

		MvcResult updateResult = mockMvc.perform(put("/api/todos/{id}", id)
						.contentType(APPLICATION_JSON_VALUE)
						.content("""
								{"title": "Exercise daily", "completed": true}
								"""))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(updateResult))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title", equalTo("Exercise daily")))
				.andExpect(jsonPath("$.completed", equalTo(true)));
	}

	@Test
	void updateTodo_returnsNotFoundForMissingTodo() throws Exception {
		MvcResult result = mockMvc.perform(put("/api/todos/{id}", 99999)
						.contentType(APPLICATION_JSON_VALUE)
						.content("""
								{"title": "Nope", "completed": false}
								"""))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", equalTo("Todo not found")));
	}

	@Test
	void deleteTodo_deletesExistingTodo() throws Exception {
		long id = createTodo("Temporary task", false);

		MvcResult deleteResult = mockMvc.perform(delete("/api/todos/{id}", id))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(deleteResult))
				.andExpect(status().isNoContent());

		MvcResult getResult = mockMvc.perform(get("/api/todos/{id}", id))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(getResult))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteTodo_returnsNotFoundForMissingTodo() throws Exception {
		MvcResult result = mockMvc.perform(delete("/api/todos/{id}", 99999))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", equalTo("Todo not found")));
	}

	private long createTodo(String title, boolean completed) throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/todos")
						.contentType(APPLICATION_JSON_VALUE)
						.content("""
								{"title": "%s", "completed": %s}
								""".formatted(title, completed)))
				.andExpect(request().asyncStarted())
				.andReturn();

		String body = mockMvc.perform(asyncDispatch(createResult))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode node = objectMapper.readTree(body);
		return node.get("id").asLong();
	}
}
