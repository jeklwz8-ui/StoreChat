pluginManagement {
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
        // ★ 精确指定在 JitPack 中查找特定的库
        maven {
            url = uri("https://jitpack.io")
            content {
                // 只在该仓库中查找这个 group ID 的依赖
                includeGroup("com.github.JessYanCoding")
            }
        }
    }
}

rootProject.name = "StoreChat"
include(":app")
