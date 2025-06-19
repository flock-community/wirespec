import java.time.Duration

plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    connectTimeout = Duration.ofMinutes(10)
    clientTimeout = Duration.ofMinutes(10)
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
            stagingProfileId.set(System.getenv("SONATYPE_STAGING_PROFILE_ID"))
        }
    }
}
