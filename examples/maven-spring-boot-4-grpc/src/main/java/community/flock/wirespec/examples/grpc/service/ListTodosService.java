package community.flock.wirespec.examples.grpc.service;

import community.flock.wirespec.examples.grpc.proto.ListTodosGrpc;
import community.flock.wirespec.examples.grpc.proto.ListTodosRequest;
import community.flock.wirespec.examples.grpc.proto.TodoList;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

/**
 * Implements the `ListTodos` gRPC service that Wirespec generated from:
 *
 * <pre>rpc ListTodos() -&gt; TodoList</pre>
 */
@Service
public class ListTodosService extends ListTodosGrpc.ListTodosImplBase {

    private final TodoStore store;

    public ListTodosService(TodoStore store) {
        this.store = store;
    }

    @Override
    public void listTodos(ListTodosRequest request, StreamObserver<TodoList> responseObserver) {
        TodoList list = TodoList.newBuilder().addAllTodos(store.findAll()).build();
        responseObserver.onNext(list);
        responseObserver.onCompleted();
    }
}
