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
    private final WirespecSerializer serializer;

    public LiveUserClient(final WirespecTransporter transporter,final  WirespecSerializer serializer) {
        this.transporter = transporter;
        this.serializer = serializer;
    }

    @Override
    public CompletableFuture<GetUsers.Response<?>> getUsers(final GetUsers.Request request) {
        return transporter.transport(GetUsers.Adapter.toRawRequest(this.serializer, request))
                .thenApplyAsync(res -> GetUsers.Adapter.fromRawResponse(this.serializer, res));
    }

    @Override
    public CompletableFuture<GetUserByName.Response<?>> getUserByName(final GetUserByName.Request request) {
        return transporter.transport(GetUserByName.Adapter.toRawRequest(this.serializer, request))
                .thenApplyAsync(res -> GetUserByName.Adapter.fromRawResponse(this.serializer, res));
    }

    @Override
    public CompletableFuture<PostUser.Response<?>> postUser(final PostUser.Request request) {
        return transporter.transport(PostUser.Adapter.toRawRequest(this.serializer, request))
                .thenApplyAsync(res -> PostUser.Adapter.fromRawResponse(this.serializer, res));
    }

    @Override
    public CompletableFuture<DeleteUserByName.Response<?>> deleteUserByName(final DeleteUserByName.Request request) {
        return transporter.transport(DeleteUserByName.Adapter.toRawRequest(this.serializer, request))
                .thenApplyAsync(res -> DeleteUserByName.Adapter.fromRawResponse(this.serializer, res));
    }

    @Override
    public CompletableFuture<UploadImage.Response<?>> uploadImage(UploadImage.Request request) {
        return transporter.transport(UploadImage.Adapter.toRawRequest(this.serializer, request))
                .thenApplyAsync(res -> UploadImage.Adapter.fromRawResponse(this.serializer, res));
    }
}
