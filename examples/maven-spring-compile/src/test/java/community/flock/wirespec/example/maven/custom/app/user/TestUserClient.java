package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.generated.java.endpoint.DeleteUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUsers;
import community.flock.wirespec.generated.java.endpoint.PostUser;
import community.flock.wirespec.generated.java.endpoint.UploadImage;
import community.flock.wirespec.generated.java.model.AuthenticationPassword;
import community.flock.wirespec.generated.java.model.UserDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class TestUserClient implements UserClient {

    public static final Set<UserDto> users = new HashSet<>(Set.of(
            new UserDto("name", new AuthenticationPassword("secret")),
            new UserDto("other name", new AuthenticationPassword("secret"))
    ));

    public static final Map<String, byte[]> images = new HashMap<>();

    @Override
    public CompletableFuture<GetUsers.Response<?>> getUsers(GetUsers.Request request) {
        var filtered = users.stream().filter(it -> Objects.equals(it.name(), request.queries().name())).toList();
        return completedFuture(new GetUsers.Response200(filtered));
    }

    @Override
    public CompletableFuture<GetUserByName.Response<?>> getUserByName(GetUserByName.Request request) {
        var res = users.stream()
                .filter(it -> Objects.equals(it.name(), request.path().name()))
                .findFirst()
                .<GetUserByName.Response<?>>map(GetUserByName.Response200::new)
                .orElseGet(() -> new GetUserByName.Response404());

        return completedFuture(res);
    }

    @Override
    public CompletableFuture<PostUser.Response<?>> postUser(PostUser.Request request) {
        var user = request.body();
        if (users.add(user)) {
            return completedFuture(new PostUser.Response200(user));
        } else {
            return completedFuture(new PostUser.Response409());
        }
    }

    @Override
    public CompletableFuture<DeleteUserByName.Response<?>> deleteUserByName(DeleteUserByName.Request request) {
        var res = users.stream()
                .filter(it -> Objects.equals(it.name(), request.path().name()))
                .findFirst()
                .<DeleteUserByName.Response<?>>map(body -> {
                    users.remove(body);
                    return new DeleteUserByName.Response200(body);
                })
                .orElseGet(() -> new DeleteUserByName.Response404());

        return completedFuture(res);
    }

    @Override
    public CompletableFuture<UploadImage.Response<?>> uploadImage(UploadImage.Request request) {
        images.put(request.path().name(),  request.body());
        return completedFuture(new UploadImage.Response201());
    }
}
