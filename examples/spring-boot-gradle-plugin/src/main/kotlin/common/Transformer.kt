package community.flock.wirespec.examples.app.common

interface Transformer<DTO : Any, DOMAIN : Any> : Consumer<DTO, DOMAIN>, Producer<DOMAIN, DTO>

interface Consumer<DTO : Any, DOMAIN : Any> {
    fun DTO.consume(): DOMAIN
}

interface Producer<DOMAIN, DTO> {
    fun DOMAIN.produce(): DTO
}
