package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.example.maven.custom.app.common.WirespecSerializer;
import community.flock.wirespec.example.maven.custom.app.common.WirespecTransporter;
import community.flock.wirespec.generated.java.endpoint.DeleteUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUsers;
import community.flock.wirespec.generated.java.endpoint.PostUser;
import community.flock.wirespec.generated.java.endpoint.UploadImage;
import community.flock.wirespec.java.Wirespec;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LiveUserClient implements UserClient {

    private final WirespecTransporter transporter;
    private final Wirespec.ClientEdge<GetUsers.Request, GetUsers.Response<?>> getUsers;
    private final Wirespec.ClientEdge<GetUserByName.Request, GetUserByName.Response<?>> getUserByName;
    private final Wirespec.ClientEdge<PostUser.Request, PostUser.Response<?>> postUser;
    private final Wirespec.ClientEdge<DeleteUserByName.Request, DeleteUserByName.Response<?>> deleteUserByName;
    private final Wirespec.ClientEdge<UploadImage.Request, UploadImage.Response<?>> uploadImage;

    public LiveUserClient(final WirespecTransporter transporter,final  WirespecSerializer serializer) {
        this.transporter = transporter;
        this.getUsers = new GetUsers.Handler.Handlers().getClient(serializer);
        this.getUserByName = new GetUserByName.Handler.Handlers().getClient(serializer);
        this.postUser = new PostUser.Handler.Handlers().getClient(serializer);
        this.deleteUserByName = new DeleteUserByName.Handler.Handlers().getClient(serializer);
        this.uploadImage = new UploadImage.Handler.Handlers().getClient(serializer);
    }

    @Override
    public CompletableFuture<GetUsers.Response<?>> getUsers(final GetUsers.Request request) {
        return transporter.transport(getUsers.to(request))
                .thenApplyAsync(getUsers::from);
    }

    @Override
    public CompletableFuture<GetUserByName.Response<?>> getUserByName(final GetUserByName.Request request) {
        return transporter.transport(getUserByName.to(request))
                .thenApplyAsync(getUserByName::from);
    }

    @Override
    public CompletableFuture<PostUser.Response<?>> postUser(final PostUser.Request request) {
        return transporter.transport(postUser.to(request))
                .thenApplyAsync(postUser::from);
    }

    @Override
    public CompletableFuture<DeleteUserByName.Response<?>> deleteUserByName(final DeleteUserByName.Request request) {
        return transporter.transport(deleteUserByName.to(request))
                .thenApplyAsync(deleteUserByName::from);
    }

    @Override
    public CompletableFuture<UploadImage.Response<?>> uploadImage(UploadImage.Request request) {
        return transporter.transport(uploadImage.to(request))
                .thenApplyAsync(uploadImage::from);
    }
}
