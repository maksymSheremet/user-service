# User Service

## Overview

Spring Boot microservice responsible for user profile management.
Consumes `USER_REGISTERED` events from **auth-service** via Kafka and manages user profiles, onboarding, and internal service-to-service communication.

## Features

- **User Profile Management** — get, update profile, complete onboarding
- **Kafka Consumer** — processes `USER_REGISTERED` events with idempotency (at-least-once safety)
- **Dead Letter Queue** — failed events routed to `-dlt` topic with exponential backoff retry
- **Internal API** — service-to-service endpoints secured via `X-Internal-Api-Key`
- **JWT Validation** — stateless authentication via Bearer token (token type validation against substitution attacks)
- **Database Migrations** — Flyway with versioned SQL scripts
- **Health Checks** — Spring Actuator with liveness/readiness probes

## Tech Stack

- **Java** 25
- **Gradle** 9.1.0
- **Spring Boot** 3.5.7
- **Spring Cloud** 2025.0.0
- **Spring Security** — stateless JWT filter
- **PostgreSQL** 17 + Flyway
- **Apache Kafka** — async event consumption
- **MapStruct** — DTO / Entity mapping
- **Docker Compose** — local development
- **Testcontainers** — integration tests with real PostgreSQL & Kafka

## Project Structure

```
my.code.userservice
├── config/              — KafkaConfig, SecurityConfig, JwtProperties, InternalApiProperties
├── controller/          — UserController (/api/me), InternalUserController (/api/internal/users)
├── dto/
│   ├── event/           — UserRegisteredEvent
│   ├── request/         — UpdateProfileRequest, CompleteOnboardingRequest
│   └── response/        — UserProfileResponse
├── entity/              — User, UserStatus
├── exception/           — GlobalExceptionHandler, UserNotFoundException
├── filter/              — JwtAuthenticationFilter, InternalApiKeyFilter
├── kafka/               — UserEventConsumer, DlqEventConsumer
├── mapper/              — UserMapper (MapStruct)
├── repository/          — UserRepository
└── service/             — UserService, UserServiceImpl, JwtService
```

## API Endpoints

### Public (JWT Bearer)

| Method | Path                  | Auth   | Description             |
|--------|-----------------------|--------|-------------------------|
| GET    | `/api/me`             | Bearer | Get own profile         |
| PUT    | `/api/me`             | Bearer | Update own profile      |
| POST   | `/api/me/onboarding`  | Bearer | Complete onboarding     |

### Internal (X-Internal-Api-Key)

| Method | Path                                 | Auth     | Description             |
|--------|--------------------------------------|----------|-------------------------|
| GET    | `/api/internal/users/{userId}`       | API Key  | Get profile by ID       |
| GET    | `/api/internal/users/by-email/{email}` | API Key | Get profile by email    |

> `/api/me` — userId завжди береться з JWT токена, підміна через URL неможлива.

## Kafka Events

| Event             | Topic                    | Action                          |
|-------------------|--------------------------|---------------------------------|
| `USER_REGISTERED` | `user-registered-events` | Creates user profile in DB      |
| (failed events)   | `user-registered-events-dlt` | Dead Letter — logged, stored |

**Idempotency:** повторна обробка одного event не створює дублікат (`existsById` check).

**Retry strategy:** exponential backoff (1s → 2s → 4s, max 8s) → DLT.

## Database Migrations

| Version | Description           |
|---------|-----------------------|
| V1      | Create `users` table  |

## Tests

| Test Class                        | Type        | Coverage                                      |
|-----------------------------------|-------------|-----------------------------------------------|
| `UserServiceImplTest`             | Unit        | Service business logic (10 tests)             |
| `JwtServiceTest`                  | Unit        | JWT validation, token substitution (7 tests)  |
| `InternalApiKeyFilterTest`        | Unit        | API key filter, timing attack (7 tests)       |
| `UserServiceIntegrationTest`      | Integration | Service → real PostgreSQL (11 tests)          |
| `UserEventConsumerIntegrationTest`| Integration | Kafka end-to-end, idempotency (2 tests)       |
| `UserServiceApplicationTests`     | Integration | Spring context smoke test (1 test)            |

## Setup

### Prerequisites
- Java 25
- Docker & Docker Compose

### Run locally

```bash
# Start infrastructure (PostgreSQL, Kafka, Zookeeper, Kafka UI)
docker-compose up -d

# Run the service
./gradlew bootRun
```

### Run tests

```bash
# Unit tests only (fast)
./gradlew test

# All tests including integration (requires Docker)
./gradlew test -Dtags=integration
```

### Environment Variables

```env
DATA_URL=jdbc:postgresql://localhost:5432/userdb
DATA_USER=postgres
DATA_PASSWORD=postgres
JWT_SECRET=<base64-encoded-256bit-key>
INTERNAL_API_KEY=<random-secure-string>
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```
