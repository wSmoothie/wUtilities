data class CompatibilityCohort(val label: String, val anchor: String, val members: List<String>) {
    fun range(loader: String): String = when {
        members.size == 1 -> members.single()
        loader == "neoforge" -> "${members.first()},${members.last()}"
        else -> ">=${members.first()} <=${members.last()}"
    }
}

val cohorts = listOf(
    CompatibilityCohort("1.20-1.20.1", "1.20", listOf("1.20", "1.20.1")),
    CompatibilityCohort("1.20.2", "1.20.2", listOf("1.20.2")),
    CompatibilityCohort("1.20.3-4", "1.20.3", listOf("1.20.3", "1.20.4")),
    CompatibilityCohort("1.20.5-6", "1.20.5", listOf("1.20.5", "1.20.6")),
    CompatibilityCohort("1.21-1.21.1", "1.21", listOf("1.21", "1.21.1")),
    CompatibilityCohort("1.21.2-4", "1.21.2", listOf("1.21.2", "1.21.3", "1.21.4")),
    CompatibilityCohort("1.21.5", "1.21.5", listOf("1.21.5")),
    CompatibilityCohort("1.21.6-8", "1.21.6", listOf("1.21.6", "1.21.7", "1.21.8")),
    CompatibilityCohort("1.21.9-10", "1.21.9", listOf("1.21.9", "1.21.10")),
    CompatibilityCohort("1.21.11", "1.21.11", listOf("1.21.11")),
    CompatibilityCohort("26.1.2", "26.1.2", listOf("26.1.2")),
    CompatibilityCohort("26.2", "26.2", listOf("26.2"))
)

val compatibilityLoader = project.name.substringAfterLast('-')
val compatibilityMinecraft = project.name.removeSuffix("-$compatibilityLoader")
val compatibilityCohort = cohorts.single { compatibilityMinecraft in it.members }
extra["compatibility.minecraftRange"] = compatibilityCohort.range(compatibilityLoader)
extra["compatibility.archiveVersion"] = compatibilityCohort.label
