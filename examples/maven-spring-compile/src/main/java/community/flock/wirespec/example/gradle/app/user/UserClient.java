package community.flock.wirespec.example.gradle.app.user;

import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;

public interface UserClient extends
        GetUsersEndpoint.Handler,
        GetUserByNameEndpoint.Handler,
        PostUserEndpoint.Handler,
        DeleteUserByNameEndpoint.Handler {
    String version = "1.0.0";
}