import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val lp = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
android {
    namespace = "com.lumi.assistant"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.lumi.assistant"; minSdk = 28; targetSdk = 34
        versionCode = 1; versionName = "1.0"
        buildConfigField("String", "API_KEY", "\"${lp.getProperty("ANTHROPIC_API_KEY", "")}\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
