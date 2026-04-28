plugins {
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.51-beta")

    testImplementation("io.papermc.paper:paper-api:26.1.2.build.51-beta")
    // MockBukkit (MC 26.1 対応モジュール) は Maven Central に未公開。
    // `.mockbukkit-sha` に書かれた dev/26.1.1 上の commit SHA をローカルビルドし
    // mavenLocal に publish して使う。詳細は scripts/install_mockbukkit.sh を参照。
    val mockbukkitShaFile = rootProject.file(".mockbukkit-sha")
    require(mockbukkitShaFile.exists()) {
        ".mockbukkit-sha が見つかりません。scripts/install_mockbukkit.sh を実行してください。"
    }
    val mockbukkitShortSha = mockbukkitShaFile.readText().trim().take(7)
    require(mockbukkitShortSha.matches(Regex("[0-9a-f]{7}"))) {
        ".mockbukkit-sha の内容が不正です: '$mockbukkitShortSha'"
    }
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1:dev-$mockbukkitShortSha")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }
    compileTestJava {
        options.encoding = "UTF-8"
        options.release = 25
    }
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        archiveBaseName.set("SavePosition")
    }
}
