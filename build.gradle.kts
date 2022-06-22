plugins {
    kotlin("multiplatform") version "1.7.0"
}

group = "community.flock"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {

    macosX64 {
        binaries {
            executable {
                entryPoint = "community.flock.wirespec.main"
            }
        }
    }

    linuxX64 {
        binaries {
            executable {
                entryPoint = "community.flock.wirespec.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "community.flock.wirespec.main"
            }
        }
    }

}
