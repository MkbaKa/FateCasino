plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/nexus/content/groups/public/")
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://repo.xenondevs.xyz/releases")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

val isDev = rootProject.version.toString().endsWith("-dev")

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    kotlin {
        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll(
                "-Xexplicit-backing-fields"
            )
        }
    }

    processResources {
        val properties = mapOf(
            "version" to rootProject.version.toString(),
            "group" to rootProject.group,
            "name" to rootProject.name,
        )
        inputs.properties(properties)
        filesMatching(listOf("paper-plugin.yml")) {
            expand(properties)
        }
    }

    shadowJar {
        archiveBaseName.set("${rootProject.name}-plugin")
        archiveClassifier.set("")

        exclude("META-INF/maven/**")

        exclude("org/intellij/lang/annotations/**")
        exclude("org/jetbrains/annotations/**")

        if (!isDev) {
            // kotlin
            relocate("kotlin.", "${rootProject.group}.libs.kotlin.")
            // coroutines
            relocate("kotlin.coroutines.", "${rootProject.group}.libs.coroutines.")
            relocate("kotlinx.coroutines.", "${rootProject.group}.libs.coroutines.")
            relocate("_COROUTINE.", "${rootProject.group}.libs._COROUTINE_.")
        }

        destinationDirectory.set(File("E:\\server\\1.21.11\\plugins"))
    }

}
