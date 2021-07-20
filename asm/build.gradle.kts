plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.ow2.asm:asm:9.1")
    implementation("org.ow2.asm:asm-util:9.1")

    implementation(project(":mir"))
    implementation(project(":hir"))

    testImplementation("junit:junit:4.13")

    testImplementation(project(":runtime"))

    testImplementation(project(":parser"))
    testImplementation(project(":hir"))
}
