plugins {
    id("io.ksmt.ksmt-base")
}

dependencies {
    implementation(project(":ksmt-core"))
    runtimeOnly(project(":ksmt-z3"))
}

tasks.register<JavaExec>("runNoodlerZ3Example") {
    group = "application"
    description = "Run Noodler ping-pong example with Z3 backend"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.ksmt.solver.noodler.core.NoodlerZ3ExampleKt")
    jvmArgs("-Xss8m")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            addKsmtPom()
            addSourcesAndJavadoc(project)
            signKsmtPublication(project)
        }
    }
}
