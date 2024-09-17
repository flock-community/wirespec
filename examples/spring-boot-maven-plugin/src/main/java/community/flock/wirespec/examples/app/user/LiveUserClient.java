package community.flock.wirespec.examples.app.user;

import community.flock.wirespec.examples.app.common.WirespecHandler;
import community.flock.wirespec.examples.app.common.WirespecSerializer;
import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;
import community.flock.wirespec.java.Wirespec;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LiveUserClient implements UserClient {

    private final WirespecHandler handler;
    private final Wirespec.ClientEdge<GetUsersEndpoint.Request, GetUsersEndpoint.Response<?>> getUsers;
    private final Wirespec.ClientEdge<GetUserByNameEndpoint.Request, GetUserByNameEndpoint.Response<?>> getUserByName;
    private final Wirespec.ClientEdge<PostUserEndpoint.Request, PostUserEndpoint.Response<?>> postUser;
    private final Wirespec.ClientEdge<DeleteUserByNameEndpoint.Request, DeleteUserByNameEndpoint.Response<?>> deleteUserByName;

    public LiveUserClient(WirespecHandler handler, WirespecSerializer serializer) {
        this.handler = handler;
        this.getUsers = new GetUsersEndpoint.Handler.Handlers().getClient(serializer);
        this.getUserByName = new GetUserByNameEndpoint.Handler.Handlers().getClient(serializer);
        this.postUser = new PostUserEndpoint.Handler.Handlers().getClient(serializer);
        this.deleteUserByName = new DeleteUserByNameEndpoint.Handler.Handlers().getClient(serializer);
    }

    @Override
    public CompletableFuture<GetUsersEndpoint.Response<?>> getUsers(GetUsersEndpoint.Request request) {
        return handler.handle(getUsers.to(request))
                .thenApplyAsync(getUsers::from);
    }

    @Override
    public CompletableFuture<GetUserByNameEndpoint.Response<?>> getUserByName(GetUserByNameEndpoint.Request request) {
        return handler.handle(getUserByName.to(request))
                .thenApplyAsync(getUserByName::from);
    }

    @Override
    public CompletableFuture<PostUserEndpoint.Response<?>> postUser(PostUserEndpoint.Request request) {
        return handler.handle(postUser.to(request))
                .thenApplyAsync(postUser::from);
    }

    @Override
    public CompletableFuture<DeleteUserByNameEndpoint.Response<?>> deleteUserByName(DeleteUserByNameEndpoint.Request request) {
        return handler.handle(deleteUserByName.to(request))
                .thenApplyAsync(deleteUserByName::from);
    }
}
