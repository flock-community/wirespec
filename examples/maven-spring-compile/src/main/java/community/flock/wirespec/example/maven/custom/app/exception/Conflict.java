package community.flock.wirespec.example.maven.custom.app.exception;

public sealed class Conflict extends AppException {
    public Conflict(String message) {
        super(message);
    }

    @SuppressWarnings("java:S110") // Intentional: sealed example app exception hierarchy.
    public static final class User extends Conflict {

        public User() {
            super("User already exists");
        }
    }
}
