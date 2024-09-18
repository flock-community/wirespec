package community.flock.wirespec.examples.app.exception;

public final class CallInterrupted extends AppException {
    public CallInterrupted(Exception e) {
        super("Call interrupted", e);
    }
}
