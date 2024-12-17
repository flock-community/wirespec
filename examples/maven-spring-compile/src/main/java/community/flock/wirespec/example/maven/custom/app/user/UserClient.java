package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.generated.java.*;

public interface UserClient extends
        GetUsersEndpoint.Handler,
        GetUserByNameEndpoint.Handler,
        PostUserEndpoint.Handler,
        DeleteUserByNameEndpoint.Handler,
        UploadImageEndpoint.Handler {
    String version = "1.0.0";
}
