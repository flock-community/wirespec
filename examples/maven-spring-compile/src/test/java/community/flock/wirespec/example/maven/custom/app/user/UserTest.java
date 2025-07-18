package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.generated.java.model.AuthenticationPassword;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static community.flock.wirespec.example.maven.custom.app.user.UserContext.Service.deleteUserByName;
import static community.flock.wirespec.example.maven.custom.app.user.UserContext.Service.getAllUsers;
import static community.flock.wirespec.example.maven.custom.app.user.UserContext.Service.getUserByName;
import static community.flock.wirespec.example.maven.custom.app.user.UserContext.Service.saveUser;
import static community.flock.wirespec.example.maven.custom.app.user.UserContext.Service.uploadImageByName;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {
    private interface Context extends UserContext {
    }

    @Test
    void testGetAllUsers() {
        testContext(it -> {
            var users = getAllUsers(it, "name");
            assertEquals(1, users.size());
        });
    }

    @Test
    void testGetUserByName() {
        testContext(it -> {
            var user = getUserByName(it, "name");
            assertEquals("name", user.name());
        });
    }

    @Test
    void testPostUser() {
        testContext(it -> {
            var user = saveUser(it, new User("newName","secret"));
            assertEquals("newName", user.name());
        });
    }

    @Test
    void testDeleteUser() {
        testContext(it -> {
            saveUser(it, new User("newName", "secret"));
            var user = deleteUserByName(it, "newName");
            assertEquals("newName", user.name());
        });
    }

    @Test
    void testUploadImage() {
        testContext(it -> {
            byte[] bytes = "Hello World".getBytes();
            uploadImageByName(it, "newName", bytes);
            assertEquals(bytes, TestUserClient.images.get("newName"));
        });
    }

    private void testContext(Consumer<Context> test) {
        // Adapter cannot be inlined. We need only 1 instance per test not multiple.
        var adapter = new LiveUserAdapter(new TestUserClient(), new UserConverter());
        test.accept(() -> adapter);
    }
}
