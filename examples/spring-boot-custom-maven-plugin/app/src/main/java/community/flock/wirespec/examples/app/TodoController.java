package community.flock.wirespec.examples.app;

import hello.Todo;
import hello.TodoId;
import hello.TodoInput;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/todos")
class TodoController {

    @GetMapping("/")
    public List<String> list() {
        return List.of(
                Todo.class.getName(),
                TodoId.class.getName(),
                TodoInput.class.getName()
        );
    }

}
