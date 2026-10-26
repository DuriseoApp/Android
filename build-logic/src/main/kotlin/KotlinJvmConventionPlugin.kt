import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * 순수 Kotlin(JVM) 모듈용 컨벤션. `domain`이 쓴다. Android 의존이 없다.
 * JVM 타깃은 카탈로그 `jvmTarget`, 테스트 도구는 docs/MODULES.md 3장.
 */
class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            val libs = versionCatalog
            val jvmTargetVersion = libs.versionOf("jvmTarget")
            val javaVersion = JavaVersion.toVersion(jvmTargetVersion)

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}
