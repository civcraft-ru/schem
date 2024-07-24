plugins {
    `java-library`

    `maven-publish`
    signing
    alias(libs.plugins.nexuspublish)
}

group = "dev.hollowcube"
version = System.getenv("TAG_VERSION") ?: "dev"
description = "Schematic reader and writer for Minestom"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    compileOnly(libs.minestom)
    testImplementation(libs.minestom)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.bundles.logback)
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/civcraft-ru/schem")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

publishing.publications.create<MavenPublication>("maven") {
    groupId = "dev.hollowcube"
    artifactId = "schem"
    version = project.version.toString()

    from(project.components["java"])

    pom {
        name.set(artifactId)
        description.set(project.description)
        url.set("https://github.com/civcraft-ru/schem")

        licenses {
            license {
                name.set("MIT")
                url.set("https://github.com/civcraft-ru/schem/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("mworzala")
                name.set("Matt Worzala")
                email.set("matt@hollowcube.dev")
            }

            developer {
                id.set("hohserg")
                name.set("hohserg")
                email.set("hohserg1@gmail.com")
            }
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/civcraft-ru/schem/issues")
        }

        scm {
            connection.set("scm:git:git://github.com/civcraft-ru/schem.git")
            developerConnection.set("scm:git:git@github.com:civcraft-ru/schem.git")
            url.set("https://github.com/civcraft-ru/schem")
            tag.set(System.getenv("TAG_VERSION") ?: "HEAD")
        }

        ciManagement {
            system.set("Github Actions")
            url.set("https://github.com/civcraft-ru/schem/actions")
        }
    }
}
/*
signing {
    isRequired = System.getenv("CI") != null

    val privateKey = System.getenv("GPG_PRIVATE_KEY")
    val keyPassphrase = System.getenv()["GPG_PASSPHRASE"]
    useInMemoryPgpKeys(privateKey, keyPassphrase)

    sign(publishing.publications)
}
*/