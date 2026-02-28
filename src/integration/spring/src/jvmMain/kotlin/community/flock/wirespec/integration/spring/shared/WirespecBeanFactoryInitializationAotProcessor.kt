package community.flock.wirespec.integration.spring.shared

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

/**
 * AOT processor that registers reflection hints for Wirespec Handler classes,
 * enabling GraalVM native image support.
 *
 * Wirespec's Spring integration uses reflection to discover Handler inner classes
 * and invoke their static methods (toRequest, fromRequest, toResponse, fromResponse).
 * In native images, these reflective operations fail unless the classes are registered
 * for reflection ahead of time.
 *
 * This processor scans all beans for Wirespec Handler implementations and registers
 * the necessary reflection hints for their declaring endpoint classes and all nested types.
 */
class WirespecBeanFactoryInitializationAotProcessor : BeanFactoryInitializationAotProcessor {

    override fun processAheadOfTime(
        beanFactory: ConfigurableListableBeanFactory,
    ): BeanFactoryInitializationAotContribution? {
        val endpointClasses = mutableSetOf<Class<*>>()

        for (beanName in beanFactory.beanDefinitionNames) {
            val beanType = beanFactory.getType(beanName) ?: continue
            collectEndpointClasses(beanType, endpointClasses)
        }

        if (endpointClasses.isEmpty()) return null

        return BeanFactoryInitializationAotContribution { generationContext, _ ->
            val hints = generationContext.runtimeHints
            for (endpointClass in endpointClasses) {
                registerHints(hints, endpointClass)
            }
        }
    }

    private fun collectEndpointClasses(beanType: Class<*>, endpointClasses: MutableSet<Class<*>>) {
        var clazz: Class<*>? = beanType
        while (clazz != null && clazz != Any::class.java) {
            for (iface in clazz.interfaces) {
                if (isWirespecHandler(iface)) {
                    val declaringClass = iface.declaringClass
                    if (declaringClass != null) {
                        endpointClasses.add(declaringClass)
                    }
                }
            }
            clazz = clazz.superclass
        }
    }

    private fun isWirespecHandler(clazz: Class<*>): Boolean =
        wirespecHandlerClasses.any { it.isAssignableFrom(clazz) }

    private fun registerHints(hints: RuntimeHints, endpointClass: Class<*>) {
        // Register the endpoint class itself.
        // Java: needs DECLARED_CLASSES to find the Handler inner class via getDeclaredClasses()
        // Kotlin: needs INVOKE_DECLARED_METHODS for toRequest/fromRequest/etc. and
        //         DECLARED_FIELDS for the INSTANCE singleton field
        hints.reflection().registerType(
            endpointClass,
            MemberCategory.DECLARED_CLASSES,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS,
        )

        // Register all inner classes (Handler, Request, Response, Path, Queries, etc.)
        for (innerClass in endpointClass.declaredClasses) {
            hints.reflection().registerType(
                innerClass,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.DECLARED_CLASSES,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            )

            // Register nested inner classes (e.g., Handler.Handlers, Response200.Headers)
            for (nestedClass in innerClass.declaredClasses) {
                hints.reflection().registerType(
                    nestedClass,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                )
            }
        }
    }

    companion object {
        private val wirespecHandlerClasses: List<Class<*>> by lazy {
            listOfNotNull(
                runCatching { Class.forName("community.flock.wirespec.java.Wirespec\$Handler") }.getOrNull(),
                runCatching { Class.forName("community.flock.wirespec.kotlin.Wirespec\$Handler") }.getOrNull(),
            )
        }
    }
}
