package community.flock.wirespec.examples.app.common;

public interface Converter<I, E> extends Internalizer<E, I>, Externalizer<I, E> {
}
