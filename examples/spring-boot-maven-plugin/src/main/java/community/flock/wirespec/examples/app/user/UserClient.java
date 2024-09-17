package community.flock.wirespec.examples.app.user;

import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;

public interface UserClient extends
        GetUsersEndpoint.Handler,
        GetUserByNameEndpoint.Handler,
        PostUserEndpoint.Handler,
        DeleteUserByNameEndpoint.Handler {
}
