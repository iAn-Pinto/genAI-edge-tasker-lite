buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    }
}

allprojects {
    tasks.withType<org.gradle.api.tasks.testing.Test> {
        useJUnitPlatform()
    }
}
