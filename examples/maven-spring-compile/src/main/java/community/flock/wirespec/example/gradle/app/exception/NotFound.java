package community.flock.wirespec.example.gradle.app.exception;

public sealed class NotFound extends AppException {
    public NotFound(String message) {
        super(message);
    }

    public static final class User extends NotFound {

        public User() {
            super("User not found");
        }
    }
}
