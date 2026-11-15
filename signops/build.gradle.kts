plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.signops"
    compileSdk = 35
    defaultConfig {
        minSdk = 28 // passkeys through Credential Manager need Android 9; StrongBox keys need it too
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    publishing { singleVariant("release") { withSourcesJar(); withJavadocJar() } }
}

dependencies {
    api("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0") // passkeys on Android 13 and earlier
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    api("androidx.fragment:fragment-ktx:1.8.5")              // Approve's screen lock check needs a FragmentActivity
    implementation("androidx.biometric:biometric:1.1.0")      // fingerprint, face or screen lock before an approval
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")           // NotificationCompat: pushes with buttons and ongoing activities
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // the real org.json, where Android's is a stub in unit tests
}

// Maven Central coordinates dev.signops:android (15 Nov 2026: com.signops would need signops.com, which is not ours).
// The Kotlin package stays com.signops. Bundle: packages/android/maven-bundle.sh (signs with the key in ~/.config/signops/maven-signing.env).
group = "dev.signops"
version = "0.4.0"

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "dev.signops"
            artifactId = "android"
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("SignOps for Android")
                description.set("SignOps in an Android app: Approve (push approval of sign-ins with Android Keystore keys), passkeys through Credential Manager, push open reporting and notifications.")
                url.set("https://signops.dev/docs/")
                licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
                developers { developer { id.set("signops"); name.set("SignOps"); email.set("hello@signops.dev"); organization.set("Veritas Tech Solutions"); organizationUrl.set("https://signops.dev") } }
                scm { url.set("https://github.com/signOps-dev/signops-android"); connection.set("scm:git:https://github.com/signOps-dev/signops-android.git") }
            }
        }
    }
    repositories { maven { name = "bundle"; url = uri(layout.buildDirectory.dir("maven-bundle")) } }
}
