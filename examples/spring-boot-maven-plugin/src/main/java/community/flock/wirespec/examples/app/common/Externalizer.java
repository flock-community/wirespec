package community.flock.wirespec.examples.app.common;

public interface Externalizer<I, E> {
    E externalize(I domain);
}
