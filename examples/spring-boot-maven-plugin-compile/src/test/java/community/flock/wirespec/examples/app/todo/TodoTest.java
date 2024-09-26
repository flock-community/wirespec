package community.flock.wirespec.examples.app.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoTest {

    @Autowired
    private TodoService todoService;

    @Test
    void testTodo() {
        assertFalse(todoService.getAllTodos().isEmpty());
    }
}
