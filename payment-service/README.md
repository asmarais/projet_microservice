# Payment Service

Standalone owner of supplier-invoice settlements and settlement reporting.
Invoice references are scalar IDs in `payment_db`; no Procurement foreign key
is used. Payment calls Procurement through OpenFeign for invoice eligibility
and active invoice totals.

## Runtime

- Java 21; Spring Boot 4.0.9; Spring Cloud 2025.1.4
- MySQL database `payment_db`
- Config Server on 8888; Eureka on 8761
- Default port 8087 (`PAYMENT_SERVICE_PORT`)
- OpenFeign/LoadBalancer to `procurement-service`, resolved via Eureka

Set `PAYMENT_DB_URL`, `PAYMENT_DB_USERNAME`, and `PAYMENT_DB_PASSWORD` outside
source control. Flyway owns the schema; Hibernate validates it. No production
data migration is performed.

## API fidelity and limits

Implements the legacy `/reglements/**` list/detail/delete, invoice lookup,
and revenue routes. Settlement create/update validates the scalar `factureId`
through Procurement: missing invoices return `404`, archived invoices return
`409`, and dependency failures return `503` without persisting the settlement.
Eligibility currently checks only existence and archive status; outstanding
balance, currency, idempotency, and reconciliation rules remain unimplemented.

The legacy `/facture/pourcentageRecouvrement/{startDate}/{endDate}` URL is
served by Payment through a more-specific Gateway route. Payment reads the
active invoice total from Procurement and settlement revenue from its own
database, keeping the synchronous dependency one-way. The former internal
revenue endpoint remains available for compatibility but Procurement no
longer calls it.

Legacy amounts remain `float`; currency/decimal policy is pending. Revenue
uses the existing active-invoice denominator and includes settlement revenue
for the date range; it cannot exclude settlements for archived invoices
without invoice lifecycle facts/projections. The endpoints are not safe for
production cutover until authentication, amount/eligibility policy,
idempotency, reconciliation, and archived-invoice reporting are approved.

Run `.\mvnw.cmd clean verify` for the standalone build.
