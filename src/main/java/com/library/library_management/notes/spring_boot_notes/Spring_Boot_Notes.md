# 🚀 SPRING BOOT — Complete In-Depth Guide

> **Spring Boot Version:** 3.x | **Java Version:** 17+  
> Covers theory, real-world analogies, diagrams, code, common mistakes, and interview Q&A.

---

## Table of Contents

1. [Spring Boot vs Spring Framework](#1-spring-boot-vs-spring-framework)
2. [Auto-Configuration Magic](#2-auto-configuration-magic)
3. [spring-boot-starter Dependencies](#3-spring-boot-starter-dependencies)
4. [application.properties / application.yml](#4-applicationproperties--applicationyml)
5. [@SpringBootApplication](#5-springbootapplication)
6. [Embedded Server (Tomcat)](#6-embedded-server-tomcat)
7. [Common Mistakes & Troubleshooting](#7-common-mistakes--troubleshooting)
8. [Interview Questions & Answers](#8-interview-questions--answers)

---

## 1. Spring Boot vs Spring Framework

### What is Spring Framework?

**Spring Framework** is a comprehensive Java framework that provides infrastructure support for building enterprise applications. It gives you IoC, DI, AOP, MVC, Data Access, Security, etc.

**BUT** — setting up a Spring application requires **massive manual configuration**:

```xml
<!-- web.xml — You had to write this MANUALLY -->
<web-app>
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring-config.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

```xml
<!-- spring-config.xml — More manual config -->
<beans>
    <context:component-scan base-package="com.library"/>
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/library"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>
    <!-- ... 50+ more lines of XML ... -->
</beans>
```

### What is Spring Boot?

**Spring Boot** is a **layer on top of Spring Framework** that eliminates boilerplate configuration. It is NOT a replacement — it makes Spring **easier to use**.

**The same app in Spring Boot:**

```java
@SpringBootApplication
public class LibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
```

```properties
# application.properties — that's it!
spring.datasource.url=jdbc:mysql://localhost:3306/library
spring.datasource.username=root
spring.datasource.password=password
```

**That's it.** No XML. No web.xml. No manual bean wiring. Spring Boot auto-configures everything.

### 💡 Real-World Analogy

- **Spring Framework** = Building a house from scratch — you buy bricks, cement, hire workers, lay the foundation, plumbing, electrical wiring, everything manually.
- **Spring Boot** = Moving into a **fully furnished apartment** — electricity, plumbing, furniture, internet all pre-configured. You just move in and start living.

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   SPRING FRAMEWORK (Manual Setup)                                │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  YOU must configure:                                     │   │
│   │  • web.xml                                               │   │
│   │  • DispatcherServlet                                     │   │
│   │  • ViewResolver                                          │   │
│   │  • DataSource                                            │   │
│   │  • Transaction Manager                                   │   │
│   │  • Component Scanning                                    │   │
│   │  • External Tomcat Server                                │   │
│   │  • Logging                                               │   │
│   │  • Error Handling                                        │   │
│   │  • ... 100+ lines of XML/Java config                     │   │
│   └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│   SPRING BOOT (Auto-Configured)                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  Spring Boot auto-configures:                            │   │
│   │  ✅ DispatcherServlet                                    │   │
│   │  ✅ ViewResolver                                         │   │
│   │  ✅ DataSource (if DB driver in classpath)               │   │
│   │  ✅ Transaction Manager                                  │   │
│   │  ✅ Component Scanning                                   │   │
│   │  ✅ Embedded Tomcat Server                               │   │
│   │  ✅ Logging (SLF4J + Logback)                            │   │
│   │  ✅ Error Handling (/error page)                         │   │
│   │  ✅ ... 200+ auto-configurations                         │   │
│   │                                                          │   │
│   │  YOU only write:                                         │   │
│   │  • @SpringBootApplication                                │   │
│   │  • application.properties (optional overrides)           │   │
│   │  • Your business code                                    │   │
│   └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### Detailed Comparison Table

| Feature                    | Spring Framework                          | Spring Boot                                |
| -------------------------- | ----------------------------------------- | ------------------------------------------ |
| Configuration              | Manual (XML or Java Config)               | Auto-configuration                         |
| Setup Time                 | Hours/Days                                | Minutes                                    |
| Server                     | External (install Tomcat separately)      | Embedded (Tomcat included)                 |
| Dependency Management      | Manual — pick each JAR version            | Starters — curated dependency bundles      |
| web.xml needed?            | Yes                                       | No                                         |
| Deployment                 | WAR file → deploy to server               | JAR file → `java -jar app.jar`             |
| Production-ready features  | Manual setup                              | Actuator (health, metrics, etc.)           |
| Opinionated?               | No — YOU decide everything                | Yes — sensible defaults, override if needed|
| Learning Curve             | Steep                                     | Gentle                                     |
| Boilerplate Code           | A LOT                                     | Almost NONE                                |

### What Spring Boot is NOT

```
┌──────────────────────────────────────────────────────────────────┐
│                 WHAT SPRING BOOT IS NOT                           │
│                                                                  │
│   ❌ NOT a replacement for Spring Framework                      │
│      → It USES Spring Framework underneath                       │
│                                                                  │
│   ❌ NOT a code generator                                        │
│      → It doesn't generate code, it auto-configures beans        │
│                                                                  │
│   ❌ NOT only for web applications                               │
│      → Works for CLI apps, batch jobs, microservices, etc.       │
│                                                                  │
│   ❌ NOT only for microservices                                  │
│      → Works for monoliths too                                   │
│                                                                  │
│   ✅ IS an opinionated layer over Spring Framework               │
│   ✅ IS a rapid application development tool                     │
│   ✅ IS the standard way to build Spring apps today              │
└──────────────────────────────────────────────────────────────────┘
```

### The Relationship

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                    ┌───────────────────┐                          │
│                    │  YOUR APPLICATION │                          │
│                    └────────┬──────────┘                          │
│                             │ uses                               │
│                    ┌────────▼──────────┐                          │
│                    │   SPRING BOOT     │  ← Simplifies setup     │
│                    │   (Auto-config,   │     Embedded server      │
│                    │    Starters,      │     Starters             │
│                    │    Actuator)      │                          │
│                    └────────┬──────────┘                          │
│                             │ built on top of                    │
│                    ┌────────▼──────────┐                          │
│                    │ SPRING FRAMEWORK  │  ← Core engine           │
│                    │   (IoC, DI, AOP,  │     (DI, MVC, Data,     │
│                    │    MVC, Data,     │      Security, etc.)     │
│                    │    Security)      │                          │
│                    └────────┬──────────┘                          │
│                             │ runs on                            │
│                    ┌────────▼──────────┐                          │
│                    │       JAVA        │  ← JDK 17+              │
│                    └──────────────────-┘                          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Auto-Configuration Magic

### What is Auto-Configuration?

**Auto-Configuration** is Spring Boot's ability to **automatically configure beans based on what JARs/dependencies are on your classpath**. You don't tell Spring what to configure — it **detects** and **configures** automatically.

### 💡 Real-World Analogy

Auto-configuration is like a **smart home system**:
- You plug in a new TV → Smart home automatically detects it, connects WiFi, sets up the remote, and configures optimal settings.
- You plug in a speaker → Same thing — auto-detected and configured.
- You don't write a manual setup for each device.

Similarly:
- You add `mysql-connector-java` to your project → Spring Boot auto-configures a `DataSource`
- You add `spring-boot-starter-web` → Spring Boot auto-configures `DispatcherServlet`, `Tomcat`, `Jackson`, etc.

### How Auto-Configuration Works — Step by Step

```
┌──────────────────────────────────────────────────────────────────┐
│           AUTO-CONFIGURATION — INTERNAL FLOW                     │
│                                                                  │
│  Step 1: Application starts                                      │
│  ┌───────────────────────────────────────────┐                   │
│  │  @SpringBootApplication                   │                   │
│  │  └── includes @EnableAutoConfiguration    │                   │
│  └──────────────────┬────────────────────────┘                   │
│                     ▼                                            │
│  Step 2: Spring reads META-INF/spring/                           │
│          org.springframework.boot.autoconfigure.AutoConfiguration│
│          .imports file                                           │
│  ┌───────────────────────────────────────────┐                   │
│  │  Lists 200+ auto-configuration classes:   │                   │
│  │  • DataSourceAutoConfiguration            │                   │
│  │  • WebMvcAutoConfiguration                │                   │
│  │  • JpaRepositoriesAutoConfiguration       │                   │
│  │  • SecurityAutoConfiguration              │                   │
│  │  • ... many more                          │                   │
│  └──────────────────┬────────────────────────┘                   │
│                     ▼                                            │
│  Step 3: For EACH auto-config class, check CONDITIONS            │
│  ┌───────────────────────────────────────────┐                   │
│  │  @ConditionalOnClass → Is the class       │                   │
│  │                        on classpath?       │                   │
│  │  @ConditionalOnMissingBean → Did user     │                   │
│  │                              already       │                   │
│  │                              define one?   │                   │
│  │  @ConditionalOnProperty → Is the property │                   │
│  │                           enabled?         │                   │
│  └──────────────────┬────────────────────────┘                   │
│                     ▼                                            │
│  Step 4: If ALL conditions pass → CREATE the beans               │
│          If ANY condition fails → SKIP                           │
│  ┌───────────────────────────────────────────┐                   │
│  │  DataSource? MySQL driver found ✅         │                   │
│  │             → Auto-create HikariDataSource│                   │
│  │                                           │                   │
│  │  Redis? No Redis dependency ❌             │                   │
│  │       → Skip RedisAutoConfiguration       │                   │
│  └───────────────────────────────────────────┘                   │
└──────────────────────────────────────────────────────────────────┘
```

### Key Conditional Annotations Used in Auto-Configuration

| Annotation                         | Meaning                                                          |
| ---------------------------------- | ---------------------------------------------------------------- |
| `@ConditionalOnClass`              | Only configure if this class exists on classpath                 |
| `@ConditionalOnMissingClass`       | Only configure if this class does NOT exist                      |
| `@ConditionalOnBean`               | Only configure if this bean already exists in container          |
| `@ConditionalOnMissingBean`        | Only configure if user has NOT defined this bean                 |
| `@ConditionalOnProperty`           | Only configure if a property has a specific value                |
| `@ConditionalOnWebApplication`     | Only configure if this is a web application                      |
| `@ConditionalOnNotWebApplication`  | Only configure if this is NOT a web application                  |

### Example: How DataSource Auto-Configuration Works Internally

This is a simplified version of what Spring Boot does behind the scenes:

```java
// This is inside spring-boot-autoconfigure JAR
@Configuration
@ConditionalOnClass(DataSource.class)           // Only if DataSource class exists
@ConditionalOnProperty(                          // Only if these properties are set
    name = "spring.datasource.url"
)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean  // Only if user hasn't defined their own DataSource
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        return ds;
    }
}
```

**What this means:**
- If you have a MySQL driver on the classpath AND `spring.datasource.url` in properties → Spring Boot creates a `DataSource` for you.
- If you define your **own** `@Bean DataSource` → Spring Boot **backs off** and uses yours instead (`@ConditionalOnMissingBean`).

### 🔑 The Golden Rule of Auto-Configuration

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   🔑 AUTO-CONFIGURATION GOLDEN RULE:                             │
│                                                                  │
│   "Your explicit configuration ALWAYS wins                       │
│    over auto-configuration."                                     │
│                                                                  │
│   If you define a @Bean → Spring Boot's auto-config backs off.   │
│   You are NEVER locked in. You can ALWAYS override.              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### How to See What Was Auto-Configured

**Method 1: Debug mode in application.properties**

```properties
# Shows a full report of what was auto-configured and what wasn't
debug=true
```

Output shows:
```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:    (Beans that WERE auto-configured)
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'

Negative matches:    (Beans that were SKIPPED)
-----------------
   RedisAutoConfiguration:
      - @ConditionalOnClass did not find required class 'org.redis.RedisClient'
```

**Method 2: Actuator endpoint**

```properties
management.endpoints.web.exposure.include=conditions
```
Visit: `http://localhost:8080/actuator/conditions`

### Override Auto-Configuration Example

```java
// Spring Boot auto-creates a DataSource, but you want a CUSTOM one:
@Configuration
public class MyDataSourceConfig {

    @Bean   // Your bean → auto-configuration backs off
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://custom-host:3306/mydb");
        ds.setMaximumPoolSize(20);  // Custom pool size
        ds.setConnectionTimeout(5000);
        return ds;
    }
}
```

### Exclude Specific Auto-Configurations

```java
// Don't auto-configure DataSource at all
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class LibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
```

Or in `application.properties`:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 3. spring-boot-starter Dependencies

### What are Starters?

**Starters** are **curated sets of dependencies** bundled together for a specific purpose. Instead of manually finding and adding 10-15 individual JARs, you add **one starter** and get everything you need.

### 💡 Real-World Analogy

Starters are like **meal combos at a restaurant**:
- **Without starters (à la carte):** "I want chicken, rice, sauce, salad, bread, drink, dessert" — you pick each item manually.
- **With starters (combo meal):** "I want Combo #3" — you get everything in one order.

```
┌──────────────────────────────────────────────────────────────────┐
│           WITHOUT STARTERS (Manual Dependencies)                 │
│                                                                  │
│   <dependency>spring-web</dependency>                            │
│   <dependency>spring-webmvc</dependency>                         │
│   <dependency>jackson-databind</dependency>                      │
│   <dependency>jackson-core</dependency>                          │
│   <dependency>jackson-annotations</dependency>                   │
│   <dependency>tomcat-embed-core</dependency>                     │
│   <dependency>tomcat-embed-el</dependency>                       │
│   <dependency>tomcat-embed-websocket</dependency>                │
│   <dependency>hibernate-validator</dependency>                   │
│   <dependency>slf4j-api</dependency>                             │
│   <dependency>logback-classic</dependency>                       │
│   ... and worry about version compatibility!                     │
│                                                                  │
│           WITH STARTERS (One Dependency)                          │
│                                                                  │
│   <dependency>                                                   │
│       spring-boot-starter-web     ← THIS ONE LINE               │
│   </dependency>                    includes ALL of the above!    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### How Starters Work

```
┌──────────────────────────────────────────────────────────────────┐
│            HOW STARTERS WORK                                     │
│                                                                  │
│   spring-boot-starter-web                                        │
│   ┌────────────────────────────────────────────┐                 │
│   │                                            │                 │
│   │   ┌──────────────────────┐                 │                 │
│   │   │ spring-boot-starter  │ (base starter)  │                 │
│   │   │  ├── spring-core     │                 │                 │
│   │   │  ├── spring-context  │                 │                 │
│   │   │  ├── spring-boot     │                 │                 │
│   │   │  ├── spring-boot-    │                 │                 │
│   │   │  │   autoconfigure   │                 │                 │
│   │   │  ├── logback         │                 │                 │
│   │   │  └── slf4j           │                 │                 │
│   │   └──────────────────────┘                 │                 │
│   │                                            │                 │
│   │   ┌──────────────────────┐                 │                 │
│   │   │ spring-web           │                 │                 │
│   │   │ spring-webmvc        │                 │                 │
│   │   └──────────────────────┘                 │                 │
│   │                                            │                 │
│   │   ┌──────────────────────┐                 │                 │
│   │   │ jackson-databind     │ (JSON)          │                 │
│   │   └──────────────────────┘                 │                 │
│   │                                            │                 │
│   │   ┌──────────────────────┐                 │                 │
│   │   │ tomcat-embed-core    │ (Server)        │                 │
│   │   └──────────────────────┘                 │                 │
│   │                                            │                 │
│   └────────────────────────────────────────────┘                 │
│                                                                  │
│   You add 1 dependency → Maven/Gradle pulls 30+ JARs            │
│   with COMPATIBLE versions (no conflicts!)                       │
└──────────────────────────────────────────────────────────────────┘
```

### Most Common Starters

| Starter                              | What It Includes                                              | Use Case                        |
| ------------------------------------ | ------------------------------------------------------------- | ------------------------------- |
| `spring-boot-starter`               | Core (IoC, DI, logging, YAML)                                 | Base for all apps               |
| `spring-boot-starter-web`           | Tomcat, Spring MVC, Jackson, Validator                        | REST APIs, Web apps             |
| `spring-boot-starter-data-jpa`      | Hibernate, JPA, HikariCP, Spring Data JPA                     | Database with ORM               |
| `spring-boot-starter-data-jdbc`     | JDBC, HikariCP, Spring Data JDBC                              | Database without ORM            |
| `spring-boot-starter-security`      | Spring Security, authentication, authorization                | Securing your app               |
| `spring-boot-starter-test`          | JUnit 5, Mockito, AssertJ, Spring Test                        | Unit & integration testing      |
| `spring-boot-starter-validation`    | Hibernate Validator, Jakarta Validation API                   | Input validation                |
| `spring-boot-starter-actuator`      | Health checks, metrics, monitoring endpoints                  | Production monitoring           |
| `spring-boot-starter-mail`          | JavaMail, Spring Mail                                         | Sending emails                  |
| `spring-boot-starter-thymeleaf`     | Thymeleaf template engine                                     | Server-side HTML rendering      |
| `spring-boot-starter-cache`         | Spring Cache abstraction                                      | Caching                         |
| `spring-boot-starter-aop`           | Spring AOP, AspectJ                                           | Aspect-Oriented Programming     |
| `spring-boot-starter-websocket`     | WebSocket support                                             | Real-time communication         |

### pom.xml — Typical Spring Boot Project

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot Parent — manages ALL dependency versions -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.library</groupId>
    <artifactId>library-management</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>library-management</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web (REST API + Embedded Tomcat) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- NO version needed! Parent manages it -->
        </dependency>

        <!-- Database (JPA + Hibernate + HikariCP) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Creates executable JAR with embedded Tomcat -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### The Parent POM — Version Management Magic

```
┌──────────────────────────────────────────────────────────────────┐
│        spring-boot-starter-parent (Parent POM)                   │
│                                                                  │
│   What it does:                                                  │
│   ┌────────────────────────────────────────────────────────┐     │
│   │  1. Defines versions for 300+ libraries                │     │
│   │     • Spring Framework: 6.1.x                          │     │
│   │     • Hibernate: 6.4.x                                 │     │
│   │     • Jackson: 2.16.x                                  │     │
│   │     • Tomcat: 10.1.x                                   │     │
│   │     • SLF4J: 2.0.x                                     │     │
│   │     • ... all tested to work TOGETHER                   │     │
│   │                                                        │     │
│   │  2. Provides default plugin configurations              │     │
│   │     • Java compiler settings                            │     │
│   │     • Resource filtering                                │     │
│   │     • JAR packaging                                     │     │
│   │                                                        │     │
│   │  3. Defines dependency management                       │     │
│   │     • You write: <artifactId>starter-web</artifactId>   │     │
│   │     • Parent fills in the version automatically         │     │
│   └────────────────────────────────────────────────────────┘     │
│                                                                  │
│   BENEFIT: No version conflicts! Everything is compatible.       │
└──────────────────────────────────────────────────────────────────┘
```

### Starter Naming Convention

```
┌──────────────────────────────────────────────────────────────────┐
│              STARTER NAMING RULES                                │
│                                                                  │
│  OFFICIAL (by Spring team):                                      │
│  spring-boot-starter-{name}                                      │
│  Examples:                                                       │
│  • spring-boot-starter-web                                       │
│  • spring-boot-starter-data-jpa                                  │
│  • spring-boot-starter-security                                  │
│                                                                  │
│  THIRD-PARTY (by community/vendors):                             │
│  {name}-spring-boot-starter                                      │
│  Examples:                                                       │
│  • mybatis-spring-boot-starter                                   │
│  • camel-spring-boot-starter                                     │
│                                                                  │
│  ⚠️ Third-party starters should NOT start with                   │
│     "spring-boot-starter-" — that's reserved for official ones.  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. application.properties / application.yml

### What are They?

These are **externalized configuration files** where you define application settings like database URL, server port, logging level, etc. Spring Boot reads them automatically from `src/main/resources/`.

### 💡 Real-World Analogy

Think of a **settings app on your phone**:
- You don't change the phone's source code to switch WiFi or change brightness.
- You use the **settings screen** to configure behavior externally.
- `application.properties` = your app's settings screen.

### application.properties (Key=Value Format)

```properties
# ============ SERVER CONFIG ============
server.port=8081
server.servlet.context-path=/api

# ============ DATABASE CONFIG ============
spring.datasource.url=jdbc:mysql://localhost:3306/library_db
spring.datasource.username=root
spring.datasource.password=secret123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ============ JPA / HIBERNATE CONFIG ============
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# ============ LOGGING ============
logging.level.root=INFO
logging.level.com.library=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.file.name=logs/library-app.log

# ============ CUSTOM PROPERTIES ============
library.name=City Central Library
library.max-books-per-member=5
library.fine-per-day=2.50
```

### application.yml (YAML Format — Same Thing, Different Syntax)

```yaml
# YAML uses indentation instead of dots
server:
  port: 8081
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/library_db
    username: root
    password: secret123
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

logging:
  level:
    root: INFO
    com.library: DEBUG
    org.hibernate.SQL: DEBUG
  file:
    name: logs/library-app.log

# Custom properties
library:
  name: City Central Library
  max-books-per-member: 5
  fine-per-day: 2.50
```

### Properties vs YAML — Comparison

| Feature                | application.properties           | application.yml                  |
| ---------------------- | -------------------------------- | -------------------------------- |
| Format                 | `key=value` (flat)               | Indentation-based (hierarchical) |
| Readability            | OK for few properties            | Better for many nested properties|
| Duplicate keys         | Allowed (last wins)              | Not allowed                      |
| Multi-line values      | Use `\` for continuation         | Native support                   |
| Lists                  | `list[0]=a`, `list[1]=b`         | `- a`, `- b`                     |
| Comments               | `#` or `!`                       | `#`                              |
| Blank values           | `key=` (empty string)            | `key: ''`                        |
| Indentation matters?   | No                               | **YES** (strict)                 |
| Spring priority        | Higher (overrides .yml)          | Lower                           |

### Reading Custom Properties in Code

**Method 1: @Value annotation**

```java
@Service
public class LibraryService {

    @Value("${library.name}")
    private String libraryName;

    @Value("${library.max-books-per-member}")
    private int maxBooks;

    @Value("${library.fine-per-day}")
    private double finePerDay;

    @Value("${library.location:Unknown}")  // Default value if property missing
    private String location;

    public String getLibraryInfo() {
        return libraryName + " — Max books: " + maxBooks + ", Fine: $" + finePerDay;
    }
}
```

**Method 2: @ConfigurationProperties (RECOMMENDED for groups of properties)**

```java
@Component
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private String name;
    private int maxBooksPerMember;
    private double finePerDay;

    // Getters and Setters (REQUIRED for @ConfigurationProperties)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMaxBooksPerMember() { return maxBooksPerMember; }
    public void setMaxBooksPerMember(int maxBooksPerMember) {
        this.maxBooksPerMember = maxBooksPerMember;
    }

    public double getFinePerDay() { return finePerDay; }
    public void setFinePerDay(double finePerDay) {
        this.finePerDay = finePerDay;
    }
}

// Usage in other classes:
@Service
public class FineCalculator {

    private final LibraryProperties libraryProperties;

    public FineCalculator(LibraryProperties libraryProperties) {
        this.libraryProperties = libraryProperties;
    }

    public double calculateFine(int daysLate) {
        return daysLate * libraryProperties.getFinePerDay();
    }
}
```

### @Value vs @ConfigurationProperties

| Feature                    | @Value                            | @ConfigurationProperties          |
| -------------------------- | --------------------------------- | --------------------------------- |
| Binding style              | One property at a time            | Group of related properties       |
| Type safety                | Limited (string injection)        | Full type safety with validation  |
| Validation                 | Manual                            | `@Validated` with `@NotNull` etc. |
| Relaxed binding            | No                                | Yes (`max-books` = `maxBooks`)    |
| Best for                   | 1-2 simple values                 | Structured config objects         |
| IDE support                | Limited                           | Auto-complete in IDE              |

### Profiles — Different Config Per Environment

```
┌──────────────────────────────────────────────────────────────────┐
│                   SPRING PROFILES                                │
│                                                                  │
│   src/main/resources/                                            │
│   ├── application.properties          ← DEFAULT (always loaded) │
│   ├── application-dev.properties      ← DEV profile             │
│   ├── application-test.properties     ← TEST profile            │
│   └── application-prod.properties     ← PRODUCTION profile      │
│                                                                  │
│   Activate with:                                                 │
│   • application.properties: spring.profiles.active=dev           │
│   • Command line: java -jar app.jar --spring.profiles.active=dev│
│   • Environment variable: SPRING_PROFILES_ACTIVE=prod            │
└──────────────────────────────────────────────────────────────────┘
```

**application.properties** (common/default):
```properties
library.name=Library App
spring.jpa.show-sql=false
```

**application-dev.properties** (development overrides):
```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:devdb
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.com.library=DEBUG
```

**application-prod.properties** (production overrides):
```properties
server.port=443
spring.datasource.url=jdbc:mysql://prod-server:3306/library
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.library=WARN
```

### Property Resolution Order (Priority — Highest to Lowest)

```
┌──────────────────────────────────────────────────────────────────┐
│         PROPERTY RESOLUTION ORDER (Highest Priority First)       │
│                                                                  │
│  1. Command-line arguments                                       │
│     java -jar app.jar --server.port=9090                         │
│                                                                  │
│  2. SPRING_APPLICATION_JSON (inline JSON)                        │
│                                                                  │
│  3. OS Environment variables                                     │
│     export SERVER_PORT=9090                                       │
│                                                                  │
│  4. application-{profile}.properties (profile-specific)          │
│                                                                  │
│  5. application.properties (default)                             │
│                                                                  │
│  6. @PropertySource annotations on @Configuration classes        │
│                                                                  │
│  7. Default properties (SpringApplication.setDefaultProperties)  │
│                                                                  │
│  ⬆ Higher overrides ⬇ Lower                                     │
│  Command-line args ALWAYS win over property files!               │
└──────────────────────────────────────────────────────────────────┘
```

### Commonly Used Spring Boot Properties

| Property                               | Default       | Description                              |
| -------------------------------------- | ------------- | ---------------------------------------- |
| `server.port`                          | `8080`        | HTTP server port                         |
| `server.servlet.context-path`          | `/`           | Base URL path                            |
| `spring.datasource.url`               | —             | Database JDBC URL                        |
| `spring.datasource.username`          | —             | Database username                        |
| `spring.datasource.password`          | —             | Database password                        |
| `spring.jpa.hibernate.ddl-auto`       | —             | `create`, `update`, `validate`, `none`   |
| `spring.jpa.show-sql`                 | `false`       | Print SQL queries to console             |
| `logging.level.root`                  | `INFO`        | Root logging level                       |
| `spring.profiles.active`             | —             | Active profile(s)                        |
| `spring.jackson.date-format`         | —             | JSON date format                         |
| `server.error.include-message`       | `never`       | Include error messages in response       |
| `spring.main.banner-mode`            | `console`     | `off`, `console`, `log`                  |

---

## 5. @SpringBootApplication

### What is @SpringBootApplication?

`@SpringBootApplication` is the **single most important annotation** in Spring Boot. It's a **meta-annotation** (composed of 3 annotations):

```java
// @SpringBootApplication is equivalent to these THREE:
@SpringBootConfiguration    // → @Configuration (this class can define @Bean methods)
@EnableAutoConfiguration    // → Turn on auto-configuration magic
@ComponentScan              // → Scan current package + sub-packages for beans
public class LibraryManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
```

### 💡 Real-World Analogy

`@SpringBootApplication` is like the **ignition key of a car**:
- One turn of the key does 3 things:
  1. **Starts the engine** (`@SpringBootConfiguration` — loads config)
  2. **Activates all systems** (`@EnableAutoConfiguration` — auto-config)
  3. **Scans for passengers** (`@ComponentScan` — finds all beans)

### Decomposition Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│               @SpringBootApplication                             │
│               (One annotation = Three)                           │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  @SpringBootConfiguration                                │   │
│   │  └── @Configuration                                      │   │
│   │      • Marks this class as a configuration source        │   │
│   │      • Can contain @Bean methods                         │   │
│   │      • Only ONE per application (convention)             │   │
│   └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  @EnableAutoConfiguration                                │   │
│   │      • Triggers auto-configuration                       │   │
│   │      • Reads META-INF/spring/...AutoConfiguration.imports│   │
│   │      • Evaluates @Conditional annotations                │   │
│   │      • Creates beans based on classpath                  │   │
│   └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │  @ComponentScan                                          │   │
│   │      • Scans the PACKAGE of this class                   │   │
│   │      • Also scans ALL sub-packages                       │   │
│   │      • Finds @Component, @Service, @Repository,          │   │
│   │        @Controller, @Configuration                       │   │
│   └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### ⚠️ Important: Package Placement Matters!

The `@SpringBootApplication` class MUST be placed in the **root package** of your project:

```
┌──────────────────────────────────────────────────────────────────┐
│                CORRECT PLACEMENT ✅                               │
│                                                                  │
│  com.library.library_management                                  │
│  ├── LibraryManagementApplication.java  ← @SpringBootApplication│
│  ├── controller/                          HERE (root package)    │
│  │   └── BookController.java            ✅ Scanned               │
│  ├── service/                                                    │
│  │   └── BookService.java              ✅ Scanned               │
│  ├── repository/                                                 │
│  │   └── BookRepository.java           ✅ Scanned               │
│  └── model/                                                      │
│      └── Book.java                     ✅ Scanned               │
│                                                                  │
│                WRONG PLACEMENT ❌                                 │
│                                                                  │
│  com.library.library_management.config                           │
│  └── LibraryManagementApplication.java  ← @SpringBootApplication│
│                                           in SUB-package!       │
│  com.library.library_management.service                          │
│  └── BookService.java                  ❌ NOT Scanned!           │
│                                        (sibling, not child)     │
└──────────────────────────────────────────────────────────────────┘
```

### What SpringApplication.run() Does Internally

```java
SpringApplication.run(LibraryManagementApplication.class, args);
```

```
┌──────────────────────────────────────────────────────────────────┐
│       SpringApplication.run() — INTERNAL STEPS                   │
│                                                                  │
│  1. ┌──────────────────────────────────────────┐                 │
│     │ Create SpringApplication instance        │                 │
│     │ • Detect application type (Web/Non-Web)  │                 │
│     │ • Load ApplicationContextInitializers    │                 │
│     │ • Load ApplicationListeners             │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  2. ┌──────────────────────────────────────────┐                 │
│     │ Print the Spring Boot Banner              │                 │
│     │   ____          _                         │                 │
│     │  / ___| _ __  _(_)_ __   __ _            │                 │
│     │  \___ \| '_ \| | | '_ \ / _` |           │                 │
│     │   ___) | |_) | | | | | | (_| |           │                 │
│     │  |____/| .__/|_|_|_| |_|\__, |           │                 │
│     │        |_|               |___/            │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  3. ┌──────────────────────────────────────────┐                 │
│     │ Create ApplicationContext                 │                 │
│     │ • AnnotationConfigServletWebServer...     │                 │
│     │   ApplicationContext (for web apps)       │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  4. ┌──────────────────────────────────────────┐                 │
│     │ Refresh Context                           │                 │
│     │ • Component scanning                      │                 │
│     │ • Auto-configuration                      │                 │
│     │ • Bean creation & DI                      │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  5. ┌──────────────────────────────────────────┐                 │
│     │ Start Embedded Server (Tomcat)            │                 │
│     │ • Bind to port 8080 (or configured port)  │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  6. ┌──────────────────────────────────────────┐                 │
│     │ Run ApplicationRunner / CommandLineRunner │                 │
│     │ • Execute startup tasks                   │                 │
│     └──────────────────┬───────────────────────┘                 │
│                        ▼                                        │
│  7. ┌──────────────────────────────────────────┐                 │
│     │ APPLICATION READY ✅                      │                 │
│     │ Listening on http://localhost:8080        │                 │
│     └──────────────────────────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
```

### Customizing SpringApplication

```java
@SpringBootApplication
public class LibraryManagementApplication {

    public static void main(String[] args) {
        // Method 1: Simple (most common)
        SpringApplication.run(LibraryManagementApplication.class, args);

        // Method 2: Customized
        SpringApplication app = new SpringApplication(LibraryManagementApplication.class);
        app.setBannerMode(Banner.Mode.OFF);            // Disable banner
        app.setDefaultProperties(Map.of(
            "server.port", "9090"                       // Default port
        ));
        app.run(args);
    }
}
```

### Startup Tasks — CommandLineRunner & ApplicationRunner

Run code **after** the application starts:

```java
@SpringBootApplication
public class LibraryManagementApplication implements CommandLineRunner {

    @Autowired
    private BookRepository bookRepository;

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // This runs AFTER the application is fully started
        System.out.println("Application started!");
        System.out.println("Total books in DB: " + bookRepository.count());

        // Seed initial data
        if (bookRepository.count() == 0) {
            bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));
            bookRepository.save(new Book("Spring in Action", "Craig Walls", "9781617294945"));
            System.out.println("Sample data loaded!");
        }
    }
}
```

```java
// Alternative: As a separate @Bean
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadData(BookRepository bookRepository) {
        return args -> {
            bookRepository.save(new Book("Effective Java", "Joshua Bloch", "9780134685991"));
            System.out.println("Data initialized!");
        };
    }
}
```

---

## 6. Embedded Server (Tomcat)

### What is an Embedded Server?

An **embedded server** is a web server (Tomcat, Jetty, or Undertow) that is **packaged inside your application JAR**. Instead of deploying your app TO a server, the server comes WITH your app.

### 💡 Real-World Analogy

- **Traditional (External Server):** You cook food at home → drive to a restaurant → use their kitchen/dining room to serve. (Deploy WAR to external Tomcat)
- **Embedded Server:** You have a **food truck** — the kitchen IS part of the truck. You drive anywhere and serve immediately. (JAR with embedded Tomcat)

```
┌──────────────────────────────────────────────────────────────────┐
│         TRADITIONAL DEPLOYMENT (External Tomcat)                 │
│                                                                  │
│   Developer Machine          Production Server                   │
│   ┌──────────────┐           ┌───────────────────────┐           │
│   │ Build WAR    │──deploy──▶│   Apache Tomcat       │           │
│   │ file         │           │   (installed on server)│           │
│   └──────────────┘           │   ┌─────────────────┐ │           │
│                              │   │  your-app.war   │ │           │
│   Steps:                     │   └─────────────────┘ │           │
│   1. Install Tomcat          └───────────────────────┘           │
│   2. Configure Tomcat                                            │
│   3. Build WAR                                                   │
│   4. Copy WAR to webapps/                                        │
│   5. Start Tomcat                                                │
│   6. Pray it works 🙏                                            │
│                                                                  │
│         SPRING BOOT (Embedded Tomcat)                            │
│                                                                  │
│   Developer Machine          Production Server                   │
│   ┌──────────────┐           ┌───────────────────────┐           │
│   │ Build JAR    │──copy──▶  │  java -jar app.jar    │           │
│   │ (FAT JAR)   │           │  ┌─────────────────┐  │           │
│   └──────────────┘           │  │ Your App Code   │  │           │
│                              │  │ + Embedded      │  │           │
│   Steps:                     │  │   Tomcat        │  │           │
│   1. mvn package             │  │ + Dependencies  │  │           │
│   2. java -jar app.jar       │  └─────────────────┘  │           │
│   3. Done! ✅                └───────────────────────┘           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### How Embedded Tomcat Works

```
┌──────────────────────────────────────────────────────────────────┐
│            EMBEDDED TOMCAT — INTERNAL STRUCTURE                  │
│                                                                  │
│   your-app.jar (Fat JAR / Uber JAR)                              │
│   ┌────────────────────────────────────────────────────────┐     │
│   │                                                        │     │
│   │   BOOT-INF/classes/          ← Your compiled code      │     │
│   │   ├── com/library/...                                  │     │
│   │   ├── application.properties                           │     │
│   │   └── static/, templates/                              │     │
│   │                                                        │     │
│   │   BOOT-INF/lib/             ← All dependency JARs      │     │
│   │   ├── spring-webmvc-6.1.x.jar                          │     │
│   │   ├── spring-core-6.1.x.jar                            │     │
│   │   ├── tomcat-embed-core-10.1.x.jar   ← TOMCAT IS HERE │     │
│   │   ├── tomcat-embed-el-10.1.x.jar                       │     │
│   │   ├── tomcat-embed-websocket-10.1.x.jar                │     │
│   │   ├── jackson-databind-2.16.x.jar                      │     │
│   │   ├── hibernate-core-6.4.x.jar                         │     │
│   │   └── ... 50+ JARs                                     │     │
│   │                                                        │     │
│   │   META-INF/MANIFEST.MF      ← Entry point              │     │
│   │   Main-Class: org.springframework.boot.loader.JarLauncher│    │
│   │   Start-Class: com.library.LibraryManagementApplication │     │
│   │                                                        │     │
│   │   org/springframework/boot/loader/  ← Boot classloader │     │
│   │                                                        │     │
│   └────────────────────────────────────────────────────────┘     │
│                                                                  │
│   Run: java -jar your-app.jar                                    │
│   Tomcat starts on port 8080 automatically!                      │
└──────────────────────────────────────────────────────────────────┘
```

### Embedded Server Startup Flow

```
┌──────────────────────────────────────────────────────────────────┐
│           EMBEDDED TOMCAT STARTUP FLOW                           │
│                                                                  │
│   java -jar library-app.jar                                      │
│        │                                                        │
│        ▼                                                        │
│   JarLauncher starts                                             │
│        │                                                        │
│        ▼                                                        │
│   SpringApplication.run()                                        │
│        │                                                        │
│        ▼                                                        │
│   Detect: "This is a WEB application"                            │
│   (spring-boot-starter-web is on classpath)                      │
│        │                                                        │
│        ▼                                                        │
│   Create: ServletWebServerApplicationContext                     │
│        │                                                        │
│        ▼                                                        │
│   Auto-configure: TomcatServletWebServerFactory                  │
│        │                                                        │
│        ▼                                                        │
│   ┌──────────────────────────────────────┐                       │
│   │  Create Tomcat instance              │                       │
│   │  • Set port (default: 8080)          │                       │
│   │  • Set context path (default: /)     │                       │
│   │  • Register DispatcherServlet        │                       │
│   │  • Register filters                  │                       │
│   │  • Configure thread pool             │                       │
│   └──────────────────┬───────────────────┘                       │
│                      ▼                                          │
│   ┌──────────────────────────────────────┐                       │
│   │  Tomcat.start()                      │                       │
│   │  "Tomcat started on port(s): 8080"   │                       │
│   └──────────────────────────────────────┘                       │
│                                                                  │
│   Application is now serving HTTP requests! 🌐                   │
└──────────────────────────────────────────────────────────────────┘
```

### Configuring Embedded Tomcat

**Via application.properties:**

```properties
# ============ SERVER PORT ============
server.port=8081
# Use port 0 for random available port (useful in testing)
# server.port=0

# ============ CONTEXT PATH ============
server.servlet.context-path=/library-api
# Now URLs are: http://localhost:8081/library-api/books

# ============ SSL / HTTPS ============
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.port=8443

# ============ TOMCAT TUNING ============
# Max threads for handling requests (default: 200)
server.tomcat.threads.max=200

# Min spare threads (default: 10)
server.tomcat.threads.min-spare=10

# Max connections (default: 8192)
server.tomcat.max-connections=8192

# Connection timeout (default: 20s)
server.tomcat.connection-timeout=20000

# Max HTTP request header size
server.max-http-request-header-size=8KB

# Max POST body size
server.tomcat.max-swallow-size=2MB
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Access log
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.directory=logs
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D
```

**Via Java Configuration:**

```java
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.setPort(9090);
            factory.setContextPath("/api");
            factory.addConnectorCustomizers(connector -> {
                connector.setMaxPostSize(10 * 1024 * 1024);  // 10MB
            });
        };
    }
}
```

### Switching from Tomcat to Jetty or Undertow

Spring Boot supports **three embedded servers**. Tomcat is the default, but you can switch:

```xml
<!-- SWITCH TO JETTY -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- Remove Tomcat -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Add Jetty -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

```xml
<!-- SWITCH TO UNDERTOW -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### Embedded Server Comparison

| Feature              | Tomcat                     | Jetty                      | Undertow                   |
| -------------------- | -------------------------- | -------------------------- | -------------------------- |
| Default in Boot?     | ✅ YES                     | ❌ Optional                | ❌ Optional                |
| Memory Usage         | Medium                     | Lower                      | Lowest                     |
| Performance          | Good                       | Good                       | Best (NIO-based)           |
| Maturity             | Most mature                | Very mature                | Newer                      |
| Servlet Support      | Full                       | Full                       | Full                       |
| WebSocket            | ✅                         | ✅                         | ✅                         |
| HTTP/2               | ✅                         | ✅                         | ✅                         |
| Best For             | General purpose            | Microservices, low memory  | High performance           |

### Building & Running the FAT JAR

```bash
# Step 1: Build the fat JAR
mvn clean package
# or
./mvnw clean package

# Output: target/library-management-0.0.1-SNAPSHOT.jar (50-80 MB)

# Step 2: Run it
java -jar target/library-management-0.0.1-SNAPSHOT.jar

# With custom properties
java -jar target/library-management-0.0.1-SNAPSHOT.jar --server.port=9090

# With a profile
java -jar target/library-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Run in background (Linux/Mac)
nohup java -jar app.jar &
```

### WAR Deployment (If You Must Use External Tomcat)

```java
// Extend SpringBootServletInitializer for WAR deployment
@SpringBootApplication
public class LibraryManagementApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LibraryManagementApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
```

```xml
<!-- pom.xml — change packaging from jar to war -->
<packaging>war</packaging>

<!-- Mark Tomcat as provided (external Tomcat provides it) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## 7. Common Mistakes & Troubleshooting

### ❌ Mistake 1: "Port 8080 already in use"

```
Error: Web server failed to start. Port 8080 was already in use.
```

**Fix:**
```properties
# Option 1: Change port
server.port=8081

# Option 2: Kill the process using port 8080
# Windows:
# netstat -ano | findstr :8080
# taskkill /PID <pid> /F

# Linux/Mac:
# lsof -i :8080
# kill -9 <pid>
```

---

### ❌ Mistake 2: "Failed to configure a DataSource"

```
Error: Failed to configure a DataSource: 'url' attribute is not specified
and no embedded datasource could be configured.
```

**Cause:** You added `spring-boot-starter-data-jpa` but didn't configure a database URL.

**Fix:**
```properties
# Option 1: Add database config
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret

# Option 2: Use H2 in-memory database for development
# Add H2 dependency and it auto-configures

# Option 3: Exclude DataSource auto-config (if you don't need a DB)
# @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

---

### ❌ Mistake 3: "Whitelabel Error Page" (404)

**Cause:** Controller is not in a sub-package of `@SpringBootApplication`.

```
┌────────────────────────────────────────────────────┐
│  com.library.app                                   │
│  └── LibraryApplication.java  (@SpringBootApp)     │
│                                                    │
│  com.library.controller    ← DIFFERENT root!       │
│  └── BookController.java   ❌ Not scanned!         │
│                                                    │
│  FIX: Move to com.library.app.controller           │
│  or add @ComponentScan(basePackages="com.library") │
└────────────────────────────────────────────────────┘
```

---

### ❌ Mistake 4: YAML Indentation Errors

```yaml
# WRONG ❌ — inconsistent indentation
spring:
  datasource:
   url: jdbc:mysql://localhost:3306/mydb   # 3 spaces
    username: root                          # 4 spaces — ERROR!

# RIGHT ✅ — consistent 2-space indentation
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb   # 4 spaces
    username: root                          # 4 spaces
```

---

### ❌ Mistake 5: Wrong Property Names

```properties
# WRONG ❌
spring.datasource.URL=jdbc:mysql://...        # Capital letters
spring.data-source.url=jdbc:mysql://...       # Wrong hyphenation

# RIGHT ✅
spring.datasource.url=jdbc:mysql://...        # Exact property name
```

---

### ❌ Mistake 6: Fat JAR Won't Start — No Main Manifest Attribute

```
Error: no main manifest attribute, in app.jar
```

**Cause:** Missing `spring-boot-maven-plugin` in `pom.xml`.

**Fix:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

---

## 8. Interview Questions & Answers

### Q1: What is the difference between Spring and Spring Boot?

**Answer:**
- **Spring Framework** is a comprehensive Java framework providing IoC, DI, AOP, MVC, etc. It requires extensive manual configuration (XML or Java-based).
- **Spring Boot** is a layer ON TOP of Spring Framework that provides auto-configuration, embedded servers, starter dependencies, and opinionated defaults to eliminate boilerplate.
- Spring Boot does NOT replace Spring — it makes Spring easier and faster to use.

---

### Q2: What is Auto-Configuration and how does it work?

**Answer:**
- Auto-configuration automatically configures Spring beans based on what dependencies are on the classpath.
- It's triggered by `@EnableAutoConfiguration` (part of `@SpringBootApplication`).
- Spring Boot reads auto-configuration classes from `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Each auto-config class uses `@Conditional` annotations to check if conditions are met (class on classpath, property set, etc.).
- If all conditions pass, the beans are created. If the user already defined the bean (`@ConditionalOnMissingBean`), auto-config backs off.

---

### Q3: What does @SpringBootApplication do?

**Answer:** It's a convenience annotation that combines three annotations:
1. `@SpringBootConfiguration` (`@Configuration`) — makes the class a configuration source.
2. `@EnableAutoConfiguration` — enables auto-configuration based on classpath.
3. `@ComponentScan` — scans the current package and sub-packages for Spring beans.

---

### Q4: What are Spring Boot Starters?

**Answer:**
- Starters are curated dependency bundles that group related libraries needed for a specific functionality.
- They simplify dependency management — one starter replaces 10+ individual dependencies.
- The parent POM (`spring-boot-starter-parent`) manages versions for all starters, preventing version conflicts.
- Official starters follow the naming pattern `spring-boot-starter-{name}`.

---

### Q5: What is the difference between application.properties and application.yml?

**Answer:**
- `.properties` uses flat `key=value` format. `.yml` uses hierarchical indentation-based YAML format.
- YAML is more readable for deeply nested properties but is strict about indentation.
- Both are functionally equivalent — Spring Boot reads both.
- If both exist, `.properties` takes precedence over `.yml`.
- YAML supports multiple documents in one file using `---` separator (useful for profiles).

---

### Q6: What is an Embedded Server? Why is it used?

**Answer:**
- An embedded server (Tomcat/Jetty/Undertow) is packaged inside the application JAR.
- Benefits:
  - No need to install/configure a separate server
  - Application is self-contained and portable (`java -jar app.jar`)
  - Simplifies deployment — especially for containers (Docker) and cloud platforms
  - Each app has its own server version — no server-level conflicts
- Tomcat is the default. You can switch to Jetty or Undertow by excluding Tomcat starter and adding the alternative.

---

### Q7: How can you change the default port in Spring Boot?

**Answer:** Multiple ways (in order of priority):
1. **Command line:** `java -jar app.jar --server.port=9090`
2. **Environment variable:** `SERVER_PORT=9090`
3. **application.properties:** `server.port=9090`
4. **application.yml:** `server: port: 9090`
5. **Programmatic:** `SpringApplication.setDefaultProperties(Map.of("server.port", "9090"))`
6. **Random port:** `server.port=0` (useful for testing)

---

### Q8: What is the purpose of spring-boot-starter-parent?

**Answer:**
- It's a parent POM that provides:
  - **Dependency version management** for 300+ libraries (no version conflicts)
  - **Default Maven plugin configurations** (compiler, resource filtering, packaging)
  - **Java version settings** and **encoding**
  - **Resource filtering** for `application.properties` (supports `@property@` placeholders)
- You specify the version of `spring-boot-starter-parent` once, and ALL starter dependency versions are managed automatically.

---

### Q9: How do Spring Profiles work?

**Answer:**
- Profiles allow you to define **different configurations for different environments** (dev, test, prod).
- Profile-specific files: `application-{profile}.properties` or `application-{profile}.yml`.
- Activate with: `spring.profiles.active=dev` in properties, or `--spring.profiles.active=dev` on command line.
- Default `application.properties` is always loaded. Profile-specific properties **override** the defaults.
- You can also use `@Profile("dev")` annotation on `@Configuration` classes or `@Bean` methods.

---

### Q10: Can you deploy a Spring Boot app as a WAR?

**Answer:**
- Yes, even though JAR is the default/recommended packaging.
- Steps:
  1. Change `<packaging>war</packaging>` in pom.xml
  2. Extend `SpringBootServletInitializer` in the main class
  3. Mark `spring-boot-starter-tomcat` as `<scope>provided</scope>`
  4. Deploy the WAR to an external Tomcat server
- However, JAR with embedded server is recommended for most use cases (cloud-native, Docker, microservices).

---

## Quick Reference Cheat Sheet

```
┌────────────────────────────────────────────────────────────────┐
│              SPRING BOOT — CHEAT SHEET                          │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Spring Boot = Spring Framework + Auto-Config + Starters       │
│                + Embedded Server + Actuator                     │
│                                                                │
│  @SpringBootApplication = @Configuration                       │
│                         + @EnableAutoConfiguration              │
│                         + @ComponentScan                        │
│                                                                │
│  Starters = Curated dependency bundles                         │
│  • spring-boot-starter-web      → REST/Web apps               │
│  • spring-boot-starter-data-jpa → Database + Hibernate         │
│  • spring-boot-starter-security → Authentication               │
│  • spring-boot-starter-test     → Testing                      │
│                                                                │
│  Config = application.properties or application.yml            │
│  Profiles = application-{profile}.properties                   │
│  Priority: cmd args > env vars > profile props > default props │
│                                                                │
│  Embedded Server: Tomcat (default) | Jetty | Undertow          │
│  Package: mvn clean package → java -jar app.jar                │
│                                                                │
│  Auto-Config Golden Rule:                                      │
│  "Your @Bean ALWAYS overrides auto-configuration"              │
│                                                                │
│  Debug: debug=true → see auto-config report                    │
│  Exclude: @SpringBootApplication(exclude = {...})              │
│                                                                │
│  Read props: @Value("${key}") or @ConfigurationProperties      │
│  Change port: server.port=9090                                 │
│  Change context: server.servlet.context-path=/api              │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

> **Author:** Spring Boot Study Notes  
> **Covers:** Spring Boot 3.x / Spring Framework 6.x / Java 17+
