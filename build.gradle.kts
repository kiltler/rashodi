// Корневой build-файл. Плагины применяются в каждом модуле отдельно
// (через alias из gradle/libs.versions.toml), чтобы не тянуть Android-плагины
// в чистый Kotlin-модуль :core. Здесь намеренно нет блока plugins {}.

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
