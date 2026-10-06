# 1. Feature packages with layers inside

## Context

A Spring Boot application is usually layered into controllers, services and repositories. The packages can follow those layers (`controller`, `service`, `repository`) or the business features (`customer`, `quote`, `invoice`).

## Decision

Packages follow features. Inside each feature package the classes are layered: a controller for HTTP and validation, a service for business rules and transactions, a repository for database access, and DTO records for requests and responses.

## Why

- Everything about quotes is in one place, so a change to quotes touches one package.
- Dependencies between features become visible as imports between packages. They point one way: `invoice` uses `order`, `order` uses `quote`, `quote` uses `customer` and `product`. Features that need several others, such as `dashboard` and `privacy`, sit on top.
- The layering inside each package keeps the familiar rules: controllers never touch repositories, entities never leave the service layer.

## Consequences

- Shared code needs its own home: `common` for error handling and document numbers, `document` for the PDF layout and mail sending used by quotes and invoices.
- Two features cannot import each other. When a quote page needs to know about its order, the frontend asks the order endpoint instead of the quote package depending on the order package.
