package community.flock.wirespec.example.gradle.app.common;

public interface Converter<I, E> extends Internalizer<E, I>, Externalizer<I, E> {
}
