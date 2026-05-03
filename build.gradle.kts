plugins {
    kotlin("jvm") version "2.3.20"
    id("java-library")
    `maven-publish`
    signing
}

group = "io.github.romsper"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.codeborne:selenide:7.16.0")
    implementation("com.microsoft.playwright:playwright:1.59.0")
    implementation("com.github.valfirst.browserup-proxy:browserup-proxy-core:3.3.0")

    testImplementation("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("OmniWeb")
                description.set("A framework-agnostic Kotlin wrapper for Playwright and Selenide with Smart Waits and Network Interception")
                url.set("https://github.com/romsper/omniweb")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("romsper")
                        name.set("Roman")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/romsper/omniweb.git")
                    developerConnection.set("scm:git:ssh://github.com/romsper/omniweb.git")
                    url.set("https://github.com/romsper/omniweb")
                }
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}