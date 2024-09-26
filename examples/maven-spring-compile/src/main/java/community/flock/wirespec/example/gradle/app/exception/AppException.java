package community.flock.wirespec.example.gradle.app.exception;

public sealed class AppException extends RuntimeException permits Conflict, NotFound, CallInterrupted, SerializationException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
