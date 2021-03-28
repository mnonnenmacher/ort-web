import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    val kotlinVersion: String by System.getProperties()
    val kvisionVersion: String by System.getProperties()

    kotlin("multiplatform") version kotlinVersion
    id("kotlinx-serialization") version kotlinVersion
    id("kvision") version kvisionVersion
}

version = "1.0.0-SNAPSHOT"
group = "org.ossreviewtoolkit"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://jitpack.io") }

    exclusiveContent {
        forRepository {
            maven("https://repo.gradle.org/gradle/libs-releases-local/")
        }

        filter {
            includeGroup("org.gradle")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.eclipse.org/content/repositories/sw360-releases/")
        }

        filter {
            includeGroup("org.eclipse.sw360")
        }
    }
}

val commonsCodecVersion: String by project
val exposedVersion: String by project
val h2Version: String by project
val hikariVersion: String by project
val hopliteVersion: String by project
val jdbcNamedParametersVersion: String by project
val kotlinVersion: String by System.getProperties()
val ktorVersion: String by project
val kvisionVersion: String by System.getProperties()
val kweryVersion: String by project
val log4jCoreVersion: String by project
val logbackVersion: String by project
val ortVersion: String by project
val pgsqlVersion: String by project

val mainClassName = "io.ktor.server.netty.EngineMain"
val webDir = file("src/frontendMain/web")

kotlin {
    jvm("backend") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = listOf("-Xjsr305=strict")
            }
        }
    }

    js("frontend", compiler = LEGACY) {
        browser {
            runTask {
                outputFileName = "main.bundle.js"
                sourceMaps = false
                devServer = KotlinWebpackConfig.DevServer(
                    open = false,
                    port = 3000,
                    proxy = mapOf(
                        "/kv/*" to "http://localhost:8080",
                        "/kvws/*" to mapOf("target" to "ws://localhost:8080", "ws" to true)
                    ),
                    contentBase = listOf("$buildDir/processedResources/frontend/main")
                )
            }

            webpackTask {
                outputFileName = "main.bundle.js"
            }

            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.kvision:kvision-server-ktor:$kvisionVersion")
            }

            kotlin.srcDir("build/generated-src/common")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val backendMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))

                implementation("com.github.oss-review-toolkit.ort:analyzer:$ortVersion")
                implementation("com.github.oss-review-toolkit.ort:downloader:$ortVersion")
                implementation("com.github.oss-review-toolkit.ort:evaluator:$ortVersion")
                implementation("com.github.oss-review-toolkit.ort:reporter:$ortVersion")
                implementation("com.github.oss-review-toolkit.ort:scanner:$ortVersion")

                //implementation("com.h2database:h2:$h2Version")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("com.axiomalaska:jdbc-named-parameters:$jdbcNamedParametersVersion")
                implementation("com.github.andrewoma.kwery:core:$kweryVersion")
                implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
                implementation("com.zaxxer:HikariCP:$hikariVersion")
                implementation("commons-codec:commons-codec:$commonsCodecVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-auth:$ktorVersion")
                implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jCoreVersion")
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.postgresql:postgresql:$pgsqlVersion")
            }
        }

        val backendTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val frontendMain by getting {
            resources.srcDir(webDir)

            dependencies {
                implementation("io.kvision:kvision:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-css:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-datetime:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-dialog:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-select:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-spinner:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap-upload:$kvisionVersion")
                implementation("io.kvision:kvision-chart:$kvisionVersion")
                implementation("io.kvision:kvision-datacontainer:$kvisionVersion")
                implementation("io.kvision:kvision-fontawesome:$kvisionVersion")
                implementation("io.kvision:kvision-handlebars:$kvisionVersion")
                implementation("io.kvision:kvision-i18n:$kvisionVersion")
                implementation("io.kvision:kvision-moment:$kvisionVersion")
                implementation("io.kvision:kvision-pace:$kvisionVersion")
                implementation("io.kvision:kvision-redux:$kvisionVersion")
                implementation("io.kvision:kvision-richtext:$kvisionVersion")
                implementation("io.kvision:kvision-routing-navigo-ng:$kvisionVersion")
                implementation("io.kvision:kvision-tabulator:$kvisionVersion")
                implementation("io.kvision:kvision-toast:$kvisionVersion")
            }

            kotlin.srcDir("build/generated-src/frontend")
        }

        val frontendTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("io.kvision:kvision-testutils:$kvisionVersion")
            }
        }
    }
}

fun getNodeJsBinaryExecutable(): String {
    val nodeDir = NodeJsRootPlugin.apply(rootProject).nodeJsSetupTaskProvider.get().destination
    val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
    val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")
    val command = NodeJsRootPlugin.apply(rootProject).nodeCommand
    val finalCommand = if (isWindows && command == "node") "node.exe" else command
    return nodeBinDir.resolve(finalCommand).absolutePath
}

tasks {
    create("generatePotFile", Exec::class) {
        dependsOn("compileKotlinFrontend")
        executable = getNodeJsBinaryExecutable()
        args("${rootProject.buildDir}/js/node_modules/gettext-extract/bin/gettext-extract")
        inputs.files(kotlin.sourceSets["frontendMain"].kotlin.files)
        outputs.file("$projectDir/src/frontendMain/resources/i18n/messages.pot")
    }
}

afterEvaluate {
    tasks {
        getByName("frontendProcessResources", Copy::class) {
            dependsOn("compileKotlinFrontend")
            exclude("**/*.pot")
            doLast("Convert PO to JSON") {
                destinationDir.walkTopDown().filter {
                    it.isFile && it.extension == "po"
                }.forEach {
                    exec {
                        executable = getNodeJsBinaryExecutable()
                        args(
                            "${rootProject.buildDir}/js/node_modules/gettext.js/bin/po2json",
                            it.absolutePath,
                            "${it.parent}/${it.nameWithoutExtension}.json"
                        )
                        println("Converted ${it.name} to ${it.nameWithoutExtension}.json")
                    }
                    it.delete()
                }
            }
        }

        create("frontendArchive", Jar::class).apply {
            dependsOn("frontendBrowserProductionWebpack")
            group = "package"
            archiveAppendix.set("frontend")
            val distribution =
                project.tasks.getByName("frontendBrowserProductionWebpack", KotlinWebpack::class).destinationDirectory!!
            from(distribution) {
                include("*.*")
            }
            from(webDir)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            into("/assets")
            inputs.files(distribution, webDir)
            outputs.file(archiveFile)
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Group" to rootProject.group,
                        "Implementation-Version" to rootProject.version,
                        "Timestamp" to System.currentTimeMillis()
                    )
                )
            }
        }

        getByName("backendProcessResources", Copy::class) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        getByName("backendJar").group = "package"

        create("jar", Jar::class).apply {
            dependsOn("frontendArchive", "backendJar")
            group = "package"
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Group" to rootProject.group,
                        "Implementation-Version" to rootProject.version,
                        "Timestamp" to System.currentTimeMillis(),
                        "Main-Class" to mainClassName
                    )
                )
            }
            val dependencies = configurations["backendRuntimeClasspath"].filter { it.name.endsWith(".jar") } +
                    project.tasks["backendJar"].outputs.files +
                    project.tasks["frontendArchive"].outputs.files
            dependencies.forEach {
                if (it.isDirectory) from(it) else from(zipTree(it))
            }
            exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
            inputs.files(dependencies)
            outputs.file(archiveFile)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            isZip64 = true
        }

        create("backendRun", JavaExec::class) {
            dependsOn("compileKotlinBackend")
            group = "run"
            main = mainClassName
            classpath =
                configurations["backendRuntimeClasspath"] + project.tasks["compileKotlinBackend"].outputs.files +
                        project.tasks["backendProcessResources"].outputs.files
            workingDir = buildDir
        }

        getByName("compileKotlinBackend") {
            dependsOn("compileKotlinMetadata")
        }

        getByName("compileKotlinFrontend") {
            dependsOn("compileKotlinMetadata")
        }
    }
}
