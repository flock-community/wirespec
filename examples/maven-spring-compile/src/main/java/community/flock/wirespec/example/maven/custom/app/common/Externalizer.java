package community.flock.wirespec.example.maven.custom.app.common;

public interface Externalizer<I, E> {
    E externalize(I domain);
}
