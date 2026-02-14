# UW-Madison Course Monitor

A containerized, high-frequency course enrollment monitoring system for the University of Wisconsin-Madison public course search API. Designed to track course availability in real-time and send immediate email notifications upon status changes (e.g., WAITLISTED → OPEN).

## Key Features

* **Real-time Monitoring**: Polling-based monitoring of specific course sections.
* **Anti-WAF Strategy**: Implemented randomized jitter (variable delay) and user-agent rotation to mimic human behavior and avoid IP bans from university firewalls.
* **Request Aggregation**: Batches queries for multiple sections of the same course into single API requests, reducing network overhead by approximately 80%.
* **Instant Notifications**: Asynchronous email delivery using SMTP (Gmail integration).
* **Dockerized Deployment**: Fully containerized application and database for consistent deployment across environments.

## Tech Stack

* **Language**: Java 21
* **Framework**: Spring Boot 4.0
* **Database**: MySQL 8.0
* **Containerization**: Docker & Docker Compose
* **Network/Parsing**: Jsoup, Spring RestTemplate
* **Build Tool**: Maven

## Architecture Highlights

### 1. Polling Logic & Optimization
Instead of naive fixed-interval polling, the system uses a dynamic scheduling algorithm.
* **Jitter**: Introduces random deviations to the polling interval to evade pattern-based firewall detection.
* **Aggregation**: If monitoring 15 sections of "CS 577", the system identifies the common subject code and fetches data in a single request, parsing the specific sections locally.

### 2. Data Persistence
MySQL is used to persist monitoring tasks and user configurations. This ensures that the monitoring state is preserved even if the application container restarts.

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
    * **SMTP Settings**: Your Gmail credentials (App Password required).
    * **Target Courses**: The Course IDs and Term IDs you wish to monitor.

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.