import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

configure<MavenPublishBaseExtension> {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    if (System.getenv("GPG_PRIVATE_KEY") != null) {
        signAllPublications()
    }

    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString(),
    )

    pom {
        name.set("Wirespec")
        description.set("Type safe wires made easy")
        url.set("https://flock.community")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/license/apache-2-0")
            }
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/flock-community/wirespec/issues")
        }
        scm {
            connection.set("https://github.com/flock-community/wirespec.git")
            url.set("https://github.com/flock-community/wirespec")
        }
        developers {
            developer {
                name.set("Jerre van Veluw")
                email.set("jerre.van.veluw@flock.community")
            }
            developer {
                name.set("Willem Veelenturf")
                email.set("willem.veelenturf@flock.community")
            }
        }
    }
}
