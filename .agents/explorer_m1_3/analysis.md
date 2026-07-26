# Strategy & File Layout Report — Milestone M1 (Database Infrastructure & Spring Boot Initialization)

**Author:** Explorer M1 Instance 3  
**Date:** 2026-07-24  
**Target Workspace:** `c:/Users/nhan/Workspace/OmniCare-EMR`  
**API Working Directory:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`  

---

## 1. Executive Summary & Objective

Milestone M1 establishes the core operational baseline for the OmniCare EMR project. The primary goal is to set up a robust, reproducible backend foundation consisting of:
1. Containerized PostgreSQL database infrastructure exposed via `docker-compose.yml` at the project root.
2. Initialized Spring Boot 3.x application (`omnicare-emr-api`) with Java 17+, Maven build configuration (`pom.xml`), strict package architecture, multi-stage `Dockerfile`, and developer documentation (`README.md`).

---

## 2. Requirement Breakdown & Detailed Specifications

### Requirement R1: Database Infrastructure (`docker-compose.yml`)

- **Location:** Project Root (`c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`).
- **Purpose:** Provide a lightweight, isolated PostgreSQL instance running locally.
- **Port Binding:** `5432:5432` on localhost.
- **Data Persistence:** Managed named Docker volume `postgres_data`.
- **Health Check:** `pg_isready` command ensuring database readiness before dependent services connect.

#### Proposed `docker-compose.yml`
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: omnicare-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: omnicare_db
      POSTGRES_USER: omnicare_user
      POSTGRES_PASSWORD: omnicare_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U omnicare_user -d omnicare_db"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local
```

---

### Requirement R2: Spring Boot Project Structure (`omnicare-emr-api`)

- **Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/`
- **JDK Target:** Java 17 (LTS)
- **Framework:** Spring Boot 3.2.5 (or latest stable Spring Boot 3.x)

#### Required Maven Dependencies (`pom.xml`)
1. `spring-boot-starter-web` — REST API support & embedded Tomcat servlet container.
2. `spring-boot-starter-data-jpa` — Persistence layer with Hibernate ORM & Spring Data JPA.
3. `spring-boot-starter-validation` — Bean Validation (`jakarta.validation-api`) for DTO validation.
4. `org.postgresql:postgresql` — JDBC driver for PostgreSQL connection.
5. `org.projectlombok:lombok` — Boilerplate code reduction (getters, setters, builders, constructors).
6. `spring-boot-starter-test` — Unit & integration testing utilities (JUnit 5, AssertJ, Mockito).

#### Proposed `pom.xml` Structure
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    
    <groupId>com.omnicare</groupId>
    <artifactId>omnicare-emr-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>omnicare-emr-api</name>
    <description>OmniCare EMR Core Backend API</description>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Database Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
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
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Strict Directory & Package Layout

The application code must follow a clear layered architecture inside `com.omnicare.emr`:

```
c:/Users/nhan/Workspace/OmniCare-EMR/
├── docker-compose.yml                      # PostgreSQL container orchestration
└── omnicare-emr-api/                       # Spring Boot Project Root
    ├── .gitignore                          # Git ignore rules for Java/Maven/IDE
    ├── Dockerfile                          # Multi-stage container build definition
    ├── pom.xml                             # Maven build configuration
    ├── README.md                           # Quickstart & documentation
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/
        │   │       └── omnicare/
        │   │           └── emr/
        │   │               ├── OmnicareApiApplication.java    # Entry point
        │   │               ├── config/                         # Configuration beans (JPA, Web, Security, etc.)
        │   │               ├── controller/                     # REST controllers (@RestController)
        │   │               ├── dto/                            # Data Transfer Objects (Request/Response DTOs)
        │   │               ├── entity/                         # JPA Entities (@Entity, BaseEntity)
        │   │               ├── exception/                      # Custom Exceptions & @ControllerAdvice
        │   │               ├── repository/                     # Spring Data JPA Repositories
        │   │               └── service/                        # Business logic interfaces & implementation
        │   │                   └── impl/                       # Service implementation classes
        │   └── resources/
        │       └── application.yml                             # DB connection & Spring properties
        └── test/
            └── java/
                └── com/
                    └── omnicare/
                        └── emr/
                            └── OmnicareApiApplicationTests.java # Context loading smoke test
```

---

### Application Entry Point: `OmnicareApiApplication.java`

```java
package com.omnicare.emr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OmnicareApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmnicareApiApplication.class, args);
    }
}
```

---

### Application Configuration: `application.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: omnicare-emr-api

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:omnicare_db}
    username: ${DB_USER:omnicare_user}
    password: ${DB_PASSWORD:omnicare_pass}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

---

### Containerization: `Dockerfile`

```dockerfile
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### `.gitignore` Specification

```gitignore
# Target output
target/
*.class

# Log files
*.log

# IDE settings
.idea/
*.iml
.classpath
.project
.settings/
.vscode/

# OS metadata
.DS_Store
Thumbs.db
```

---

### Documentation: `README.md` Proposal

```markdown
# OmniCare EMR Core API (`omnicare-emr-api`)

## Prerequisites
- Java 17 SDK or higher
- Maven 3.8+ (or Maven Wrapper)
- Docker & Docker Compose

## Quick Start Guide

### 1. Start Database Container
At the project root (`c:/Users/nhan/Workspace/OmniCare-EMR`):
```bash
docker compose up -d
```
Verify PostgreSQL is healthy:
```bash
docker compose ps
```

### 2. Build & Run API Service
Inside `omnicare-emr-api`:
```bash
mvn clean package
mvn spring-boot:run
```

### 3. Run Unit & Integration Tests
```bash
mvn clean test
```
```

---

## 3. Implementation Checklist for Implementer M1

- [ ] Create `docker-compose.yml` in `c:/Users/nhan/Workspace/OmniCare-EMR/`.
- [ ] Create directory structure for `omnicare-emr-api/`.
- [ ] Create `pom.xml` with specified dependencies and Maven compiler configuration.
- [ ] Create `OmnicareApiApplication.java` under `com.omnicare.emr`.
- [ ] Create package layout (`config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`, `service/impl`).
- [ ] Create `application.yml` under `src/main/resources/`.
- [ ] Create `OmnicareApiApplicationTests.java` under `src/test/java/com/omnicare/emr/`.
- [ ] Create `Dockerfile`, `.gitignore`, and `README.md`.
- [ ] Execute `mvn clean compile` to ensure zero compilation or Lombok annotation processing errors.

---

## 4. Risks & Mitigations

1. **Port 5432 Conflict:** Local PostgreSQL instances running on port 5432 may clash with `docker-compose`.
   - *Mitigation:* Document environment variable overrides (`DB_PORT`) in `application.yml` and `docker-compose.yml`.
2. **Lombok Annotation Processing Failures:** JDK 17+ strict module system may require proper `maven-compiler-plugin` configuration.
   - *Mitigation:* Explicitly declare `annotationProcessorPaths` in `pom.xml`.
3. **Database Connection Delay on Startup:** Spring Boot starting faster than PostgreSQL container.
   - *Mitigation:* Include health checks in `docker-compose.yml`.
