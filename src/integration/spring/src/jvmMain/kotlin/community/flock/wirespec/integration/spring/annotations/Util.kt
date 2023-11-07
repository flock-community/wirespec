package community.flock.wirespec.integration.spring.annotations

import community.flock.wirespec.Wirespec
import java.io.BufferedReader
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.full.companionObjectInstance

object Util {
    fun Class<*>.isKotlinClass(): Boolean = declaredAnnotations.any {
        it.annotationClass.qualifiedName == "kotlin.Metadata"
    }

    fun Class<*>.getStaticClass() =
        if (isKotlinClass()) {
            kotlin.companionObjectInstance?.javaClass ?: error("not found")
        } else {
            this
        }

    fun Class<*>.getStaticField(name: String): Field? {
        return this.getStaticClass().getDeclaredField(name).apply { setAccessible(true) }
    }

    fun Class<*>.getStaticMethode(name: String): Method? {
        return this.getStaticClass().methods.find { it.name == name }
    }

    fun Method.invokeStatic(obj:Class<*>, contentMapper: Wirespec.ContentMapper<BufferedReader>): (Wirespec.Request<BufferedReader>) -> Wirespec.Request<*> {
        return this.invoke(obj.kotlin.companionObjectInstance, contentMapper) as (Wirespec.Request<BufferedReader>) -> Wirespec.Request<*>
    }

}