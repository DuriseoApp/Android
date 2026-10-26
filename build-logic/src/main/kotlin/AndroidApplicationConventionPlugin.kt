import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val catalog = versionCatalog
            val javaVersion = JavaVersion.toVersion(catalog.versionOf("jvmTarget"))

            extensions.configure<ApplicationExtension> {
                compileSdk = catalog.intVersionOf("compileSdk")

                defaultConfig {
                    minSdk = catalog.intVersionOf("minSdk")
                    targetSdk = catalog.intVersionOf("targetSdk")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                compileOptions {
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }

                buildFeatures {
                    buildConfig = true
                }
            }
        }
    }
}
