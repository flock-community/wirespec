package community.flock.wirespec.examples.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import community.flock.wirespec.examples.grpc.proto.CreateTodoGrpc;
import community.flock.wirespec.examples.grpc.proto.CreateTodoRequest;
import community.flock.wirespec.examples.grpc.proto.ListTodosGrpc;
import community.flock.wirespec.examples.grpc.proto.ListTodosRequest;
import community.flock.wirespec.examples.grpc.proto.Todo;
import community.flock.wirespec.examples.grpc.proto.TodoInput;
import community.flock.wirespec.examples.grpc.proto.TodoList;
import community.flock.wirespec.examples.grpc.service.CreateTodoService;
import community.flock.wirespec.examples.grpc.service.ListTodosService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Loads the full Spring Boot context (which proves the Spring gRPC server auto-configuration and
 * the generated service beans wire up) and then exercises those Spring-managed service beans over
 * gRPC's in-process transport using the stubs Wirespec → protoc generated.
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
class TodoGrpcTest {

    @Autowired
    private CreateTodoService createTodoService;

    @Autowired
    private ListTodosService listTodosService;

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(createTodoService)
                .addService(listTodosService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void createsAndListsTodosOverGrpc() {
        var createStub = CreateTodoGrpc.newBlockingStub(channel);
        var listStub = ListTodosGrpc.newBlockingStub(channel);

        Todo created = createStub.createTodo(CreateTodoRequest.newBuilder()
                .setTodo(TodoInput.newBuilder()
                        .setName("Buy milk")
                        .setDone(false)
                        .build())
                .build());

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getName()).isEqualTo("Buy milk");
        assertThat(created.getDone()).isFalse();

        TodoList todos = listStub.listTodos(ListTodosRequest.getDefaultInstance());

        assertThat(todos.getTodosList()).hasSize(1);
        assertThat(todos.getTodos(0).getName()).isEqualTo("Buy milk");
    }
}
