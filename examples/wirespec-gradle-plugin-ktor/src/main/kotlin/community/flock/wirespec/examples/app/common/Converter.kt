package community.flock.wirespec.examples.app.common

interface Converter<DOMAIN : Any, DTO : Any> :
    Internalizer<DTO, DOMAIN>,
    Externalizer<DOMAIN, DTO>

fun interface Internalizer<DTO : Any, DOMAIN : Any> {
    fun DTO.internalize(): DOMAIN
}

fun interface Externalizer<DOMAIN : Any, DTO : Any> {
    fun DOMAIN.externalize(): DTO
}
