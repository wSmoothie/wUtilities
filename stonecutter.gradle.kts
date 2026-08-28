plugins { id("dev.kikugie.stonecutter") }

stonecutter active "1.21.11-fabric"

stonecutter parameters {
    val (minecraft, loader) = current.project.split('-', limit = 2)
    properties { tags(minecraft, loader) }
    constants { match(loader, "fabric", "neoforge", "forge") }
}

val fabricAnchors = listOf("1.20", "1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2",
    "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1.2", "26.2")
val neoForgeAnchors = fabricAnchors.filterNot { it == "1.20" }
val forgeVersions = listOf("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2")

tasks.register<Delete>("cleanArtifacts") { delete(layout.buildDirectory.dir("artifacts")) }
tasks.register("buildAll") {
    group = "build"
    dependsOn(":bukkit:buildAndCollect")
    dependsOn(fabricAnchors.map { ":$it-fabric:buildAndCollect" })
    dependsOn(neoForgeAnchors.map { ":$it-neoforge:buildAndCollect" })
    dependsOn(forgeVersions.map { ":$it-forge:buildAndCollect" })
}
