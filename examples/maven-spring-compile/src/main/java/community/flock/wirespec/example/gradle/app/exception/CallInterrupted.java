package community.flock.wirespec.example.gradle.app.exception;

public final class CallInterrupted extends AppException {
    public CallInterrupted(Exception e) {
        super("Call interrupted", e);
    }
}
