plugins {
    id("java")
}

group = "org.checkout"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("io.javalin:javalin:6.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")



    // Test
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}