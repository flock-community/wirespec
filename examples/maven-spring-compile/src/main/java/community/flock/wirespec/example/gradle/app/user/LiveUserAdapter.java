package community.flock.wirespec.example.gradle.app.user;

import community.flock.wirespec.example.gradle.app.exception.CallInterrupted;
import community.flock.wirespec.example.gradle.app.exception.Conflict;
import community.flock.wirespec.example.gradle.app.exception.NotFound;
import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;
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
        var res = complete(client.getUsers(new GetUsersEndpoint.Request(name, UserClient.version)));
        return switch (res) {
            case GetUsersEndpoint.Response200 r -> converter.internalize(r.getBody());
        };
    }

    public User getUserByName(final String name) {
        var res = complete(client.getUserByName(new GetUserByNameEndpoint.Request(name)));
        return switch (res) {
            case GetUserByNameEndpoint.Response200 r -> converter.internalize(r.getBody());
            case GetUserByNameEndpoint.Response404 ignored -> throw new NotFound.User();
        };
    }

    public User saveUser(final User user) {
        var res = complete(client.postUser(new PostUserEndpoint.Request(converter.externalize(user))));
        return switch (res) {
            case PostUserEndpoint.Response200 r -> converter.internalize(r.getBody());
            case PostUserEndpoint.Response409 ignored -> throw new Conflict.User();
        };
    }

    public User deleteUserByName(final String name) {
        var res = complete(client.deleteUserByName(new DeleteUserByNameEndpoint.Request(name)));
        return switch (res) {
            case DeleteUserByNameEndpoint.Response200 r -> converter.internalize(r.getBody());
            case DeleteUserByNameEndpoint.Response404 ignored -> throw new NotFound.User();
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
