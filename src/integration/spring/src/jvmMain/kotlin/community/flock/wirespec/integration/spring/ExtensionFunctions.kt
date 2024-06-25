package community.flock.wirespec.integration.spring.annotations

import community.flock.wirespec.Wirespec
import java.io.BufferedReader
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.full.companionObjectInstance

object ExtensionFunctions {
    private fun Class<*>.isKotlinClass(): Boolean = declaredAnnotations.any {
        it.annotationClass.qualifiedName == "kotlin.Metadata"
    }

    fun Class<*>.getStaticClass() =
        if (isKotlinClass()) {
            kotlin.companionObjectInstance?.javaClass ?: error("not found")
        } else {
            this
        }

    fun Class<*>.getStaticMethode(name: String): Method? {
        return this.getStaticClass().methods.find { it.name == name }
    }

    fun Class<*>.invoke(method: Method, contentMapper: Wirespec.ContentMapper<BufferedReader>, request: Wirespec.Request<BufferedReader>): Wirespec.Request<*> {
        return if (isKotlinClass()) {
            val func = method.invoke(this.kotlin.companionObjectInstance, contentMapper) as (Wirespec.Request<BufferedReader>) -> Wirespec.Request<*>
            return func(request)
        }else{
            val func = method.invoke(this, contentMapper) as java.util.function.Function<Wirespec.Request<BufferedReader>, Wirespec.Request<*>>
            func.apply(request)
        }
    }
}