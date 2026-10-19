pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }
        maven { url = uri("https://artifactory.appodeal.com/appodeal-public") }
    }
}
rootProject.name = "AuraPop"
include(":app")
