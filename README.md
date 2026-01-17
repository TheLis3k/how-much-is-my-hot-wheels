# How Much Is My Hot Wheels 🏎️

Backend service for tracking and valuing Hot Wheels collections.
Scrapes market data to provide price estimations.

## Tech Stack
* **Java 21**
* **Spring Boot 3**
* **MongoDB**
* **Docker & Docker Compose**

## Getting Started

### Prerequisites
* Docker & Docker Compose
* JDK 21
* Maven

### Installation
1. Clone the repository.
2. Create `.env` file based on `.env.example`:

``` sh
   cp .env.example .env
```

(Fill in your own passwords in `.env`)

3. Run the database infrastructure:
``` sh
  docker compose up -d
```


4. Access Mongo Express (Database UI):
* URL: `http://localhost:8081`



### Project Structure

* `scrapper` - Logic for retrieving data from external APIs/Sources.
* `hotwheels` - Core domain logic.
* `security` - Auth mechanisms.
