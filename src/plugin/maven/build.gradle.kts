
import org.w3c.dom.Node
import org.w3c.dom.NodeList

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.plugin.development)
}

group = "${libs.versions.group.id.get()}.plugin.maven"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:converter:avro"))
    implementation(project(":src:converter:openapi"))
    implementation(project(":src:plugin:arguments"))
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.maven.plugin)
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.25")
}

java {
    withSourcesJar()
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "wirespec-maven-plugin"
            from(components["java"])
//            pom.withXml {
//                val root = asElement()
//                println("childNodes: ${root.childNodes.length}")
//                val dependencies = root.childNodes.find {it.nodeName == "dependencies"}
//                val kotlinCompilerDep = dependencies?.childNodes?.map {
//                    val dep = it.childNodes.find { it.nodeName == "artifactId" && it.textContent == "kotlin-compiler-embeddable"}
//                    if(dep != null){
//                        val version = it.childNodes.find { it.nodeName == "version" }
//                        version?.textContent = "${'$'}{kotlin.version}"
//                    }
//                    println(dep?.textContent)
//                }
//
//
//            }
        }
    }
}

tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
    dependsOn(":src:converter:openapi:publishToMavenLocal")
    dependsOn(":src:plugin:arguments:publishToMavenLocal")
}

mavenPlugin {
    artifactId.set("wirespec-maven-plugin")
    description.set("Plugin to run wirespec compiler")
    goalPrefix.set("wirespec")
}

private fun <T> NodeList.map(action: (Node) -> T) = (0 until length)
    .map{item(it)}
    .map {  action(it) }

private fun NodeList.filter(predicate: (Node) -> Boolean) = (0 until length)
    .map{item(it)}
    .filter {  predicate(it) }

private fun NodeList.find(predicate: (Node) -> Boolean) = (0 until length)
    .map { item(it) }
    .find {  predicate(it) }
