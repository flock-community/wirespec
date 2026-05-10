package community.flock.wirespec.integration.kotest

import java.lang.reflect.Method

/**
 * Reflective Scala ↔ Kotlin conversions used by [WirespecScalaGeneratorAdapter].
 *
 * Filled in by Task 7 once a Java fixture is on the test classpath. Until then
 * `dispatch` is intentionally unimplemented so any caller surfaces a clear
 * error rather than silent garbage.
 */
internal object ScalaInterop {

    fun dispatch(inner: KotestGenerator, method: Method, args: Array<Any?>): Any? {
        check(method.name == "generate") {
            "Scala adapter received unexpected method call: ${method.name}"
        }
        TODO("ScalaInterop.dispatch — implemented in Task 7")
    }
}
