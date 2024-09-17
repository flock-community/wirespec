package community.flock.wirespec.examples.app.exception;

public sealed class AppException extends RuntimeException permits Conflict, NotFound, CallInterrupted {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
