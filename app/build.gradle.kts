import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// La clave con la que se firma la app NO está en el repositorio: vive en
// `local.properties` (que nunca se sube a git) y en el archivo .jks, que
// tampoco se sube. Sin esas dos cosas no se puede firmar una versión nueva.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) {
        archivo.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.cesar.ahorros"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cesar.ahorros"
        // 24 = Android 7.0 Nougat. La app usa java.time (LocalDate, Instant),
        // que existe de forma nativa desde la 26: se resuelve con desugaring.
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.12"
    }

    // -----------------------------------------------------------------
    // Dos versiones de la MISMA app:
    //
    //  - "general": para cualquiera. Es la que sube a Google Play.
    //  - "g11":     la misma app pero con la lista del salón del 11G ya
    //               escrita (vive en app/src/g11/).
    //
    // Llevan distinto paquete, así que las dos se pueden instalar en el
    // mismo teléfono sin pisarse.
    // -----------------------------------------------------------------
    flavorDimensions += "plantel"
    productFlavors {
        create("general") {
            dimension = "plantel"
        }
        create("g11") {
            dimension = "plantel"
            applicationIdSuffix = ".g11"
            versionNameSuffix = "-11G"
        }
    }

    signingConfigs {
        create("release") {
            val keystore = rootProject.file("app/keystore/ahorros.jks")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = propiedadesLocales.getProperty("aura.storePassword") ?: ""
                keyAlias = propiedadesLocales.getProperty("aura.keyAlias") ?: "ahorros"
                keyPassword = propiedadesLocales.getProperty("aura.keyPassword") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Si no está la clave, se compila igual pero el APK sale sin firmar.
            val keystore = rootProject.file("app/keystore/ahorros.jks")
            signingConfig = if (keystore.exists()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time en Android 7.0 (API 24): se "desazuca" al ejecutar.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Robolectric necesita los recursos y el manifiesto fusionados para
    // poder renderizar Compose en el PC.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Tests de la lógica del dinero: corren en el PC (JVM), sin emulador.
    testImplementation("junit:junit:4.13.2")

    // Capturas de pantalla REALES de la app, generadas en el PC (sin emulador):
    // Robolectric con renderizado nativo de gráficos.
    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
