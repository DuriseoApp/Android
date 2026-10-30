pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 카카오 SDK(com.kakao.sdk)만 이 저장소에서 받는다 (docs/progress/unit-2.md 위험 1)
        maven(url = "https://devrepo.kakao.com/nexus/content/groups/public/") {
            content {
                includeGroupByRegex("com\\.kakao.*")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Duriseo"
include(":app")
include(":core:designsystem")
include(":core:common")
include(":domain")
include(":data:remote")
include(":data:local")
include(":data:repository")
include(":feature:main")
include(":feature:calendar")
include(":feature:settings")
