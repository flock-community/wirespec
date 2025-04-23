package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.generated.java.endpoint.DeleteUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUserByName;
import community.flock.wirespec.generated.java.endpoint.GetUsers;
import community.flock.wirespec.generated.java.endpoint.PostUser;
import community.flock.wirespec.generated.java.endpoint.UploadImage;

public interface UserClient extends
        GetUsers.Handler,
        GetUserByName.Handler,
        PostUser.Handler,
        DeleteUserByName.Handler,
        UploadImage.Handler {
    String version = "1.0.0";
}
