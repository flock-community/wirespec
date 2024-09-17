package community.flock.wirespec.examples.app.exception;

public sealed class Conflict extends AppException {
    public Conflict(String message) {
        super(message);
    }

    public static final class User extends Conflict {

        public User() {
            super("User already exists");
        }
    }
}
