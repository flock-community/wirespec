package community.flock.wirespec.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class WirespecPlugin : Plugin<Project> {
    // Tasks are registered directly by consumers via CompileWirespecTask and ConvertWirespecTask;
    // applying this plugin only signals the Gradle build that those task types are available.
    override fun apply(project: Project) {}
}
