package community.flock.wirespec.example.maven.custom.app.common

fun interface Consumer<DTO : Any, DOMAIN : Any> {
    fun DTO.consume(): DOMAIN
}

fun interface Producer<DOMAIN : Any, DTO : Any> {
    fun DOMAIN.produce(): DTO
}
