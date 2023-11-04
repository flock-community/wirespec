package community.flock.wirespec.integration.spring.annotations

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

    fun Class<*>.getStaticField(name: String) = this.getStaticClass().getDeclaredField(name).apply { setAccessible(true) }

    fun <T> Method.invokeStatic(obj:Any, vararg args:Any): T {
        return this.invoke(obj, args) as T
    }

}