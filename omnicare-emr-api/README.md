# OmniCare EMR Core API (`omnicare-emr-api`)

## Project Overview
OmniCare EMR Core Backend API is a Spring Boot 3.2.5 application built with Java 17, providing core electronic medical record microservices for patient management, medical records, and clinical workflows.

## Tech Stack
- **Java**: 17
- **Framework**: Spring Boot 3.2.5 (Spring Web, Spring Data JPA, Spring Validation)
- **Database**: PostgreSQL 16 (via Docker Compose)
- **ORM / Persistence**: Hibernate / JPA
- **Build Tool**: Apache Maven
- **Containerization**: Docker multi-stage build

## Package Architecture
The application adheres to a strict package structure under `com.omnicare.emr`:
- `config`: Spring Configuration beans (MVC, Security, JPA, Auditing).
- `controller`: REST API endpoints handling HTTP requests and responses.
- `dto`: Request and response Data Transfer Objects with validation annotations.
- `entity`: JPA entity models and mapped superclasses (`BaseEntity`, `Patient`).
- `exception`: Domain-specific exceptions and `@RestControllerAdvice` exception handlers.
- `repository`: Data access interfaces extending `JpaRepository`.
- `service`: Business logic interface contracts and implementation components (`PatientService`, `PatientServiceImpl`).

## Prerequisites
- JDK 17 or higher
- Apache Maven 3.8+ (or Maven Wrapper)
- Docker & Docker Compose

## Database Infrastructure Setup
To start the PostgreSQL 16 container, run the following command from the workspace root (`c:/Users/nhan/Workspace/OmniCare-EMR`):

```bash
docker-compose up -d
```

### PostgreSQL Details:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `omnicare_db`
- **Username**: `omnicare_user`
- **Password**: `omnicare_pass`

## Build & Run Commands

### Local Development
To compile the project and verify zero compilation errors:
```bash
mvn clean compile
```

To run unit and integration tests:
```bash
mvn clean test
```

To package the executable JAR artifact:
```bash
mvn clean package
```

To run the application locally:
```bash
java -jar target/omnicare-emr-api-0.0.1-SNAPSHOT.jar
```
Or using Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

### Docker Container Build & Run
To build the multi-stage Docker image:
```bash
docker build -t omnicare-emr-api .
```

To run the containerized API:
```bash
docker run -p 8080:8080 --name omnicare-api omnicare-emr-api
```

## API Documentation

### Patient Management API

#### Create Patient
- **Endpoint**: `POST /api/v1/patients`
- **Content-Type**: `application/json`
- **Request Body Example**:
```json
{
  "fullName": "Nguyen Van A",
  "identifier": "012345678901",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phoneNumber": "0912345678",
  "email": "nguyenvana@example.com",
  "address": "123 Le Loi, District 1, Ho Chi Minh City"
}
```
- **Responses**:
  - `201 Created`: Returns created patient record including generated `id` (UUID), `createdAt`, `updatedAt`, `version`, and `isDeleted`.
  - `400 Bad Request`: Returned when payload fails validation constraint checks.
  - `409 Conflict`: Returned when patient identifier/CCCD already exists.
