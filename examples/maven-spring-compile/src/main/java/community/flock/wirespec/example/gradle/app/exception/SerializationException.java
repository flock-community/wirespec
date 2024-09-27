package community.flock.wirespec.example.gradle.app.exception;

public final class SerializationException extends AppException {
    public SerializationException(Exception cause) {
        super("Serialization failed", cause);
    }
}
