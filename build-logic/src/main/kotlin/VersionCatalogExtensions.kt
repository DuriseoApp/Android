import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** `gradle/libs.versions.toml` 카탈로그. 컨벤션 플러그인은 숫자를 코드에 쓰지 않고 여기서 읽는다. */
internal val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** `[versions]`의 값을 문자열로 읽는다. */
internal fun VersionCatalog.versionOf(alias: String): String =
    findVersion(alias).get().requiredVersion

/** `[versions]`의 값을 정수로 읽는다 (compileSdk, minSdk 등). */
internal fun VersionCatalog.intVersionOf(alias: String): Int = versionOf(alias).toInt()
