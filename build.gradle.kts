import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val kotlinx_serialization_version: String by project
val maria_db_version: String by project
val arrow_kt_version: String by project
val jakarta_mail_version: String by project
val angus_mail_version: String by project
val kotlinx_coroutines_version: String by project
val swagger_codegen_version: String by project

plugins {
    application
    java
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.11"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(21)
}

group = "net.mt32.expoll"
version = "4.2.4"

application {
    mainClass.set("net.mt32.expoll.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.withType<Test> {
    useJUnitPlatform {
        ignoreFailures = true
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

ktor {
    fatJar {
        archiveFileName.set("expoll.jar")
    }
}

tasks.withType<ShadowJar>{
    mergeGroovyExtensionModules()
}

tasks.register("openapi", type = Exec::class){
    workingDir = projectDir
    executable = "bash"
    commandLine("./scripts/openAPIBundle.sh", "src/main/resources/openapi/openapi_v4.yaml", "src/main/resources/openapi/openapi.yaml")
    /*
    commandLine(
        "./scripts/openAPIBundle.sh", "src/main/resources/openapi/openapi_v4.yaml",
        layout.buildDirectory.file("/resources/main/openapi/openapi_v4.yaml").get().asFile.absolutePath
    )
     */
}

tasks.compileKotlin.configure{
    dependsOn("openapi")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-forwarded-header:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-protobuf:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    //implementation("io.swagger.codegen.v3:swagger-codegen-generators:$swagger_codegen_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.mariadb.jdbc:mariadb-java-client:$maria_db_version")
    implementation("io.ktor:ktor-server-double-receive:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-jetty:$ktor_version")
    implementation("io.ktor:ktor-client-java:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-rate-limit:$ktor_version")
    implementation("io.arrow-kt:arrow-core:$arrow_kt_version")
    implementation("io.github.nefilim.kjwt:kjwt-core:0.8.0")
    implementation("jakarta.mail:jakarta.mail-api:$jakarta_mail_version")
    implementation("org.eclipse.angus:angus-mail:$angus_mail_version")
    //implementation("com.webauthn4j:webauthn4j-core:0.21.0.RELEASE")
    implementation("com.yubico:webauthn-server-core:2.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation(kotlin("test-junit5"))
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    // https://mvnrepository.com/artifact/org.thymeleaf/thymeleaf
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    // https://mvnrepository.com/artifact/nz.net.ultraq.thymeleaf/thymeleaf-layout-dialect
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
    // https://mvnrepository.com/artifact/joda-time/joda-time
    implementation("joda-time:joda-time:2.12.7")



    //testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
}