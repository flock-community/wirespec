package community.flock.wirespec.examples.app.common

fun interface Consumer<DTO : Any, DOMAIN : Any> {
    fun DTO.consume(): DOMAIN
}

fun interface Producer<DOMAIN, DTO> {
    fun DOMAIN.produce(): DTO
}
