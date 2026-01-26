package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.example.maven.custom.app.exception.CallInterrupted;
import community.flock.wirespec.example.maven.custom.app.exception.Conflict;
import community.flock.wirespec.example.maven.custom.app.exception.NotFound;
import community.flock.wirespec.generated.java.endpoint.DeleteUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUsers;
import community.flock.wirespec.generated.java.endpoint.PostUser;
import community.flock.wirespec.generated.java.endpoint.UploadImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class LiveUserAdapter implements UserAdapter {

    private final UserClient client;
    private final UserConverter converter;

    public LiveUserAdapter(final UserClient client, final UserConverter converter) {
        this.client = client;
        this.converter = converter;
    }

    public List<User> getAllUsers(String name) {
        var res = complete(client.getUsers(new GetUsers.Request(name, UserClient.version)));
        return switch (res) {
            case GetUsers.Response200 r -> converter.internalize(r.body());
        };
    }

    public User getUserByName(final String name) {
        var res = complete(client.getUserByName(new GetUserByName.Request(name)));
        return switch (res) {
            case GetUserByName.Response200 r -> converter.internalize(r.body());
            case GetUserByName.Response404 ignored -> throw new NotFound.User();
        };
    }

    public User saveUser(final User user) {
        var res = complete(client.postUser(new PostUser.Request(converter.externalize(user))));
        return switch (res) {
            case PostUser.Response200 r -> converter.internalize(r.body());
            case PostUser.Response409 ignored -> throw new Conflict.User();
        };
    }

    public User deleteUserByName(final String name) {
        var res = complete(client.deleteUserByName(new DeleteUserByName.Request(name)));
        return switch (res) {
            case DeleteUserByName.Response200 r -> converter.internalize(r.body());
            case DeleteUserByName.Response404 ignored -> throw new NotFound.User();
        };
    }

    @Override
    public void uploadImage(String name, byte[] bytes) {
        var res = complete(client.uploadImage(new UploadImage.Request(name, bytes)));
        switch (res) {
            case UploadImage.Response201 ignored -> {}
            case UploadImage.Response404 ignored -> throw new NotFound.User();
        };
    }

    private <T> T complete(final CompletableFuture<T> future) {
        try {
            return future.get(10L, SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new CallInterrupted(e);
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new CallInterrupted(e);
        }
    }
}
