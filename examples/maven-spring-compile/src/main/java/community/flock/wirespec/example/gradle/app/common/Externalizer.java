package community.flock.wirespec.example.gradle.app.common;

public interface Externalizer<I, E> {
    E externalize(I domain);
}
