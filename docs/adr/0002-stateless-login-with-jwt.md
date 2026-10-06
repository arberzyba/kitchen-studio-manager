# 2. Stateless login with JWT

## Context

The React frontend and the Spring Boot backend are separate applications that can be hosted at different addresses. The backend has to know who is calling and what role they have.

## Decision

Login returns a signed JSON Web Token (HS256) that is valid for 8 hours and carries the user's id and role. The frontend sends it as a `Bearer` token with every request. The backend validates it with Spring Security's built-in resource server support and keeps no session.

Role checks are declared on the controllers with `@PreAuthorize` and tested for every feature.

## Why

- No server-side session means any backend instance can answer any request.
- Spring Security already validates tokens and maps claims to roles, so there is no hand-written security filter to get wrong.
- A shared secret (HS256) is enough while one application both issues and checks the tokens.

## Consequences

- A token cannot be revoked before it expires. A deactivated employee keeps access until their token runs out, at most 8 hours. Their next login is refused.
- The frontend keeps the token in local storage, which script injected into the page could read. An httpOnly cookie would protect against that at the cost of CSRF handling; for an internal tool this trade-off was accepted and is listed as a limit in the README.
- There is no refresh token; users sign in again after 8 hours. An expired token is discarded by the frontend, which returns to the login page.
