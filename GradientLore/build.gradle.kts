plugins {
    java
}

group = "com.example.gradientlore"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Pinned to a known 26.3 build. If you run a newer build, bumping this is optional;
    // you can also try "26.3.build.+" to always use the latest.
    compileOnly("io.papermc.paper:paper-api:26.3.build.143-beta")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
