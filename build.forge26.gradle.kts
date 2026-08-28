import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins { id("dev.architectury.loom-no-remap") }

version = "${project.property("mod.version")}+mc${sc.current.version}"
group = project.property("mod.group") as String
base.archivesName = "wWorldMapUtils ${project.property("mod.version")} Forge ${sc.current.version}"
sourceSets.main { java.exclude("com/wworldmap/utils/paper/**") }
repositories { maven("https://maven.minecraftforge.net/"); mavenCentral() }
dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    add("forge", "net.minecraftforge:forge:${sc.current.version}-${project.property("deps.forge")}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
java { withSourcesJar(); toolchain.languageVersion = JavaLanguageVersion.of(25) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release = 25 }
tasks.withType<AbstractArchiveTask>().configureEach { archiveVersion.set("") }
tasks.named("compileJava") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.named("processResources") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.named("compileTestJava") { dependsOn(tasks.named("stonecutterGenerateTest")) }
tasks.test { useJUnitPlatform(); workingDir = rootProject.projectDir }
tasks.processResources {
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
    val props = mapOf("version" to project.property("mod.version"), "minecraft_version" to sc.current.version,
        "forge_version" to project.property("deps.forge"),
        "javafml_version" to (project.property("deps.forge") as String).substringBefore('.'))
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") { expand(props) }
}
tasks.register<Copy>("buildAndCollect") {
    dependsOn(tasks.named("jar")); from(tasks.named("jar")); into(rootProject.layout.buildDirectory.dir("artifacts"))
}
