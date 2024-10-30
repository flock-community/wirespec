package community.flock.wirespec.example.maven.custom.app;

import hello.TodoCustom;
import hello.TodoIdCustom;
import hello.TodoInputCustom;
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
                TodoCustom.class.getName(),
                TodoIdCustom.class.getName(),
                TodoInputCustom.class.getName()
        );
    }

}
