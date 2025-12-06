import org.gradle.api.tasks.JavaExec

plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.jflex:jflex:1.9.1")
    implementation("com.github.vbmacher:java-cup:11b-20160615")
    implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")
}

application {
    mainClass.set("InterpreterMain")
}

val genDir = layout.buildDirectory.dir("generated-src/main/java")

sourceSets {
    named("main") {
        java {
            srcDir(genDir)
        }
    }
}

val compileClasspath: Configuration by configurations.getting

val generateLexer by tasks.registering {
    val flexFile = layout.projectDirectory.file("src/main/jflex/BashLexer.flex")

    inputs.file(flexFile)
    outputs.dir(genDir)

    doLast {
        ant.withGroovyBuilder {
            "mkdir"("dir" to genDir.get().asFile)

            "java"(
                "classname" to "jflex.Main",
                "fork" to true,
                "classpath" to compileClasspath.asPath
            ) {
                "arg"("value" to "-d")
                "arg"("value" to genDir.get().asFile.absolutePath)
                "arg"("value" to flexFile.asFile.absolutePath)
            }
        }
    }
}

val generateParser by tasks.registering(JavaExec::class) {
    val cupFile = layout.projectDirectory.file("src/main/cup/BashParser.cup")

    inputs.file(cupFile)
    outputs.dir(genDir)

    classpath = compileClasspath
    mainClass.set("java_cup.Main")

    args(
        "-parser", "BashParser",
        "-symbols", "sym",
        "-destdir", genDir.get().asFile.absolutePath,
        cupFile.asFile.absolutePath
    )
}

tasks.compileJava {
    dependsOn(generateLexer, generateParser)
}
