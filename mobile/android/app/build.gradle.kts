plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val releaseSigningVariables = mapOf(
    "ACORDE_ANDROID_KEYSTORE_PATH" to System.getenv("ACORDE_ANDROID_KEYSTORE_PATH"),
    "ACORDE_ANDROID_KEYSTORE_PASSWORD" to System.getenv("ACORDE_ANDROID_KEYSTORE_PASSWORD"),
    "ACORDE_ANDROID_KEY_ALIAS" to System.getenv("ACORDE_ANDROID_KEY_ALIAS"),
    "ACORDE_ANDROID_KEY_PASSWORD" to System.getenv("ACORDE_ANDROID_KEY_PASSWORD")
)
val hasReleaseSigning = releaseSigningVariables.values.all { !it.isNullOrBlank() }
val hasPartialReleaseSigning = releaseSigningVariables.values.any { !it.isNullOrBlank() } && !hasReleaseSigning
if (hasPartialReleaseSigning) {
    throw GradleException("Configure all ACORDE_ANDROID_* signing variables or leave all unset for local debug signing.")
}
if (!hasReleaseSigning) {
    logger.warn("Android release APK will use the debug key. Configure ACORDE_ANDROID_* signing variables for distribution.")
}

android {
    namespace = "br.com.acorde.acorde_mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "br.com.acorde.acorde_mobile"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    if (hasReleaseSigning) {
        signingConfigs.create("acordeRelease") {
            storeFile = file(releaseSigningVariables.getValue("ACORDE_ANDROID_KEYSTORE_PATH")!!)
            storePassword = releaseSigningVariables.getValue("ACORDE_ANDROID_KEYSTORE_PASSWORD")!!
            keyAlias = releaseSigningVariables.getValue("ACORDE_ANDROID_KEY_ALIAS")!!
            keyPassword = releaseSigningVariables.getValue("ACORDE_ANDROID_KEY_PASSWORD")!!
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "acordeRelease" else "debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
