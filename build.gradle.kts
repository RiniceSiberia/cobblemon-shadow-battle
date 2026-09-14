plugins {
    java
    idea
    kotlin("jvm") version "2.2.20"
    id("net.neoforged.moddev") version "2.0.147"
}

group = "io.github.rinicesiberia"
version = "0.1.3"
base.archivesName.set("cobblemon-shadow-battle-neoforge")

repositories {
    mavenCentral()
    maven("https://maven.architectury.dev") { content { includeGroup("dev.architectury") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)

neoForge {
    enable {
        version = "21.1.66"
        setDisableRecompilation(true)
    }
    runs {
        create("client") { client() }
        create("server") { server(); programArgument("--nogui") }
    }
    mods { create("cobblebattle") { sourceSet(sourceSets.main.get()) } }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("dev.architectury:architectury-neoforge:13.0.8")
    implementation("maven.modrinth:cobblemon:jeZJOCEb")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val java9 by sourceSets.creating
tasks.named<JavaCompile>(java9.compileJavaTaskName) { options.release.set(9) }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-proc:none")
}
tasks.test {
    useJUnitPlatform()
    workingDir(layout.buildDirectory.dir("test-workspace"))
    doFirst { workingDir.mkdirs() }
}
tasks.jar {
    from(java9.output) { into("META-INF/versions/9") }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
idea.module.isDownloadSources = true

