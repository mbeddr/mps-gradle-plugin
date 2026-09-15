tasks.register("build") {
    description = "Aggregates the build task of each included build."
    dependsOn(gradle.includedBuilds.map { it.task(":build") })
}

tasks.register("publishToMavenLocal") {
    description = "Aggregates the publishToMavenLocal task of each included build."
    dependsOn(gradle.includedBuilds.map { it.task(":publishToMavenLocal") })
}
