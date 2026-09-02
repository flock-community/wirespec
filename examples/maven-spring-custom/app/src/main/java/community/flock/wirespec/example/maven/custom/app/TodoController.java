package community.flock.wirespec.example.maven.custom.app;

import community.flock.wirespec.example.maven.custom.generated.custom.CustomCustom;
import community.flock.wirespec.example.maven.custom.generated.custom.TodoCustom;
import community.flock.wirespec.example.maven.custom.generated.custom.TodoIdCustom;
import community.flock.wirespec.example.maven.custom.generated.custom.TodoInputCustom;
import community.flock.wirespec.example.maven.custom.generated.model.Todo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
class TodoController {

    @GetMapping("/")
    public List<String> list() {

        return List.of(
                Todo.class.getName(),
                CustomCustom.class.getName(),
                TodoCustom.class.getName(),
                TodoIdCustom.class.getName(),
                TodoInputCustom.class.getName());
    }
}
