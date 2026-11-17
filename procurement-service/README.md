# Procurement Service

Standalone owner of supplier invoices, invoice lines, invoice lifecycle, and
local operator assignments. It uses a command/query split within the service.
Axon Framework and Axon event storage are not enabled because the required
Boot-compatible runtime/persistence decision is still open; this is not the
final CQRS acceptance state.

## Runtime

- Java 21; Spring Boot 4.0.9; Spring Cloud 2025.1.4
- MySQL database `procurement_db`
- Config Server on 8888; Eureka on 8761
- Default port 8086 (`PROCUREMENT_SERVICE_PORT`)
- Feign dependencies: `product-service`, `supplier-service`, and
  `user-service`, resolved via Eureka (no host/port URLs). Payment queries
  Procurement's internal invoice endpoints; Procurement does not call Payment.

Set `PROCUREMENT_DB_URL`, `PROCUREMENT_DB_USERNAME`, and
`PROCUREMENT_DB_PASSWORD` outside source control. Flyway owns the schema;
Hibernate validates it. No production data migration is performed.

## API fidelity and limits

Implements the legacy `/facture/**` invoice paths and scalar header fields.
Create accepts an optional legacy-shaped `detailsFacture` array (`qteCommandee`,
`pourcentageRemise`, and `produit.idProduit`). Procurement validates each
product through the existing Product `GET /produits/{productId}` endpoint,
applies the monolith's existing line-price/discount calculation, persists
product snapshots, and returns typed line DTOs on create/list/get. Existing
header-only Angular requests remain valid. A nullable `fournisseurId` request
field remains an additive scalar reference validated through Supplier.

Internal APIs expose invoice payment eligibility and active invoice totals.
Payment validates settlement writes and calculates the recovery rate from
these queries; the Gateway preserves the legacy recovery-rate URL by routing
that specific path to Payment. Invoice eligibility currently checks existence
and archive status only. Amounts remain `float`; currency, outstanding-balance,
zero-total, and recovery-report error semantics remain open. Inventory
receiving is intentionally deferred because no confirmed-receipt workflow or
Inventory receipt API exists. No production data migration is performed.

Run `.\mvnw.cmd clean verify` for the standalone build.
