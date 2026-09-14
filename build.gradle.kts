plugins {
    java
    idea
    kotlin("jvm") version "2.2.20"
    id("net.neoforged.moddev") version "2.0.147"
    id("com.gradleup.shadow") version "9.1.0"
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

val bundledLibraries by configurations.creating
configurations.implementation { extendsFrom(bundledLibraries) }

dependencies {
    bundledLibraries("org.yaml:snakeyaml:2.6")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("dev.architectury:architectury-neoforge:13.0.8")
    implementation("maven.modrinth:cobblemon:jeZJOCEb")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-proc:none")
}
tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.shadowJar)
    systemProperty("battle.artifact", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
    classpath += sourceSets.main.get().compileClasspath
    workingDir(layout.buildDirectory.dir("test-workspace"))
    doFirst { workingDir.mkdirs() }
}
tasks.jar {
    archiveClassifier.set("thin")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.shadowJar {
    configurations = listOf(bundledLibraries)
    archiveClassifier.set("")
    relocate("org.yaml.snakeyaml", "io.github.rinicesiberia.shadowbattle.internal.yaml")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.assemble { dependsOn(tasks.shadowJar) }
idea.module.isDownloadSources = true


