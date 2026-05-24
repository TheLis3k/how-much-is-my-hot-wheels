# How Much Is My Hot Wheels 🏎️

> A Spring Boot REST API that scrapes five marketplaces — **eBay, Etsy, OLX, Vinted, and the Hot Wheels Fandom Wiki** — to value Hot Wheels die-cast cars on demand.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-brightgreen)
![MongoDB](https://img.shields.io/badge/MongoDB-7-green)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## Why this project

Hot Wheels collectors have no central price index. Values are scattered across auction sites, second-hand marketplaces and wiki pages, in multiple currencies, with no consistent naming. This project solves that: a scheduled scraping engine pulls listings from five sources, normalizes them, persists them in MongoDB, and exposes a small REST API that answers questions like *"what is car X worth right now?"* and *"which car in the catalogue currently has the highest market value?"*.

It was built to practice production-style Spring Boot — multi-source scraping with both static (JSoup) and JavaScript-rendered (Playwright) pages, async scheduled jobs, aggregation queries on MongoDB, layered architecture, and Docker-based local infrastructure.

---

## Key features

- **Multi-source scraping engine** — one dedicated scraper per marketplace under [scrapper/engine/](src/main/java/pl/thelis3k/howmuchismyhotwheels/scrapper/engine/).
- **Hybrid scraping stack** — [JSoup](https://jsoup.org/) for static HTML, [Microsoft Playwright](https://playwright.dev/java/) for JS-rendered pages.
- **Scheduled background jobs** — [`ScrapingScheduler`](src/main/java/pl/thelis3k/howmuchismyhotwheels/scrapper/scheduler/ScrapingScheduler.java) runs scrapers asynchronously via a dedicated executor ([`AsyncConfig`](src/main/java/pl/thelis3k/howmuchismyhotwheels/config/AsyncConfig.java)).
- **Aggregated valuations** — per-platform breakdown plus min / max / average across sources, served by [`ValuationService`](src/main/java/pl/thelis3k/howmuchismyhotwheels/valuation/service/).
- **Catalogue browsing** — search by name, list by release year, look up the currently most- and least-valuable car in the database.
- **Global exception handling** — single [`GlobalExceptionHandler`](src/main/java/pl/thelis3k/howmuchismyhotwheels/exception/GlobalExceptionHandler.java) translates errors into consistent JSON responses.
- **Dockerized infrastructure** — `docker-compose up -d` brings up MongoDB and Mongo Express; Spring Boot's Docker Compose integration wires the app to it automatically in dev.
- **Spring Security wired in** — central [`SecurityConfig`](src/main/java/pl/thelis3k/howmuchismyhotwheels/config/SecurityConfig.java) filter chain, ready to add auth rules (currently open for local development).

---

## Architecture

```mermaid
flowchart LR
    Client[HTTP Client] -->|REST| Controller[HotWheelsController]

    Controller --> ValuationService
    Controller --> CarRepo[(HotWheelsCarRepository)]
    Controller --> ValRepo[(CarValuationRepository)]

    ValuationService --> CarRepo
    ValuationService --> ValRepo

    Scheduler[ScrapingScheduler] -->|@Async| Scrapers
    subgraph Scrapers[Scraping engines]
        Ebay[EbayScraper]
        Etsy[EtsyApiService]
        Olx[OlxScraper]
        Vinted[VintedScraper]
        Fandom[FandomScraper]
    end
    Scrapers --> ValRepo
    Fandom --> CarRepo

    CarRepo <--> Mongo[(MongoDB)]
    ValRepo <--> Mongo
```

- **Fandom Wiki** seeds the catalogue of known cars.
- **eBay, Etsy, OLX, Vinted** feed price data points into `CarValuation` documents.
- **Valuation queries** aggregate those data points on the fly.

---

## Tech stack

| Layer        | Technology |
|--------------|------------|
| Language     | Java 21 |
| Framework    | Spring Boot 3 (Web MVC, Data MongoDB, Security, Validation) |
| Persistence  | MongoDB 7 |
| Scraping     | JSoup 1.17, Microsoft Playwright 1.41, Jackson |
| Async / jobs | Spring `@Scheduled` + `@Async` with custom executor |
| Tooling      | Maven, Lombok, Docker Compose, Spring Boot Docker Compose integration |

---

## API reference

Base URL: `http://localhost:8080/api`

| Method | Endpoint                          | Purpose |
|--------|-----------------------------------|---------|
| GET    | `/health`                         | Liveness probe — `{ "status": "UP" }` |
| GET    | `/stats`                          | Total cars in catalogue + how many have at least one valuation |
| GET    | `/years`                          | Distinct release years available in the catalogue |
| GET    | `/cars/{year}`                    | All cars released in the given year, sorted by name |
| GET    | `/namevaluation/{name}`           | Fuzzy search by car name |
| GET    | `/valuation/{id}`                 | Full car details + aggregated valuation across platforms |
| GET    | `/valuation/max` &nbsp;/&nbsp; `/valuation/min` | Currently most- / least-valuable car in the database |

Example:

```sh
curl http://localhost:8080/api/health
curl http://localhost:8080/api/valuation/max
curl http://localhost:8080/api/namevaluation/Mustang
```

---

## Project structure

```
src/main/java/pl/thelis3k/howmuchismyhotwheels/
├── HowMuchIsMyHotWheelsApplication.java
├── config/          # AsyncConfig, SecurityConfig, DataSeeder
├── controller/      # HotWheelsController + response DTOs
├── exception/       # GlobalExceptionHandler
├── hotwheels/       # Core domain: HotWheelsCar model + repository
├── scrapper/
│   ├── engine/      # One scraper per source (eBay, Etsy, OLX, Vinted, Fandom)
│   └── scheduler/   # ScrapingScheduler — orchestrates async runs
├── util/            # PriceUtil — currency / number parsing helpers
└── valuation/       # Valuation model, repository, DTOs, service
```

---

## Getting started

### Prerequisites
- JDK 21
- Maven (or use the bundled `./mvnw`)
- Docker & Docker Compose

### Run locally

```sh
# 1. Configure environment
cp .env.example .env          # fill in MongoDB credentials

# 2. Start MongoDB + Mongo Express
docker compose up -d

# 3. Run the app
./mvnw spring-boot:run
```

- API:           http://localhost:8080/api/health
- Mongo Express: http://localhost:8081

---

## Roadmap

- [ ] Unit + integration tests (Testcontainers for MongoDB, WireMock for scrapers)
- [ ] GitHub Actions CI (build, test, lint, container image)
- [ ] Currency normalization (PLN / EUR / USD) with daily FX rates
- [ ] Caching layer for valuation responses (Caffeine / Redis)
- [ ] OpenAPI / Swagger UI documentation
- [ ] Real authentication (JWT) and per-user collections
- [ ] Minimal frontend (React or HTMX) and deployment to Fly.io / Render

---

## License

MIT

## Author

Built by **[@TheLis3k](https://github.com/TheLis3k)**. Feedback and PRs welcome.
