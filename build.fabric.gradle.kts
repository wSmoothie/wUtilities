import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.util.jar.JarFile
import groovy.json.JsonSlurper

plugins { id("dev.kikugie.loom-back-compat") }
apply(from = rootProject.file("gradle/compatibility-cohorts.gradle.kts"))

val mcRange = extra["compatibility.minecraftRange"] as String
val archiveVersion = extra["compatibility.archiveVersion"] as String
// Quilt 0.30 advertises Fabric Loader 0.19.2; compilation may use a newer loader.
val loaderMinimum = project.findProperty("deps.fabric_loader_minimum")
    ?: project.property("deps.fabric_loader")
version = "${project.property("mod.version")}+$archiveVersion"
group = project.property("mod.group") as String
base.archivesName = "wUtilities ${project.property("mod.version")} Fabric $archiveVersion"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}
sourceSets.main { java.exclude("com/wworldmap/utils/paper/**") }

loom { mods { register("wutilities") { sourceSet(sourceSets.main.get()) } } }

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${project.property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("deps.fabric_api")}")
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
    exclude("plugin.yml", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")
    val props = mapOf("version" to project.property("mod.version"), "minecraft_version" to mcRange,
        "java_version" to requiredJava.majorVersion, "fabric_loader_version" to loaderMinimum)
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

val verifyLoaderMetadata = tasks.register("verifyLoaderMetadata") {
    dependsOn(tasks.named("jar"))
    val packagedJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    inputs.file(packagedJar)
    doLast {
        JarFile(packagedJar.get().asFile).use { jar ->
            val entry = requireNotNull(jar.getJarEntry("fabric.mod.json"))
            val metadata = JsonSlurper().parse(jar.getInputStream(entry)) as Map<*, *>
            val dependencies = metadata["depends"] as Map<*, *>
            require(dependencies["fabricloader"] == ">=$loaderMinimum")
            if (sc.current.version in setOf("26.1.2", "26.2")) {
                require(dependencies["fabricloader"] == ">=0.19.2") {
                    "Minecraft 26.x artifacts must accept Quilt 0.30's Fabric Loader compatibility version"
                }
            }
        }
    }
}
tasks.named("check") { dependsOn(verifyLoaderMetadata) }
tasks.register<Copy>("buildAndCollect") {
    dependsOn(loomx.modJar)
    from(loomx.modJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("artifacts"))
}
