# Milestone M1 Code Modification Summary

## Files Created / Modified

### 1. `docker-compose.yml` (Workspace Root)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`
- **Description**: Configured PostgreSQL 16 Alpine containerized database infrastructure.
- **Key Settings**:
  - Service: `postgres`
  - Container Name: `omnicare-postgres`
  - Image: `postgres:16-alpine`
  - Environment: `POSTGRES_DB=omnicare_db`, `POSTGRES_USER=omnicare_user`, `POSTGRES_PASSWORD=omnicare_pass`
  - Ports: `5432:5432`
  - Healthcheck: `pg_isready -U omnicare_user -d omnicare_db` (interval 10s, timeout 5s, retries 5, start_period 10s)
  - Persistent volume: `postgres_data` mapped to `/var/lib/postgresql/data`

### 2. `pom.xml`
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/pom.xml`
- **Description**: Configured Maven project for Spring Boot 3.2.5 and Java 17.
- **Dependencies Included**:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-validation`
  - `postgresql` (runtime scope)
  - `lombok` (optional)
  - `spring-boot-starter-test` (test scope)
- **Plugins Included**:
  - `maven-compiler-plugin` (configured for Java 17 and Lombok processor)
  - `spring-boot-maven-plugin` (configured to exclude Lombok from executable JAR)

### 3. `Dockerfile`
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/Dockerfile`
- **Description**: Multi-stage container build definition.
- **Build Stage**: `maven:3.9.6-eclipse-temurin-17-alpine`
- **Runtime Stage**: `eclipse-temurin:17-jre-alpine`
- **Exposed Port**: `8080`
- **Entrypoint**: `java -jar app.jar`

### 4. Application Configuration (`application.yml`)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/resources/application.yml`
- **Description**: Configured Spring Boot database connection properties matching `docker-compose.yml`, Hibernate DDL auto `update`, PostgreSQL dialect, and server port 8080.

### 5. Application Main Class (`OmnicareApiApplication.java`)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/OmnicareApiApplication.java`
- **Description**: Application entry point annotated with `@SpringBootApplication`.

### 6. Strict Package Structure (`package-info.java`)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/{config,controller,dto,entity,exception,repository,service}/package-info.java`
- **Description**: Established strict package directory hierarchy required by PROJECT.md.

### 7. Application Test (`OmnicareApiApplicationTests.java`)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/test/java/com/omnicare/emr/OmnicareApiApplicationTests.java`
- **Description**: Initial context load unit test harness for Spring Boot.

### 8. Project Documentation (`README.md`)
- **Path**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/README.md`
- **Description**: Comprehensive documentation covering project overview, tech stack, package architecture, prerequisites, database setup commands, local build/run commands, Docker build/run commands, and API specification for `POST /api/v1/patients`.
