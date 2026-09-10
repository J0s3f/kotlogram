plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

// This intentionally lives outside `build/`: CI runs `gradle clean` after
// downloading the platform artifacts, and `clean` must not remove them.
val nativeResourceRoot = rootProject.layout.projectDirectory.dir("generated/native")

tasks.processResources {
    from(rootProject.file("LICENSE"), rootProject.file("NOTICE"))
    from(nativeResourceRoot) {
        into("native")
    }
}

dependencies {
    api(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("kotlogramme")
                description.set("Kotlin/JVM Telegram API built on the grammers Rust client")
                url.set("https://github.com/J0s3f/kotlogram")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/J0s3f/kotlogram.git")
                    url.set("https://github.com/J0s3f/kotlogram")
                }
            }
        }
    }
}
