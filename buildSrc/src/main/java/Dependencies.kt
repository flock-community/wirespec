import Libraries.lsp
import Testing.jUnitApi
import Testing.jUnitEngine
import Versions.Libraries.lsp4j
import Versions.Tools.jUnit
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementLSP() {
    add("implementation", lsp)
}

fun DependencyHandlerScope.implementTesting() {
    add("testImplementation", jUnitApi)
    add("testRuntimeOnly", jUnitEngine)
}

private object Libraries {
    const val lsp = "org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4j"
}

private object Testing {
    const val jUnitApi = "org.junit.jupiter:junit-jupiter-api:$jUnit"
    const val jUnitEngine = "org.junit.jupiter:junit-jupiter-engine:$jUnit"
}
