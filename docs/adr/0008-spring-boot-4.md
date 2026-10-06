# 8. Spring Boot 4 instead of 3

## Context

The project was planned with Spring Boot 3. When it was started, in October 2026, the official project generator (start.spring.io) no longer offered the 3.x line; it listed only 4.0 and 4.1.

## Decision

Use Spring Boot 4.1 with Java 21.

## Why

- A new project should start on a line that is currently maintained, not on one that is on its way out.
- Java 21 is supported by both lines, so the language level is unchanged.

## Consequences

- Some details differ from Spring Boot 3 material: starters are split into smaller modules (for example `spring-boot-starter-webmvc`), test auto-configuration moved to new packages, and JSON handling is based on Jackson 3.
- Libraries had to be chosen in versions that support Spring Boot 4, such as springdoc-openapi 3.
