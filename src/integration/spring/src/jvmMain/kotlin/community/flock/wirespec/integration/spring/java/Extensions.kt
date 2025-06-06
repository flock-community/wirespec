package community.flock.wirespec.integration.spring.java

import community.flock.wirespec.java.Wirespec
import java.io.BufferedReader
import java.lang.reflect.Method
import java.util.function.Function
import kotlin.reflect.full.companionObjectInstance

@Suppress("UNCHECKED_CAST")
fun Class<*>.invoke(
    method: Method,
    wirespecSerialization: Wirespec.Serialization<String>,
    request: Wirespec.Request<BufferedReader>,
): Wirespec.Request<*> = if (isKotlinClass()) {
    val func = method(
        kotlin.companionObjectInstance,
        wirespecSerialization,
    ) as (Wirespec.Request<BufferedReader>) -> Wirespec.Request<*>
    func(request)
} else {
    val func = method(
        this,
        wirespecSerialization,
    ) as Function<Wirespec.Request<BufferedReader>, Wirespec.Request<*>>
    func.apply(request)
}

private fun Class<*>.isKotlinClass(): Boolean = declaredAnnotations.any {
    it.annotationClass.qualifiedName == "kotlin.Metadata"
}
