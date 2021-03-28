![OSS Review Toolkit Logo](./logos/ort.png)

# ORT Web

This is an **experimental** backend and web frontend for the [OSS Review Toolkit (ORT)](https://oss-review-toolkit.org).
At the current state, do not use this for running ORT in production and expect bugs, breaking changes and force pushes
to the repository.

ORT web is a Kotlin multiplatform project, meaning the server backend and web frontend are written in pure Kotlin. It
uses [KVision](https://kvision.io/) and is based on the
[template-fullstack-ktor example](https://github.com/rjaros/kvision-examples/tree/81c14441eca8c14b34e10b03951ebef5fd7a0b9e/template-fullstack-ktor). 

## Gradle Tasks

This is an overview of the most important Gradle tasks.

### Resource Processing
* generatePotFile - Generates a `src/frontendMain/resources/i18n/messages.pot` translation template file.
### Compiling
* compileKotlinFrontend - Compiles frontend sources.
* compileKotlinBackend - Compiles backend sources.
### Running
* frontendRun - Starts a webpack dev server on port 3000
* backendRun - Starts a dev server on port 8080
### Packaging
* frontendBrowserWebpack - Bundles the compiled js files into `build/distributions`
* frontendJar - Packages a standalone "web" frontend jar with all required files into `build/libs/*.jar`
* backendJar - Packages a backend jar with compiled source files into `build/libs/*.jar`
* jar - Packages a "fat" jar with all backend sources and dependencies while also embedding frontend resources into `build/libs/*.jar`

## Continuous Build

During development run the following three tasks in continuous mode (-t), this will rebuild the code after each change
and automatically reload the backend and refresh the frontend in the browser:

```shell
./gradlew -t backendRun
./gradlew -t compileKotlinBackend
./gradlew -t frontendRun
```

# License

See the [LICENSE](./LICENSE) file in the root of this project for license details.
