plugins { java }

version = "0.2.0"
group = "com.wworldmap"
base.archivesName = "wWorldMapUtils 0.2.0 Bukkit 1.20+"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
}
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets.main {
    java.srcDir(rootProject.file("src/main/java"))
    java.include("com/wworldmap/utils/policy/**", "com/wworldmap/utils/protocol/PolicyProtocol.java",
        "com/wworldmap/utils/paper/**")
}
sourceSets.test { java.srcDir(rootProject.file("src/test/java")) }
java { withSourcesJar(); toolchain.languageVersion = JavaLanguageVersion.of(17) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release = 17 }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
tasks.test { useJUnitPlatform(); workingDir = rootProject.projectDir }
tasks.jar { archiveVersion.set("") }
tasks.register<Copy>("buildAndCollect") {
    dependsOn(tasks.named("jar")); from(tasks.named("jar")); into(rootProject.layout.buildDirectory.dir("artifacts"))
}
