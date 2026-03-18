# UW-Madison Course Monitor

A containerized, high-frequency course enrollment monitoring system for the University of Wisconsin-Madison public course search API. The platform tracks course availability in real-time, sends immediate email notifications on status changes (e.g., WAITLISTED → OPEN), and provides user-isolated task management with JWT-based authentication.

## Key Features

* **Real-time Monitoring**: Polling-based monitoring of specific course sections.
* **Anti-WAF Strategy**: Implemented randomized jitter (variable delay) and user-agent rotation to mimic human behavior and avoid IP bans from university firewalls.
* **Request Aggregation**: Batches queries for multiple sections of the same course into single API requests, reducing network overhead.
* **Instant Notifications**: SMTP email alerts when monitored section states change, delivered to the owning user's account email.
* **JWT Authentication**: Supports user registration/login and stateless token-based API authorization.
* **User Data Isolation**: Every task is scoped by `user_id`, including monitoring, CRUD operations, and scheduler processing.
* **Audit Logging**: Writes both application logs and section status history (`logs/application.log`, `logs/history.csv`).
* **Dockerized Deployment**: Fully containerized application and database for consistent deployment across environments.

## Tech Stack

* **Language**: Java 21
* **Framework**: Spring Boot 4.0, Spring Security
* **Database**: MySQL 8.0
* **Containerization**: Docker & Docker Compose
* **Network/Parsing**: Jsoup, Jackson
* **Auth**: JWT (JJWT), BCrypt password hashing
* **Build Tool**: Maven

## Architecture Highlights

### 1. Polling Logic & Optimization
Instead of naive fixed-interval polling, the system uses a dynamic scheduling algorithm.
* **Jitter**: Introduces random deviations to the polling interval to evade pattern-based firewall detection.
* **Aggregation**: If monitoring many sections under one course, the scheduler fetches course-level data once and updates matching sections locally.

### 2. Authentication & Authorization
* **Auth APIs**: `/auth/register` and `/auth/login`.
* **Token Flow**: Login validates BCrypt password hashes and returns JWT for the frontend.
* **Protected APIs**: `/api/tasks/**` requires Bearer token; unauthenticated access returns `401`.
* **Frontend Integration**: The web client stores JWT and attaches it to `Authorization` headers for all task requests.

### 3. Data Model & Persistence
* **Users Table**: Stores `id`, `email`, and `password_hash`.
* **Tasks Table**: Stores monitoring tasks with `user_id` ownership and composite uniqueness on `(user_id, section_id)`.
* **Migration Bootstrap**: Existing legacy task records are assigned to the configured admin user at startup.
* **State Durability**: Task state remains persistent across restarts via MySQL.

## API Overview

### Public Endpoints
* `POST /auth/register`
* `POST /auth/login`

### Protected Endpoints (JWT Required)
* `GET /api/tasks`
* `POST /api/tasks?courseName=...`
* `PATCH /api/tasks/{id}/toggle`
* `DELETE /api/tasks?courseDisplayName=...`

## Getting Started

### Prerequisites
* Docker Desktop (Recommended)
* **OR** Java 21 SDK + MySQL Server 8.0

### Configuration

1.  Clone the repository.
2.  Copy the example configuration file:
    ```bash
    cp src/main/resources/application.properties.example src/main/resources/application.properties
    ```
3.  Edit `src/main/resources/application.properties` with your details:
    * **SMTP Settings**: Your Gmail credentials and sender address (`spring.mail.*`, `app.mail.from`).
    * **JWT Secret**: `app.jwt.secret` must be a strong key (at least 32 characters).
    * **Admin Bootstrap User**: `app.auth.admin.email` / `app.auth.admin.password`.
    * **Target Courses / Term**: `uw-api.term-id`, `uw-api.subject-id`.

### Deployment (Docker) - **Recommended**

The project includes a `docker-compose.yml` that orchestrates both the Spring Boot application and the MySQL database.

1.  **Build and Run**:
    ```bash
    docker-compose up -d --build
    ```
    *This command builds the JAR file, creates the Docker image, and starts both the App and MySQL containers in the background.*

2.  **Check Logs**:
    ```bash
    docker-compose logs -f
    ```

3.  **Stop Services**:
    ```bash
    docker-compose down
    ```

### Local Development (Manual)

1.  Ensure a local MySQL instance is running on port 3306.
2.  Update `application.properties` to point to `localhost:3306`.
3.  Run the application:
    ```bash
    ./mvnw spring-boot:run
    ```
4.  Open `http://localhost:8080`, register/login, then create your monitoring tasks.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
