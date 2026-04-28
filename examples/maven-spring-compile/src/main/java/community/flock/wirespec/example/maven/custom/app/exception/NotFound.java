package community.flock.wirespec.example.maven.custom.app.exception;

public sealed class NotFound extends AppException {
    public NotFound(String message) {
        super(message);
    }

    @SuppressWarnings("java:S110")
    public static final class User extends NotFound {

        public User() {
            super("User not found");
        }
    }
}
