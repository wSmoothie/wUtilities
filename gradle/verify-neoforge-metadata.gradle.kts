import java.util.jar.JarFile

val legacyMetadata = extra["verification.neoforgeLegacyMetadata"] as Boolean
val minecraftRange = extra["verification.neoforgeMinecraftRange"] as String
val verifyLoaderMetadata = tasks.register("verifyLoaderMetadata") {
    dependsOn(tasks.named("jar"))
    val packagedJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    inputs.file(packagedJar)
    doLast {
        val expectedName = if (legacyMetadata) "META-INF/mods.toml" else "META-INF/neoforge.mods.toml"
        val excludedName = if (legacyMetadata) "META-INF/neoforge.mods.toml" else "META-INF/mods.toml"
        JarFile(packagedJar.get().asFile).use { jar ->
            require(jar.getJarEntry(excludedName) == null) { "Unexpected loader metadata: $excludedName" }
            val entry = requireNotNull(jar.getJarEntry(expectedName)) { "Missing loader metadata: $expectedName" }
            val metadata = jar.getInputStream(entry).bufferedReader().readText()
            require(metadata.contains("loaderVersion=\"[${if (legacyMetadata) "1" else "3"},)\""))
            require(metadata.contains("version=\"${project.property("mod.version")}\""))
            require(metadata.contains("versionRange=\"[${project.property("deps.neoforge")},)\""))
            require(metadata.contains("versionRange=\"[$minecraftRange]\""))
            require(metadata.split("mandatory=true").size == if (legacyMetadata) 3 else 1)
            require(metadata.split("type=\"required\"").size == 3)
        }
    }
}
tasks.named("check") { dependsOn(verifyLoaderMetadata) }
