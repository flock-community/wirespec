package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.example.maven.custom.app.common.WirespecSerializer;
import community.flock.wirespec.example.maven.custom.app.common.WirespecTransporter;
import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;
import community.flock.wirespec.java.Wirespec;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LiveUserClient implements UserClient {

    private final WirespecTransporter transporter;
    private final Wirespec.ClientEdge<GetUsersEndpoint.Request, GetUsersEndpoint.Response<?>> getUsers;
    private final Wirespec.ClientEdge<GetUserByNameEndpoint.Request, GetUserByNameEndpoint.Response<?>> getUserByName;
    private final Wirespec.ClientEdge<PostUserEndpoint.Request, PostUserEndpoint.Response<?>> postUser;
    private final Wirespec.ClientEdge<DeleteUserByNameEndpoint.Request, DeleteUserByNameEndpoint.Response<?>> deleteUserByName;

    public LiveUserClient(final WirespecTransporter transporter,final  WirespecSerializer serializer) {
        this.transporter = transporter;
        this.getUsers = new GetUsersEndpoint.Handler.Handlers().getClient(serializer);
        this.getUserByName = new GetUserByNameEndpoint.Handler.Handlers().getClient(serializer);
        this.postUser = new PostUserEndpoint.Handler.Handlers().getClient(serializer);
        this.deleteUserByName = new DeleteUserByNameEndpoint.Handler.Handlers().getClient(serializer);
    }

    @Override
    public CompletableFuture<GetUsersEndpoint.Response<?>> getUsers(final GetUsersEndpoint.Request request) {
        return transporter.transport(getUsers.to(request))
                .thenApplyAsync(getUsers::from);
    }

    @Override
    public CompletableFuture<GetUserByNameEndpoint.Response<?>> getUserByName(final GetUserByNameEndpoint.Request request) {
        return transporter.transport(getUserByName.to(request))
                .thenApplyAsync(getUserByName::from);
    }

    @Override
    public CompletableFuture<PostUserEndpoint.Response<?>> postUser(final PostUserEndpoint.Request request) {
        return transporter.transport(postUser.to(request))
                .thenApplyAsync(postUser::from);
    }

    @Override
    public CompletableFuture<DeleteUserByNameEndpoint.Response<?>> deleteUserByName(final DeleteUserByNameEndpoint.Request request) {
        return transporter.transport(deleteUserByName.to(request))
                .thenApplyAsync(deleteUserByName::from);
    }
}
