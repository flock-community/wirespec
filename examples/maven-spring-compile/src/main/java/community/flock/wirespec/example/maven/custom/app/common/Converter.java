package community.flock.wirespec.example.maven.custom.app.common;

public interface Converter<I, E> extends Internalizer<E, I>, Externalizer<I, E> {
}
