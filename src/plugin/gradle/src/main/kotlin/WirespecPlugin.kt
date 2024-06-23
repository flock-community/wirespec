package community.flock.wirespec.plugin.gradle


import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.toDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.BufferedReader
import java.io.File
import kotlin.io.path.Path
import kotlin.streams.asSequence

class WirespecPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("Wirespec plugin")
    }
}
