# NSLAPI File Generation

This  Micronaut app to build various files format delivered by nslapi endpoints. So far this only builds skos files.

## Java

You need `Java 11` and `Groovy 3.0.9` to run this application

```bash
 mo > ../IdeaProjects/nslapi > java -version
openjdk version "11.0.15" 2022-04-19 LTS
OpenJDK Runtime Environment Zulu11.56+19-CA (build 11.0.15+10-LTS)
OpenJDK 64-Bit Server VM Zulu11.56+19-CA (build 11.0.15+10-LTS, mixed mode)

 mo > ../IdeaProjects/nslapi > groovy --version
Groovy Version: 3.0.9 JVM: 11.0.15 Vendor: Azul Systems, Inc. OS: Linux
```

## Build

This app uses gradle and can be built using the command `./gradlew build`

## Run

To run the app, use `./gradlew run`

## Prerequisites

1. The app expects a graphql endpoint as provided by the config file provided using an environment variable
2. Sample config is [here](https://raw.githubusercontent.com/bio-org-au/nslapi-file-generation/main/src/test/resources/nsl-api-config.groovy)
3. Export following env variables before you build or run the app

```bash
MICRONAUT_CONFIG_FILES=/home/mo/.nsl/nsl-api-config.groovy
MICRONAUT_SERVER_PORT=8080
```

# Technical documentation

## Micronaut 3.5.2 Documentation

- [User Guide](https://docs.micronaut.io/3.5.2/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.5.2/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.5.2/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)

---

- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)

## Feature tomcat-server documentation

- [Micronaut Tomcat Server documentation](https://micronaut-projects.github.io/micronaut-servlet/1.0.x/guide/index.html#tomcat)

## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)


