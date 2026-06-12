import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
  FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

fun releaseKeystoreProperty(name: String): String? = keystoreProperties
  .getProperty(name)
  ?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseKeystoreProperty("storeFile")
val releaseStorePassword = releaseKeystoreProperty("storePassword")
val releaseKeyAlias = releaseKeystoreProperty("keyAlias")
val releaseKeyPassword = releaseKeystoreProperty("keyPassword")
val hasReleaseKeystore = listOf(
  releaseStoreFile,
  releaseStorePassword,
  releaseKeyAlias,
  releaseKeyPassword,
).all { it != null } && releaseStoreFile?.let { rootProject.file(it).exists() } == true

android {
  namespace = "com.park.reagentkeeper"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.park.ontect"
    minSdk = 26
    targetSdk = 35
    versionCode = 3
    versionName = "0.3.0"
  }

  flavorDimensions += "edition"
  productFlavors {
    create("free") {
      dimension = "edition"
      versionNameSuffix = "-free"
    }
  }

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = rootProject.file(requireNotNull(releaseStoreFile))
        storePassword = requireNotNull(releaseStorePassword)
        keyAlias = requireNotNull(releaseKeyAlias)
        keyPassword = requireNotNull(releaseKeyPassword)
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (hasReleaseKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2025.02.00")

  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("com.google.android.material:material:1.12.0")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
