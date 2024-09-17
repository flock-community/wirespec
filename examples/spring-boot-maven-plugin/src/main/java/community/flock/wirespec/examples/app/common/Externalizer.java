package community.flock.wirespec.examples.app.common;

public interface Externalizer<DOMAIN, DTO> {
    DTO externalize(DOMAIN domain);
}
