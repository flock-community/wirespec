package community.flock.wirespec.examples.app.common

interface Consumer<DTO : Any, DOMAIN : Any> {
    fun DTO.consume(): DOMAIN
}

interface Producer<DOMAIN, DTO> {
    fun DOMAIN.produce(): DTO
}
