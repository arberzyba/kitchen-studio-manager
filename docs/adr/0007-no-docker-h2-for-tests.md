# 7. No Docker; H2 for tests

## Context

The project should run on a developer machine without Docker. Tests still need a database, and the usual way to test against real PostgreSQL, Testcontainers, depends on Docker.

## Decision

- Development uses a locally installed PostgreSQL.
- Backend tests run against an in-memory H2 database that Spring Boot starts automatically.
- Flyway migrations are written in SQL that both databases accept, so the tests run the same migrations as production.
- Browser tests run against the real stack with PostgreSQL.

## Why

- `./mvnw test` works on a fresh checkout with only a JDK installed.
- Running the real migrations in tests catches a broken migration before it reaches a real database.

## Consequences

- H2 is not PostgreSQL. A query that behaves differently on PostgreSQL would pass the backend tests. Two things reduce the risk: queries use JPQL, not database-specific SQL, and the browser tests exercise every feature on PostgreSQL.
- Migrations cannot use PostgreSQL-only features without giving up H2 for tests.
- Testcontainers remains the better choice once Docker is acceptable.
