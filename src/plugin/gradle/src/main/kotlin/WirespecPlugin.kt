package community.flock.wirespec.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class WirespecPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Marker plugin: exists so the `community.flock.wirespec.plugin.gradle`
        // id resolves and the task classes (CompileWirespecTask, ConvertWirespecTask)
        // are reachable on the build classpath. Consumers register task instances
        // directly; no project-level configuration is needed here.
    }
}
