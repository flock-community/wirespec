package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class WirespecPluginExtension @Inject constructor(val objectFactory: ObjectFactory) {

    var compile: CompilePlugin? = null
    fun compile(action: Action<in CompilePlugin>) {
        compile = CompilePlugin().also(action::execute)
    }

    var convert: ConvertPlugin? = null
    fun convert(action: Action<in ConvertPlugin>) {
        convert = ConvertPlugin().also(action::execute)
    }

    var custom: CustomPlugin? = null
    fun custom(action: Action<in CustomPlugin>) {
        custom = CustomPlugin().also(action::execute)
    }

    companion object {

        open class BasePlugin(
            var input: String = "${'&'}{projectDir}/src/main/wirespec",
            var output: String = "${'&'}{layout.buildDirectory.get()}/generated",
            var packageName: String = DEFAULT_PACKAGE_STRING
        )

        open class CompilePlugin : BasePlugin() {
            var languages: List<Language> = emptyList()
            var shared: Boolean = true
        }

        class ConvertPlugin : CompilePlugin() {
            var format: Format? = null
            var strict: Boolean = true
        }

        class CustomPlugin : BasePlugin() {
            var emitter: Emitter? = null
            var extention: String? = null
            var shared: Shared? = null
            var split: Boolean = false
        }
    }
}
