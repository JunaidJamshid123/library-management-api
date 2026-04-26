# 🔧 BUILD TOOLS — Maven & Gradle Quick Guide

> Concise reference covering Maven, Gradle, and their comparison.

---

## Table of Contents

1. [What are Build Tools?](#1-what-are-build-tools)
2. [Maven](#2-maven)
3. [Gradle](#3-gradle)
4. [Maven vs Gradle](#4-maven-vs-gradle)

---

## 1. What are Build Tools?

Build tools **automate** the process of compiling code, managing dependencies, running tests, and packaging your application into a deployable artifact (JAR/WAR).

```
┌──────────────────────────────────────────────────────────┐
│              WITHOUT BUILD TOOLS                         │
│                                                          │
│  You manually:                                           │
│  1. Download 50+ JAR files from the internet             │
│  2. Add them to classpath                                │
│  3. Run javac to compile                                 │
│  4. Run tests manually                                   │
│  5. Package into JAR/WAR yourself                        │
│  6. Pray all versions are compatible 🙏                  │
│                                                          │
│              WITH BUILD TOOLS (Maven/Gradle)             │
│                                                          │
│  You write ONE config file → tool does everything:       │
│  ✅ Downloads dependencies automatically                 │
│  ✅ Manages version compatibility                        │
│  ✅ Compiles source code                                 │
│  ✅ Runs tests                                           │
│  ✅ Packages into JAR/WAR                                │
│  ✅ Deploys to repository/server                         │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Maven

### What is Maven?

**Apache Maven** is a build tool that uses an **XML file (`pom.xml`)** to define project configuration, dependencies, and build instructions. It follows the **"Convention over Configuration"** principle.

### pom.xml — Project Object Model

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- ====== PROJECT IDENTITY ====== -->
    <groupId>com.library</groupId>          <!-- Organization/company -->
    <artifactId>library-management</artifactId> <!-- Project name -->
    <version>1.0.0-SNAPSHOT</version>       <!-- Version -->
    <packaging>jar</packaging>              <!-- jar (default) or war -->

    <!-- ====== PARENT (Spring Boot) ====== -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <!-- ====== PROPERTIES ====== -->
    <properties>
        <java.version>17</java.version>
    </properties>

    <!-- ====== DEPENDENCIES ====== -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- Version managed by parent -->
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- ====== BUILD PLUGINS ====== -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Dependency Scopes

| Scope      | Available At          | Included in JAR? | Use Case                          |
| ---------- | --------------------- | ----------------- | --------------------------------- |
| `compile`  | Compile + Runtime     | Yes               | Default. Most dependencies.       |
| `runtime`  | Runtime only          | Yes               | JDBC drivers, implementations     |
| `test`     | Test only             | No                | JUnit, Mockito                    |
| `provided` | Compile only          | No                | Servlet API (server provides it)  |
| `system`   | Local JAR (not Maven) | No                | Rare. Local file system JARs.     |

### Maven Standard Directory Structure

```
project-root/
├── pom.xml                          ← Build configuration
├── src/
│   ├── main/
│   │   ├── java/                    ← Application source code
│   │   │   └── com/library/...
│   │   └── resources/               ← Config files (application.properties)
│   │       ├── static/              ← Static assets (CSS, JS)
│   │       └── templates/           ← HTML templates
│   └── test/
│       ├── java/                    ← Test source code
│       └── resources/               ← Test config files
└── target/                          ← Build output (generated)
    ├── classes/                      ← Compiled .class files
    └── library-management-1.0.0.jar ← Packaged artifact
```

### Maven Build Lifecycle

Maven has **3 built-in lifecycles**, each with phases that run **sequentially**:

```
┌──────────────────────────────────────────────────────────────────┐
│              DEFAULT LIFECYCLE (most important)                   │
│                                                                  │
│   mvn validate    → Check project is correct                     │
│        │                                                         │
│        ▼                                                         │
│   mvn compile     → Compile source code (src/main/java)          │
│        │                                                         │
│        ▼                                                         │
│   mvn test        → Run unit tests (src/test/java)               │
│        │                                                         │
│        ▼                                                         │
│   mvn package     → Package into JAR/WAR (target/)               │
│        │                                                         │
│        ▼                                                         │
│   mvn verify      → Run integration tests                        │
│        │                                                         │
│        ▼                                                         │
│   mvn install     → Install JAR to local repo (~/.m2/)           │
│        │                                                         │
│        ▼                                                         │
│   mvn deploy      → Upload to remote repo (Nexus/Artifactory)   │
│                                                                  │
│   ⚠️ Each phase runs ALL phases before it!                       │
│   "mvn package" = validate + compile + test + package            │
│                                                                  │
│              CLEAN LIFECYCLE                                      │
│   mvn clean       → Deletes target/ folder                       │
│                                                                  │
│              SITE LIFECYCLE                                       │
│   mvn site        → Generates project documentation              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Common Maven Commands

```bash
mvn clean                  # Delete target/ folder
mvn compile                # Compile source code
mvn test                   # Run unit tests
mvn package                # Compile + test + create JAR/WAR
mvn clean package          # Clean first, then package (MOST USED)
mvn clean install          # Clean + package + install to local repo
mvn clean package -DskipTests  # Package without running tests

mvn dependency:tree        # Show dependency tree (debug conflicts)
mvn dependency:resolve     # List all resolved dependencies
mvn spring-boot:run        # Run Spring Boot app directly
mvn versions:display-dependency-updates  # Check for newer versions
```

### Maven Wrapper (mvnw)

The `mvnw` / `mvnw.cmd` files let you run Maven **without installing it globally**:

```bash
./mvnw clean package       # Linux/Mac
mvnw.cmd clean package     # Windows
```

### How Dependencies Are Resolved

```
┌──────────────────────────────────────────────────────────────────┐
│          MAVEN DEPENDENCY RESOLUTION                             │
│                                                                  │
│   pom.xml says: "I need spring-boot-starter-web"                 │
│        │                                                         │
│        ▼                                                         │
│   1. Check LOCAL repo (~/.m2/repository/)                        │
│      Found? → Use it                                             │
│      Not found? ↓                                                │
│                                                                  │
│   2. Check REMOTE repo (Maven Central / custom Nexus)            │
│      Download JAR + POM → save to local repo                     │
│        │                                                         │
│        ▼                                                         │
│   3. Read downloaded POM for TRANSITIVE dependencies             │
│      spring-boot-starter-web needs:                              │
│      ├── spring-webmvc (which needs spring-core, spring-beans)   │
│      ├── jackson-databind (which needs jackson-core)             │
│      └── tomcat-embed-core                                       │
│        │                                                         │
│        ▼                                                         │
│   4. Download ALL transitive dependencies recursively            │
│      → Everything lands in ~/.m2/repository/                     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Gradle

### What is Gradle?

**Gradle** is a modern build tool that uses **Groovy or Kotlin DSL** instead of XML. It's **faster** than Maven due to incremental builds and build caching.

### build.gradle (Groovy DSL)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.library'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()    // Where to download dependencies from
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### build.gradle.kts (Kotlin DSL)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.library"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Gradle Dependency Configurations (Scopes)

| Gradle Config        | Maven Equivalent | Description                        |
| -------------------- | ---------------- | ---------------------------------- |
| `implementation`     | `compile`        | Compile + runtime, not transitive  |
| `api`                | `compile`        | Compile + runtime + transitive     |
| `runtimeOnly`        | `runtime`        | Runtime only                       |
| `testImplementation` | `test`           | Test compile + runtime             |
| `compileOnly`        | `provided`       | Compile only, not in artifact      |
| `annotationProcessor`| —                | Annotation processing (Lombok etc.)|

### Common Gradle Commands

```bash
gradle build               # Compile + test + package
gradle clean               # Delete build/ folder
gradle test                # Run tests
gradle bootRun             # Run Spring Boot app
gradle clean build         # Clean + full build
gradle build -x test       # Build without tests
gradle dependencies        # Show dependency tree

# Gradle Wrapper (recommended)
./gradlew build            # Linux/Mac
gradlew.bat build          # Windows
```

### Gradle Build Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│              GRADLE BUILD PHASES                                 │
│                                                                  │
│   1. INITIALIZATION                                              │
│      → Read settings.gradle, determine which projects to build   │
│                                                                  │
│   2. CONFIGURATION                                               │
│      → Execute build.gradle, create task graph                   │
│                                                                  │
│   3. EXECUTION                                                   │
│      → Run only the requested tasks (and their dependencies)     │
│                                                                  │
│   "gradle build" executes:                                       │
│   compileJava → processResources → classes →                     │
│   compileTestJava → processTestResources → testClasses →         │
│   test → jar → assemble → check → build                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Maven vs Gradle

### Quick Comparison

```
┌──────────────────────────────────────────────────────────────────┐
│                   MAVEN vs GRADLE                                │
│                                                                  │
│   MAVEN                          GRADLE                          │
│   ┌──────────────────┐           ┌──────────────────┐            │
│   │  pom.xml (XML)   │           │  build.gradle    │            │
│   │  Verbose          │           │  (Groovy/Kotlin) │            │
│   │  Declarative     │           │  Concise          │            │
│   │  Convention-based│           │  Flexible/Script  │            │
│   │  Slower builds   │           │  Faster builds    │            │
│   │  Stable/Mature   │           │  Modern/Evolving  │            │
│   └──────────────────┘           └──────────────────┘            │
│                                                                  │
│   Maven = Reliable Toyota         Gradle = Fast Tesla             │
└──────────────────────────────────────────────────────────────────┘
```

### Detailed Comparison

| Feature                | Maven                              | Gradle                              |
| ---------------------- | ---------------------------------- | ----------------------------------- |
| **Config file**        | `pom.xml` (XML)                    | `build.gradle` (Groovy/Kotlin)      |
| **Language**           | XML (verbose)                      | Groovy/Kotlin DSL (concise)         |
| **Build speed**        | Slower (no caching)                | Faster (incremental + cache)        |
| **Flexibility**        | Rigid (convention-based)           | Highly flexible (scriptable)        |
| **Learning curve**     | Easier for beginners               | Steeper (scripting knowledge)       |
| **Dependency mgmt**    | Via parent POM                     | Via dependency-management plugin    |
| **Multi-module**       | Supported                          | Better support                      |
| **IDE support**        | Excellent                          | Excellent                           |
| **Build output**       | `target/`                          | `build/`                            |
| **Wrapper**            | `mvnw`                             | `gradlew`                           |
| **Spring Boot default**| ✅ Supported (common)              | ✅ Supported (common)               |
| **Android default**    | ❌ Not used                        | ✅ Official build tool              |
| **Community**          | Larger (older)                     | Growing fast                        |
| **Plugin ecosystem**   | Huge                               | Large + custom tasks easy           |

### Same Dependency — Maven vs Gradle

```xml
<!-- MAVEN (pom.xml) — 5 lines -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```groovy
// GRADLE (build.gradle) — 1 line
implementation 'org.springframework.boot:spring-boot-starter-web'
```

### Build Speed — Why Gradle is Faster

```
┌──────────────────────────────────────────────────────────────────┐
│           WHY GRADLE IS FASTER                                   │
│                                                                  │
│   1. INCREMENTAL BUILDS                                          │
│      Only recompiles files that CHANGED                          │
│      Maven recompiles EVERYTHING every time                      │
│                                                                  │
│   2. BUILD CACHE                                                 │
│      Caches task outputs. If input didn't change → skip task     │
│      Maven has no built-in cache                                 │
│                                                                  │
│   3. DAEMON PROCESS                                              │
│      Gradle runs a background daemon (JVM stays warm)            │
│      Maven starts a new JVM every time                           │
│                                                                  │
│   4. PARALLEL EXECUTION                                          │
│      Gradle runs independent tasks in parallel by default        │
│      Maven is mostly sequential                                  │
│                                                                  │
│   Result: Gradle is 2-10x faster for large projects              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### When to Use Which?

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   USE MAVEN when:                                                │
│   • You're a beginner (simpler to learn)                         │
│   • Team is already using Maven                                  │
│   • Enterprise/corporate projects (standard)                     │
│   • Simple project structure                                     │
│   • You prefer convention over configuration                     │
│                                                                  │
│   USE GRADLE when:                                               │
│   • Build speed matters (large projects)                         │
│   • Android development (required)                               │
│   • You need custom build logic                                  │
│   • Multi-module complex projects                                │
│   • You prefer concise build scripts                             │
│                                                                  │
│   BOTTOM LINE:                                                   │
│   Both work perfectly with Spring Boot.                          │
│   Maven is more common in Spring ecosystem.                      │
│   Choose what your team uses.                                    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Command Equivalents

| Task                   | Maven                          | Gradle                       |
| ---------------------- | ------------------------------ | ---------------------------- |
| Clean                  | `mvn clean`                    | `gradle clean`               |
| Compile                | `mvn compile`                  | `gradle compileJava`         |
| Test                   | `mvn test`                     | `gradle test`                |
| Package                | `mvn package`                  | `gradle build`               |
| Skip tests             | `mvn package -DskipTests`      | `gradle build -x test`       |
| Run Spring Boot        | `mvn spring-boot:run`          | `gradle bootRun`             |
| Dependency tree        | `mvn dependency:tree`          | `gradle dependencies`        |
| Clean + Build          | `mvn clean package`            | `gradle clean build`         |
| Install to local repo  | `mvn install`                  | `gradle publishToMavenLocal` |

---

> **Author:** Build Tools Study Notes  
> **Covers:** Maven 3.x / Gradle 8.x / Spring Boot 3.x
