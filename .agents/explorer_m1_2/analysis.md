# Milestone M1 Analysis Report: Database Infrastructure & Spring Boot Initialization

**Author:** Explorer M1 Instance 2  
**Date:** 2026-07-24  
**Working Directory:** `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_m1_2`  
**Target Scope:** Workspace Root (`c:/Users/nhan/Workspace/OmniCare-EMR/`) & Backend API (`c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/`)

---

## 1. Executive Summary & Objective Scope

Milestone M1 establishes the foundational infrastructure and project skeleton for the **OmniCare EMR Core Backend**.
This investigation analyzed two primary requirements specified in `PROJECT.md` and `ORIGINAL_REQUEST.md`:
- **R1 (Database Infrastructure):** PostgreSQL `docker-compose.yml` configuration placed at the workspace root (`c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`), exposing port `5432` with healthchecks, persistent volume storage, and environment variables.
- **R2 (Spring Boot Project Initialization):** Maven-based Spring Boot 3.x project layout under `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/` targeting **Java 17**, equipped with `pom.xml`, `Dockerfile`, Maven Wrapper files (`mvnw`, `mvnw.cmd`, `.mvn/`), `README.md`, strict package organization, and `application.yml` readiness.

---

## 2. Existing Workspace State Analysis

| Target Path | Existing Status | Observations & Recommendations |
|---|---|---|
| `c:/Users/nhan/Workspace/OmniCare-EMR/` | Present | Currently contains `.agents/` and `knowledge/`. Root directory is ready for `docker-compose.yml`. |
| `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml` | Missing | Needs creation in M1. Must set up PostgreSQL container. |
| `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/` | Missing | Needs directory creation and full Spring Boot project initialization in M1. |
| `knowledge/` | Present | Contains API Design (`OMNICARE-EMR_API_Design.md`), DB Design (`OMNICARE-EMR_Database_Design.md`), and Project Overview. Confirms PostgreSQL DB name `omnicare_emr` and audit fields. |

---

## 3. Requirement R1 Breakdown: PostgreSQL `docker-compose.yml`

### 3.1 Technical Requirements
1. File Location: `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`
2. Container Engine: Docker Compose format v3.8.
3. Database Image: `postgres:16-alpine` (lightweight, stable).
4. Service Name: `postgres` / Container Name: `omnicare-postgres`.
5. Port Binding: `5432:5432` (exposing host port 5432 to container port 5432).
6. Environment Variables:
   - `POSTGRES_DB`: `omnicare_emr`
   - `POSTGRES_USER`: `omnicare_user`
   - `POSTGRES_PASSWORD`: `omnicare_password`
7. Volume Persistence: `postgres_data:/var/lib/postgresql/data`
8. Health check: `pg_isready -U omnicare_user -d omnicare_emr` with interval `10s`, timeout `5s`, retries `5`.
9. Restart Policy: `unless-stopped`.

### 3.2 Concrete Specification File (`docker-compose.yml`)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: omnicare-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: omnicare_emr
      POSTGRES_USER: omnicare_user
      POSTGRES_PASSWORD: omnicare_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U omnicare_user -d omnicare_emr"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local
```

---

## 4. Requirement R2 Breakdown: Spring Boot Project Setup (`omnicare-emr-api`)

### 4.1 Project Directory Layout Blueprint

The Spring Boot backend will live under `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/` with the following strict package and directory layout:

```
omnicare-emr-api/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── Dockerfile
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── omnicare/
    │   │           └── emr/
    │   │               ├── OmniCareEmrApplication.java
    │   │               ├── config/
    │   │               │   └── AuditConfig.java
    │   │               ├── controller/
    │   │               │   └── PatientController.java
    │   │               ├── dto/
    │   │               │   ├── ErrorResponseDto.java
    │   │               │   ├── PatientRequestDto.java
    │   │               │   └── PatientResponseDto.java
    │   │               ├── entity/
    │   │               │   ├── BaseEntity.java
    │   │               │   └── Patient.java
    │   │               ├── exception/
    │   │               │   ├── DuplicateIdentifierException.java
    │   │               │   └── GlobalExceptionHandler.java
    │   │               ├── repository/
    │   │               │   └── PatientRepository.java
    │   │               └── service/
    │   │                   ├── PatientService.java
    │   │                   └── impl/
    │   │                       └── PatientServiceImpl.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── omnicare/
                    └── emr/
                        └── OmniCareEmrApplicationTests.java
```

---

### 4.2 `pom.xml` Specification

Target framework: **Spring Boot 3.2.4**  
Java Version: **17**  
Build tool: **Maven**

#### Required Dependencies:
1. `org.springframework.boot:spring-boot-starter-web`: Controller REST API, embedded Tomcat, Jackson.
2. `org.springframework.boot:spring-boot-starter-data-jpa`: Spring Data JPA, Hibernate, ORM support.
3. `org.postgresql:postgresql`: PostgreSQL JDBC Driver (runtime scope).
4. `org.projectlombok:lombok`: Boilerplate reduction (@Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor).
5. `org.springframework.boot:spring-boot-starter-validation`: Bean validation annotations (`@NotBlank`, `@NotNull`, `@Pattern`, `@Past`).
6. `org.springframework.boot:spring-boot-starter-test`: JUnit 5, Mockito, Spring Boot Test (test scope).

#### Complete `pom.xml` Model Specification:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
        <relativePath/>
    </parent>
    
    <groupId>com.omnicare</groupId>
    <artifactId>omnicare-emr-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>omnicare-emr-api</name>
    <description>OmniCare EMR Core Backend API Service</description>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Starter Test -->
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
        </plugins>
    </build>
</project>
```

---

### 4.3 Multi-Stage `Dockerfile` Specification

Multi-stage build ensures efficient, small production container images using OpenJDK 17.

```dockerfile
# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
# Pre-fetch dependencies for faster subsequent builds
RUN ./mvnw dependency:go-offline -B || true
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S omnicare && adduser -S omnicare -G omnicare
USER omnicare
COPY --from=builder /app/target/omnicare-emr-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 4.4 Maven Wrapper Files Strategy

To guarantee build tool compatibility on systems without Maven pre-installed, `omnicare-emr-api` must include Maven wrapper scripts:
1. `mvnw` (POSIX shell script for Unix/Linux/macOS)
2. `mvnw.cmd` (Batch script for Windows)
3. `.mvn/wrapper/maven-wrapper.properties` containing:
   ```properties
   distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
   wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
   ```

---

### 4.5 Configuration Blueprint (`application.yml`)

Spring Boot configuration connecting to the PostgreSQL container and enabling automatic DDL execution:

```yaml
spring:
  application:
    name: omnicare-emr-api
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/omnicare_emr}
    username: ${SPRING_DATASOURCE_USERNAME:omnicare_user}
    password: ${SPRING_DATASOURCE_PASSWORD:omnicare_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080
```

---

### 4.6 `README.md` Specification

`omnicare-emr-api/README.md` will document developer quick-start steps:
- System Prerequisites (Java 17, Docker Engine / Docker Compose, Maven 3.8+ or Maven Wrapper)
- Starting Database: `docker-compose up -d` (from project root)
- Building Backend: `./mvnw clean package`
- Running Application: `./mvnw spring-boot:run`
- Docker Deployment: `docker build -t omnicare-emr-api .`
- API Summary & Architecture overview

---

## 5. Verification & Test Plan

1. **Docker Compose Verification:**
   - Execute `docker-compose up -d` from workspace root.
   - Verify container `omnicare-postgres` is running (`docker ps`).
   - Check database connection on port `5432`.

2. **Spring Boot Build & Compilation Verification:**
   - Run `./mvnw clean compile` under `omnicare-emr-api`.
   - Verify zero compilation errors with JDK 17.

3. **Application Boot Verification:**
   - Run `./mvnw spring-boot:run` under `omnicare-emr-api`.
   - Confirm Spring Boot starts on port 8080 and connects to PostgreSQL database without dialect or connection errors.

---

## 6. Implementation Action Plan for Implementer Agent (M1)

1. Create `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml` with the specified PostgreSQL 16 container definition.
2. Create directory structure for `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/`.
3. Populate `pom.xml`, `Dockerfile`, `README.md`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`.
4. Create application entry class `OmnCareEmrApplication.java` and initial package folders.
5. Create `src/main/resources/application.yml` with PostgreSQL configuration.
6. Verify build and startup.
