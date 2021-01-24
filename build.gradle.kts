import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.nbaztec"
version = "1.2.10"

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        languageVersion = "1.4"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    google()
    mavenCentral()
    jcenter()
}

val testAndroidMain: SourceSet by sourceSets.creating {
    java {
        srcDir("src/testAndroid/kotlin")
        resources.srcDir("src/test/resources")
    }
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val testAndroidMainImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val testAndroidMainRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.4.2")
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.4.21")
    implementation("org.dom4j", "dom4j", "2.1.1")
    implementation("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.4.21")
    implementation("org.eclipse.jgit", "org.eclipse.jgit", "5.10.0.202012080955-r")
    implementation("org.apache.httpcomponents", "httpmime", "4.5.13")
    implementation("com.google.code.gson", "gson", "2.8.6")
    compileOnly("com.android.tools.build", "gradle", "4.0.1")

    testImplementation("junit", "junit", "4.13.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.0")
    testImplementation("io.mockk", "mockk", "1.10.5")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.0")

    testAndroidMainImplementation("com.android.tools.build", "gradle", "4.0.1")
    testAndroidMainImplementation("junit", "junit", "4.13.1")
    testAndroidMainImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.0")
    testAndroidMainImplementation("io.mockk", "mockk", "1.10.5")
    testAndroidMainRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.0")
}

tasks {
    test {
        useJUnitPlatform()
    }

    val testAndroid by registering(Test::class) {
        useJUnitPlatform()
        description = "Runs android source set tests."
        group = "verification"
        testClassesDirs = testAndroidMain.output.classesDirs
        classpath = testAndroidMain.runtimeClasspath
        shouldRunAfter(test)
    }

    check {
        dependsOn(testAndroid)
    }

    jacocoTestReport {
        dependsOn(test, testAndroid)
        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
        executionData(File(buildDir, "jacoco/testAndroid.exec"))
    }

    create("setupPublishSecrets") {
        doLast {
            val key = System.getenv("GRADLE_PUBLISH_KEY")
            val secret = System.getenv("GRADLE_PUBLISH_SECRET")

            check(key != null) { "GRADLE_PUBLISH_KEY is required" }
            check(secret != null) { "GRADLE_PUBLISH_SECRET is required" }

            System.setProperty("gradle.publish.key", key)
            System.setProperty("gradle.publish.secret", secret)
        }
    }
}

plugins {
    idea
    jacoco
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.4.21"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("com.github.nbaztec.coveralls-jacoco") version "1.2.10"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "coveralls-jacoco"
            from(components["java"])
        }
    }
}

gradlePlugin {
    plugins {
        create("coverallsJacocoPlugin") {
            id = "com.github.nbaztec.coveralls-jacoco"
            implementationClass = "org.gradle.plugin.coveralls.jacoco.CoverallsJacocoPlugin"
        }
    }
}

pluginBundle {
    (plugins) {
        "coverallsJacocoPlugin" {
            website = "http://github.com/nbaztec/coveralls-jacoco-gradle-plugin/"
            vcsUrl = "https://github.com/nbaztec/coveralls-jacoco-gradle-plugin.git"
            description = "Send jacoco coverage data to coveralls.io"
            tags = listOf("coverage", "coveralls")
            displayName = "Coveralls Jacoco Plugin"
        }
    }
}

idea {
    module {
        testSourceDirs = testSourceDirs.apply {
            addAll(testAndroidMain.allJava.srcDirs)
        }
    }
}
