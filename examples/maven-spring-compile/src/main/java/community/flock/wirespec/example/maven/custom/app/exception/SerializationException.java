package community.flock.wirespec.example.maven.custom.app.exception;

public final class SerializationException extends AppException {
    public SerializationException(Exception cause) {
        super("Serialization failed", cause);
    }
}
