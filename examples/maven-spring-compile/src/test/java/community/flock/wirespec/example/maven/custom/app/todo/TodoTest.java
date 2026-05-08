package community.flock.wirespec.example.maven.custom.app.todo;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoTest {

    @Autowired
    private TodoService todoService;

    @Test
    void testTodo() {
        assertFalse(todoService.getAllTodos().isEmpty());
    }
}
