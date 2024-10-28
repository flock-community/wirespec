package community.flock.wirespec.example.maven.custom.app.exception;

public sealed class AppException extends RuntimeException permits Conflict, NotFound, CallInterrupted, SerializationException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
