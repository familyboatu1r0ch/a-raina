import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    //kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.1")
    implementation("com.google.guava:guava:18.0")
    implementation("org.apache.commons:commons-exec:1.3")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation("org.tukaani:xz:1.8")
    implementation("commons-codec:commons-codec:1.15")
    implementation("junit:junit:4.12")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    implementation("de.vandermeer:asciitable:0.3.2")
    implementation(project(":helper"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("cfig.packable.PackableLauncherKt")
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks {
    jar {
        manifest {
            attributes["Implementation-Title"] = "Android image reverse engineering toolkit"
            attributes["Main-Class"] = "cfig.packable.PackableLauncherKt"
        }
        from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
        excludes.addAll(mutableSetOf("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(":helper:jar")
    }
    test {
        testLogging {
            showExceptions = true
            showStackTraces = true
        }
    }
}
