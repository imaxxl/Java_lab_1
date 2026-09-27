plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "ru.university"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.graphics")
}

application {
    mainClass.set("ru.university.socialnetwork.Main")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

tasks.test {
    useJUnitPlatform()

    forkEvery = 0
    maxParallelForks = 1

    testLogging {
        events("passed", "skipped", "failed")
    }
}
