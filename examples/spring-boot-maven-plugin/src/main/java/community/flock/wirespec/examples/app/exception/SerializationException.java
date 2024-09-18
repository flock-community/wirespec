package community.flock.wirespec.examples.app.exception;

public final class SerializationException extends AppException {
    public SerializationException(Exception cause) {
        super("Serialization failed", cause);
    }
}
