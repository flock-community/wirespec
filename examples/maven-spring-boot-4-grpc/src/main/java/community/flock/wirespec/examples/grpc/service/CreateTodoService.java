package community.flock.wirespec.examples.grpc.service;

import community.flock.wirespec.examples.grpc.proto.CreateTodoGrpc;
import community.flock.wirespec.examples.grpc.proto.CreateTodoRequest;
import community.flock.wirespec.examples.grpc.proto.Todo;
import community.flock.wirespec.examples.grpc.proto.TodoInput;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implements the `CreateTodo` gRPC service that Wirespec generated from:
 *
 * <pre>rpc CreateTodo(todo: TodoInput) -&gt; Todo</pre>
 *
 * Spring gRPC auto-registers every {@link io.grpc.BindableService} bean with the server.
 */
@Service
public class CreateTodoService extends CreateTodoGrpc.CreateTodoImplBase {

    private final TodoStore store;

    public CreateTodoService(TodoStore store) {
        this.store = store;
    }

    @Override
    public void createTodo(CreateTodoRequest request, StreamObserver<Todo> responseObserver) {
        TodoInput input = request.getTodo();
        // The `! Error` in todo.ws documents the failure contract; in gRPC it surfaces as a status.
        if (input.getName().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("name must not be blank")
                    .asRuntimeException());
            return;
        }
        Todo todo = Todo.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName(input.getName())
                .setDone(input.getDone())
                .build();
        store.save(todo);
        responseObserver.onNext(todo);
        responseObserver.onCompleted();
    }
}
