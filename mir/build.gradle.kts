plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":hir"))

    testImplementation(project(":parser"))
    testImplementation("junit:junit:4.13")
}
