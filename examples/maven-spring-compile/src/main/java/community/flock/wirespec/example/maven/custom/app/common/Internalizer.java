package community.flock.wirespec.example.maven.custom.app.common;

import java.util.List;

public interface Internalizer<E, I> {
    I internalize(E dto);

    default List<I> internalize(List<E> dto) {
        return dto.stream().map(this::internalize).toList();
    }
}
