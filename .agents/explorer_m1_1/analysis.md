# Milestone M1 Technical Strategy & File Layout Analysis

**Author:** Explorer M1 Instance 1  
**Target Milestone:** M1 — Database Infrastructure & Spring Boot Project Initialization  
**Workspace Root:** `c:/Users/nhan/Workspace/OmniCare-EMR/`  
**Project Path:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`  

---

## 1. Executive Summary & Current Workspace Status

An inspection of the workspace root (`c:/Users/nhan/Workspace/OmniCare-EMR/`) reveals the following current state:
- **Existing Directories**: `.agents/` (agent metadata, plans, progress) and `knowledge/` (project domain design docs for database and API).
- **Missing Infrastructure**: Neither `docker-compose.yml` at the workspace root nor the `omnicare-emr-api/` Java project directory exists yet.

Milestone M1 establishes the foundation of the OmniCare EMR Core Backend. This report provides an exact blueprint for implementing:
1. `docker-compose.yml` at the workspace root for PostgreSQL database containerization.
2. Maven project configuration (`pom.xml`) with Spring Boot 3.2+ and required starters.
3. Multi-stage container build definition (`Dockerfile`).
4. Project documentation (`README.md`).
5. Strict Java package hierarchy (`com.omnicare.emr.*`) and base application bootstrap class.

---

## 2. Detailed Component Specifications

### 2.1 Database Infrastructure (`docker-compose.yml`)

**Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`

**Specifications:**
- **Service Name**: `postgres` (or `omnicare-db`)
- **Image**: `postgres:16-alpine`
- **Container Name**: `omnicare-postgres`
- **Port Mapping**: `5432:5432`
- **Environment Variables**:
  - `POSTGRES_DB=omnicare_db`
  - `POSTGRES_USER=omnicare_user`
  - `POSTGRES_PASSWORD=omnicare_pass`
- **Healthcheck**:
  - Test command: `pg_isready -U omnicare_user -d omnicare_db`
  - `interval`: `10s`
  - `timeout`: `5s`
  - `retries`: `5`
  - `start_period`: `10s`
- **Volume Mapping**:
  - Named volume `postgres_data` mapped to `/var/lib/postgresql/data`
- **Restart Policy**: `unless-stopped`

**Proposed `docker-compose.yml` Content:**
```yaml
version: '3.8'

services:
  omnicare-db:
    image: postgres:16-alpine
    container_name: omnicare-postgres
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: omnicare_db
      POSTGRES_USER: omnicare_user
      POSTGRES_PASSWORD: omnicare_pass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U omnicare_user -d omnicare_db"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

volumes:
  postgres_data:
    driver: local
```

---

### 2.2 Maven Project Structure & Dependency Specification (`pom.xml`)

**Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/pom.xml`

**Specifications:**
- **Parent**: `org.springframework.boot:spring-boot-starter-parent:3.2.5`
- **Java Version**: `17`
- **Group ID**: `com.omnicare`
- **Artifact ID**: `omnicare-emr-api`
- **Version**: `0.0.1-SNAPSHOT`
- **Required Dependencies**:
  1. `org.springframework.boot:spring-boot-starter-web` (RESTful Web APIs)
  2. `org.springframework.boot:spring-boot-starter-data-jpa` (JPA / Hibernate ORM)
  3. `org.springframework.boot:spring-boot-starter-validation` (Bean Validation API / Hibernate Validator)
  4. `org.postgresql:postgresql` (PostgreSQL JDBC Driver - runtime scope)
  5. `org.projectlombok:lombok` (Boilerplate code reduction - optional/provided)
  6. `org.springframework.boot:spring-boot-starter-test` (Testing framework JUnit 5, Mockito, Spring Test - test scope)
- **Build Plugin**: `org.springframework.boot:spring-boot-maven-plugin` configured with Lombok annotation processor path if necessary.

**Proposed `pom.xml` Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Boot Starter Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL JDBC Driver -->
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

### 2.3 Multi-Stage Container Definition (`Dockerfile`)

**Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/Dockerfile`

**Specifications:**
- **Stage 1 (Build)**: Uses `maven:3.9.6-eclipse-temurin-17-alpine` to compile the application and generate executable JAR.
- **Stage 2 (Runtime)**: Uses `eclipse-temurin:17-jre-alpine` lightweight runtime environment.
- **Exposed Port**: `8080`
- **Entrypoint**: `java -jar app.jar`

**Proposed `Dockerfile` Content:**
```dockerfile
# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy compiled JAR from build stage
COPY --from=build /app/target/omnicare-emr-api-0.0.1-SNAPSHOT.jar app.jar

# Expose API port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 2.4 Strict Java Package & Directory Hierarchy

**Root Package:** `com.omnicare.emr`  
**Physical Directory:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr`

**Directory Tree:**
```
omnicare-emr-api/
├── Dockerfile
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── omnicare/
    │   │           └── emr/
    │   │               ├── OmnicareEmrApiApplication.java
    │   │               ├── config/
    │   │               ├── controller/
    │   │               ├── dto/
    │   │               ├── entity/
    │   │               ├── exception/
    │   │               ├── repository/
    │   │               └── service/
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── omnicare/
                    └── emr/
                        └── OmnicareEmrApiApplicationTests.java
```

**Package Responsibilities:**
- `com.omnicare.emr`: Application entry point (`OmnicareEmrApiApplication.java` annotated with `@SpringBootApplication`).
- `config/`: Spring Configuration classes (e.g., Auditing, Web MVC CORS, OpenAPI/Swagger if added later).
- `controller/`: REST API Controllers (e.g., `PatientController.java`).
- `dto/`: Request/Response Data Transfer Objects (e.g., `PatientRequestDto`, `PatientResponseDto`, `ErrorResponseDto`).
- `entity/`: JPA entities and mapped superclasses (e.g., `BaseEntity`, `Patient`).
- `exception/`: Domain exceptions and `GlobalExceptionHandler` (`@RestControllerAdvice`).
- `repository/`: Spring Data JPA interfaces (e.g., `PatientRepository`).
- `service/`: Service interfaces and implementation classes (e.g., `PatientService`, `PatientServiceImpl`).

---

### 2.5 Application Configuration (`application.yml`)

**Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/resources/application.yml`

**Specifications:**
- Database Connection details corresponding to `docker-compose.yml`:
  - `url: jdbc:postgresql://localhost:5432/omnicare_db`
  - `username: omnicare_user`
  - `password: omnicare_pass`
- Hibernate Auto-DDL setting: `update` (enables dynamic entity-to-schema mapping for M2).
- Dialect: `org.hibernate.dialect.PostgreSQLDialect`
- Server Port: `8080`

**Proposed `application.yml` Content:**
```yaml
server:
  port: 8080

spring:
  application:
    name: omnicare-emr-api

  datasource:
    url: jdbc:postgresql://localhost:5432/omnicare_db
    username: omnicare_user
    password: omnicare_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

### 2.6 Documentation Blueprint (`README.md`)

**Location:** `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/README.md`

**Key Sections:**
1. **OmniCare EMR Core API Overview**: System description and architecture stack.
2. **Prerequisites**: Java 17+, Maven 3.8+, Docker & Docker Compose.
3. **Database Infrastructure Setup**: Command `docker-compose up -d` at root.
4. **Build & Run Instructions**:
   - Local: `./mvnw clean package` / `mvn clean package` and `java -jar target/omnicare-emr-api-0.0.1-SNAPSHOT.jar`
   - Docker: `docker build -t omnicare-emr-api .` and `docker run -p 8080:8080 omnicare-emr-api`
5. **API Endpoint Documentation**:
   - `POST /api/v1/patients`: Patient registration schema, example JSON body, status codes (201, 400, 409).

---

## 3. Sequence of File Creation for Implementation

To implement M1 smoothly, the Implementer should execute the following steps:
1. Create `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`.
2. Create `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/pom.xml`.
3. Create `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/Dockerfile`.
4. Create package directories under `src/main/java/com/omnicare/emr/`:
   - `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`
5. Create `OmnicareEmrApiApplication.java` in `com.omnicare.emr`.
6. Create `application.yml` in `src/main/resources/`.
7. Create `README.md` in `omnicare-emr-api/`.
8. Create initial test harness in `src/test/java/com/omnicare/emr/OmnicareEmrApiApplicationTests.java`.

---

## 4. Verification Plan

1. **Docker Compose Verification**:
   - Run `docker-compose up -d` from `c:/Users/nhan/Workspace/OmniCare-EMR/`
   - Run `docker ps` to verify `omnicare-postgres` container is healthy on port 5432.
2. **Spring Boot Build & Launch Verification**:
   - Navigate to `omnicare-emr-api/` and run `mvn clean compile` or `mvn clean test`.
   - Verify zero build failures and correct directory structure.
