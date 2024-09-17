package community.flock.wirespec.examples.app.common;

public interface Converter<DOMAIN, DTO> extends Internalizer<DTO, DOMAIN>, Externalizer<DOMAIN, DTO> {
}
