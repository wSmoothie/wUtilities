import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins { id("dev.architectury.loom") }
apply(from = rootProject.file("gradle/compatibility-cohorts.gradle.kts"))

val mcRange = extra["compatibility.minecraftRange"] as String
val archiveVersion = extra["compatibility.archiveVersion"] as String
version = "${project.property("mod.version")}+$archiveVersion"
group = project.property("mod.group") as String
base.archivesName = "wWorldMapUtils ${project.property("mod.version")} NeoForge $archiveVersion"
val requiredJava = if (sc.current.parsed >= "1.20.5") JavaVersion.VERSION_21 else JavaVersion.VERSION_17
sourceSets.main { java.exclude("com/wworldmap/utils/paper/**") }

repositories { maven("https://maven.neoforged.net/releases/"); mavenCentral() }

loom { mods { register("wworldmap_utils") { sourceSet(sourceSets.main.get()) } } }
dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings(loom.officialMojangMappings())
    add("neoForge", "net.neoforged:neoforge:${project.property("deps.neoforge")}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
java { withSourcesJar(); toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release = requiredJava.majorVersion.toInt() }
tasks.withType<AbstractArchiveTask>().configureEach { archiveVersion.set("") }
tasks.named("compileJava") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.named("processResources") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.named("compileTestJava") { dependsOn(tasks.named("stonecutterGenerateTest")) }
tasks.test { useJUnitPlatform(); workingDir = rootProject.projectDir }
tasks.processResources {
    exclude("fabric.mod.json", "META-INF/mods.toml")
    val props = mapOf("version" to project.property("mod.version"), "minecraft_version" to mcRange,
        "java_version" to requiredJava.majorVersion, "neoforge_version" to project.property("deps.neoforge"))
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}
tasks.register<Copy>("buildAndCollect") {
    dependsOn(tasks.named("remapJar")); from(tasks.named("remapJar")); into(rootProject.layout.buildDirectory.dir("artifacts"))
}
