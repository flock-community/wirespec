package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

open class WirespecPluginExtension @Inject constructor(val objectFactory: ObjectFactory) {

    var input: String = ""

    var kotlin: Kotlin? = null
    fun kotlin(action: Action<in Kotlin>) {
        kotlin = Kotlin().also(action::execute)
    }

    var java: Java? = null
    fun java(action: Action<in Java>) {
        java = Java().also(action::execute)
    }

    var scala: Scala? = null
    fun scala(action: Action<in Scala>) {
        scala = Scala().also(action::execute)
    }

    var typescript: Typescript? = null
    fun typescript(action: Action<in Typescript>) {
        typescript = Typescript().also(action::execute)
    }

    var wirespec: Wirespec? = null
    fun wirespec(action: Action<in Wirespec>) {
        wirespec = Wirespec().also(action::execute)
    }

    companion object {
        abstract class HasTargetDirectory {
            var output: String = ""
        }

        data class Decorators(
            var type:String? = null,
            var endpoint:String? = null,
        )

        abstract class JvmLanguage : HasTargetDirectory() {
            var packageName: String = DEFAULT_PACKAGE_STRING
            var decorators: Decorators = Decorators()
            fun decorators(action: Action<in Decorators>) {
                decorators = Decorators().also(action::execute)
            }
        }

        class Kotlin : JvmLanguage()
        class Java : JvmLanguage()
        class Scala : JvmLanguage()
        class Typescript : HasTargetDirectory()
        class Wirespec : HasTargetDirectory()
    }
}
