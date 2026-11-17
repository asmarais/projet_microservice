# User Service

Standalone staff-operator profile service. It owns `user_db`; it does not
retain the legacy cross-service `Operateur.factures` relationship. The
legacy `factureIds` response field remains present and empty because invoice
assignments belong to Procurement.

## Runtime

- Java 21; Spring Boot 4.0.9; Spring Cloud 2025.1.4
- MySQL database `user_db`
- Config Server on 8888; Eureka on 8761
- Default port 8085 (`USER_SERVICE_PORT`)

Set `USER_DB_URL`, `USER_DB_USERNAME`, and `USER_DB_PASSWORD` outside source
control. Flyway owns the schema; Hibernate validates it. No production data
migration is performed.

## Scope and security limits

`/operateurs/**` preserves operator profile CRUD fields/statuses. Incoming
passwords are stored as Spring's delegating password-encoder hashes and are
never returned. `/internal/v1/operators/{operatorId}` supplies a reference
for Procurement. Login/token issuance, refresh/revocation, roles/permissions,
lockout, JWKS, and authorization policy remain unimplemented pending security
decisions; this service does not yet authenticate requests. Do not expose the
compatibility routes publicly until an approved authorization policy is
deployed.

Run `.\mvnw.cmd clean verify` for the standalone build.
