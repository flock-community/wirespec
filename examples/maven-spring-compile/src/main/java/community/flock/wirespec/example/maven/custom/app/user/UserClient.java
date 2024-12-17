package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.generated.java.DeleteUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUserByNameEndpoint;
import community.flock.wirespec.generated.java.GetUsersEndpoint;
import community.flock.wirespec.generated.java.PostUserEndpoint;
import community.flock.wirespec.generated.java.UploadImageEndpoint;

public interface UserClient extends
        GetUsersEndpoint.Handler,
        GetUserByNameEndpoint.Handler,
        PostUserEndpoint.Handler,
        DeleteUserByNameEndpoint.Handler,
        UploadImageEndpoint.Handler {
    String version = "1.0.0";
}
