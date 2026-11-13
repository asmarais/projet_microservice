# Microservices Development Standards

**Status:** Shared engineering baseline for the six business services. This
document does not implement services or resolve the open business, security,
data, and deployment decisions listed in Section 12.

## 1. Authority, scope, and versions

The six business-service boundaries in
[`02-revised-target-architecture.md`](./02-revised-target-architecture.md)
are authoritative for this work. That revised architecture supersedes the
earlier service proposal in
[`01-current-architecture-analysis.md`](./01-current-architecture-analysis.md),
which proposed `cart-service` and `order-service` and placed supplier data
under order. The approved target set is:

1. `product-service`
2. `inventory-service`
3. `supplier-service`
4. `user-service`
5. `procurement-service`
6. `payment-service`

Config Server, Eureka Discovery Server, and API Gateway are platform
applications, not additional business services. The existing monolith remains
an independent application and is not changed by these standards.

| Concern | Standard |
|---|---|
| Java | 21 |
| Spring Boot for new Cloud applications | 4.0.9 |
| Spring Cloud release train | 2025.1.4 |
| Maven | Use Maven Wrapper; pin the wrapper version consistently (currently 3.10.0) |
| Database | MySQL, with independent ownership and credentials per service |
| Frontend boundary | Angular keeps relative `/api` calls through the API Gateway |

The running infrastructure parent at
[`infrastructure/pom.xml`](../../infrastructure/pom.xml) uses Boot 4.0.9,
Java 21, and the 2025.1.4 Cloud BOM. It is a separate reactor containing only
the infrastructure applications. Do not add business services to that
reactor. The monolith remains on Boot 4.1.1 and its own Maven build.

Before each service is created, verify the approved Boot/Cloud combination
against the current official compatibility matrix, confirm resolved BOM
versions from Maven Central, and build/test the standalone application. Do
not override individual Spring Cloud dependency versions unless a documented
compatibility defect requires it and the platform owner approves.

## 2. Service ownership

The table names below are logical migration targets inferred from the JPA
model and architecture decisions. They are **not verified deployed MySQL
table names**. The source schema, constraints, and join-table names must be
inspected before designing a backfill; see
[`05-database-migration-inventory.md`](./05-database-migration-inventory.md).

| Service | Owned entities/data and logical tables | Public API responsibilities | Cross-service references and dependencies | Explicit exclusions |
|---|---|---|---|---|
| **Product** | `Produit`, `CategorieProduit`; likely `produit`, `categorie_produit`. Owns catalog metadata and pricing. | Preserve `/produits/**` and `/categories-produits/**` during compatibility migration; provide service-owned catalog DTOs and internal product lookup for authorized consumers. | Product IDs are scalar references in inventory and procurement. Procurement needs product identity and a price/name snapshot for historical lines. | Stock quantities, stock locations, reservations, movements, supplier data, invoices, and settlements. No JPA relation to inventory. |
| **Inventory** | Existing `Stock`; new inventory positions, movements, reservations, and low-stock alert records. Likely legacy table `stock`. | Preserve `/stocks/**` while migrating; own stock adjustment, availability, reservation, receipt, and movement contracts as those capabilities are approved. | Uses product IDs as values, not product foreign keys. Procurement may issue an idempotent receipt or reservation command under an approved workflow. | Product catalog/pricing, purchase invoices, supplier master data, user accounts, and payment records. Invoice creation/approval alone does not adjust physical stock. |
| **Supplier** | `Fournisseur`, `DetailFournisseur`, `SecteurActivite`, supplier-sector membership; likely `fournisseur`, `detail_fournisseur`, `secteur_activite`, plus an unverified join table. | Preserve `/fournisseurs/**` and `/secteurs-activite/**`; own supplier and sector APIs. Supplier-performance projections require approved definitions. | Procurement references supplier IDs. Optional supplier projections consume procurement/payment facts rather than joining their databases. | Purchase invoices, invoice lines, operators, stock, and settlements. |
| **User** | Existing `Operateur` staff profiles and future credential verifiers, roles, permissions, and security audit data; likely legacy table `operateur`. | Preserve `/operateurs/**` as a compatibility surface; own approved staff identity, authentication, and authorization contracts. | Procurement stores operator/approver IDs. Gateway and protected services validate the approved identity and authorization mechanism. | Invoice ownership, operator-to-invoice JPA collections, product/inventory/supplier data, and payment history. The current `password` field is not evidence that values are secure password hashes. |
| **Procurement** | `Facture`, `DetailFacture`, workflow/approval/audit state and service-owned CQRS projections/event state; likely legacy `facture`, `detail_facture`. | Preserve `/facture/**` legacy paths and behavior; own supplier-invoice and purchasing lifecycle contracts. Invoice details currently have no standalone legacy API. | References supplier, operator, and product IDs. Uses explicitly defined contracts with supplier, user, product, inventory, and payment. Publishes durable invoice lifecycle facts when event contracts are approved. | Supplier master data, products, physical stock, operator credentials, and settlement records. Do not reinterpret supplier invoices as customer sales orders. |
| **Payment** | `Reglement`, payment history, reconciliation/audit, and any local invoice reference projection; likely `reglement`. | Preserve `/reglements/**` legacy paths and settlement/revenue reporting contracts while migrating. | References invoice IDs as values and may consume procurement facts to validate invoice references. Emits settlement facts for approved consumers. | Invoice tables and procurement write state, supplier master data, card-provider workflows unless separately approved, and shared database joins. |

### Ownership rules

- One service owns each entity’s writes and schema migrations. Other services
  refer to it through stable scalar IDs and explicit APIs/events.
- Service-owned data may include a purpose-limited snapshot or projection.
  Record its source, freshness, and update semantics; it is not a second
  authority for the source entity.
- No cross-service JPA entity/repository imports, shared ORM model library,
  cross-service SQL, or foreign key spanning service ownership boundaries.
- A service does not mutate another service’s data through a shared database.
- The legacy `Produit`–`Stock`, `Operateur`–`Facture`,
  `Facture`–`Fournisseur`, `DetailFacture`–`Produit`, and
  `Reglement`–`Facture` mappings must be replaced with ownership-appropriate
  IDs/contracts during extraction. Preserve source relationship evidence for
  migration and compatibility.
- Keep supplier invoices, any future customer order, and any legal invoice
  document distinct unless product owners approve a unified domain model.

## 3. Project and Maven conventions

Each business service is an independently buildable and deployable Spring
Boot application. A service build must not require building or starting the
monolith, another business service, or the infrastructure reactor.

Recommended service layout:

```text
<service-name>/
  pom.xml
  mvnw
  mvnw.cmd
  .mvn/wrapper/
  src/
    main/
      java/<organization>/<domain>/
        api/
        application/
        domain/
        infrastructure/
      resources/
        application.properties
        db/migration/
    test/
      java/
      resources/
```

Use the repository’s established Java package/naming style where practical.
Keep web/API adapters, use-case orchestration, domain rules, and persistence or
remote-client adapters distinguishable. Do not introduce layers that only
rename or forward calls. A separately managed services directory or parent
build may be used if approved, but the infrastructure aggregator must remain
infrastructure-only and every service must retain its independent build
entrypoint.

Maven rules:

- In each standalone POM, inherit Spring Boot 4.0.9 and import the Spring Cloud
  2025.1.4 BOM when Cloud dependencies are used. Prefer the same Maven Wrapper
  version as infrastructure (3.10.0) unless changed by a platform decision.
- Use BOM-managed Spring Cloud versions; do not hardcode versions on Cloud
  starters.
- Keep only needed dependencies. A business service normally needs MVC,
  validation, persistence/driver, Config Client, and Eureka Client as
  required by its approved design. Add OpenFeign only for a service that
  makes declarative synchronous calls. Add Actuator, security, messaging,
  tracing, or metrics libraries only with explicit configuration and tests.
- No Gateway Server dependency belongs in a business service.
- Keep build plugins, compiler release, encoding, and test execution explicit
  and consistent. Do not skip tests in verification builds.
- At minimum, verify with the service’s wrapper using `clean verify` before
  merge. Produce a runnable artifact and document its local configuration.

## 4. Configuration and discovery

The implemented platform uses Config Server on port 8888, Eureka on 8761, and
Gateway on 8080. The Config Server reads the Git-backed `config-repo/`; the
Gateway and Discovery Server import remote properties with Spring Boot Config
Data. Config Server itself bootstraps locally and does not fetch config from
itself. Eureka disables self-registration and registry fetching. These
conventions are visible in
[`infrastructure/README.md`](../../infrastructure/README.md),
[`config-repo/discovery-server.yml`](../../config-repo/discovery-server.yml),
and [`config-repo/api-gateway.yml`](../../config-repo/api-gateway.yml).

For each business service:

- Set a stable, lowercase `spring.application.name` equal to its service ID.
- Use `spring.config.import=configserver:...` for Config Data. The import
  should be required in shared/test/production environments so the service
  fails clearly when required configuration is unavailable. Any optional
  local fallback must be explicit, safe, and documented.
- Register with Eureka and set the service URL/advertised address by
  environment-specific configuration. Do not assume host `localhost` is
  reachable from a container.
- Keep service discovery separate from public exposure. The Gateway’s
  discovery route locator remains disabled; add only reviewed, explicit
  routes as services are accepted.
- Config Git contains non-secret defaults only. Inject passwords, signing
  keys, tokens, and production credentials from a secret manager or protected
  deployment environment. Never log resolved secrets.
- Use environment-variable placeholders for ports and external URLs where
  the application needs local overrides. Document defaults and override
  names, but do not hardcode container hostnames into local defaults.

The current Gateway route is a local-development monolith fallback:
`/api/**` → `StripPrefix=1` → `MONOLITH_BASE_URL` (default port 8089).
It is not the final set of service routes. In Step 06 the Angular development
proxy forwards `/api/**` unchanged to port 8080; the Gateway strips the prefix
once. This reflects the current implementation and supersedes older design
text that describes Angular stripping `/api` itself. Preserve client paths
while explicit routes are added and verified service by service.

## 5. Database and migration standards

- Each service has a separately permissioned database and credentials. The
  target logical names in the revised architecture are `product_db`,
  `inventory_db`, `supplier_db`, `user_db`, `procurement_db`, and
  `payment_db`; actual physical provisioning remains a deployment decision.
- A service’s migrations are versioned, reviewed, repeatable, and owned by
  that service. Prefer a migration tool such as Flyway or Liquibase after
  confirming Boot 4 compatibility and team selection. Do not rely on
  Hibernate `ddl-auto=update` in shared or production environments.
- Apply schema changes forward through reviewed migrations. Test a clean
  database and an upgrade from the prior supported schema version.
- Before extracting any tables, inspect the actual deployed DDL, indexes,
  constraints, data types, join tables, nullability, and representative data.
  Existing `@Entity` declarations and the likely table names in the inventory
  are not proof of deployed schema.
- Preserve IDs where required by the legacy API, or create a documented,
  deterministic ID mapping and retain it for every foreign reference and
  rollback/reconciliation.
- Backfill from a consistent source snapshot. Compare row counts, distinct
  identifiers, relationship pairs, null/invalid values, monetary totals, and
  representative records before routing writes. Define reconciliation
  tolerances and rollback criteria with data owners.
- Do not perform permanent dual writes. For a controlled cutover, define
  ownership of writes, the synchronization window, conflict handling, and
  the point at which the source becomes read-only.
- Store quantities with an explicitly chosen unit and integer/decimal
  precision. The current `Stock.qte` is a whole-number field on `Stock`, not
  a verified per-product inventory-position quantity. Do not replicate it
  across products without a validated business mapping.

### Monetary representation

The source entities use Java `float` for `Produit.prix`, invoice and invoice
detail amounts, and settlement amounts; there is no explicit source currency
or JPA precision/scale. The deployed SQL types and monetary history have not
been verified.

**Recommended target, pending finance/data-owner approval:** use Java
`BigDecimal` for monetary values and MySQL `DECIMAL(19,4)` as the storage
baseline. Carry an explicit ISO 4217 currency code with each priced or
settled value when the domain requires it. Apply currency-specific rounding
only at documented business boundaries (for example, invoice line, invoice
total, or settlement), with a specified rounding mode; do not rely on binary
floating-point arithmetic or implicit database conversion. Confirm whether
the business requires more or less scale before implementation.

This recommendation does not authorize changing existing calculations,
rewriting historical totals, or converting the current `float` data without
source-column inspection, a deterministic conversion/rounding specification,
reconciliation, and approval. Preserve historic invoice-line and settlement
amounts; never recalculate them from today’s product price.

## 6. API, DTO, validation, and error conventions

### API and payloads

- The Gateway is the browser’s single API entrypoint. Keep the existing
  unversioned legacy routes and wire contracts as compatibility routes during
  extraction:
  - Product: `/produits/**`, `/categories-produits/**`
  - Inventory: `/stocks/**`
  - Supplier: `/fournisseurs/**`, `/secteurs-activite/**`
  - User/operator: `/operateurs/**`
  - Procurement: `/facture/**`
  - Payment: `/reglements/**`
- Preserve legacy methods, path spellings, query parameters, JSON field names,
  nullability, date formats, status codes, and response shapes until a
  deliberate, versioned change is accepted. Do not use this work to repair
  baseline behavior silently.
- New or incompatible public contracts should be versioned and documented.
  Internal service APIs should use explicit service-owned paths such as
  `/internal/v1/...`; do not expose them automatically through Eureka or the
  Gateway.
- Define request/response DTOs for every API. Do not serialize JPA entities
  as public or internal service contracts. IDs crossing service boundaries
  are scalar values, not nested foreign service entities.
- Use ISO-8601 dates/timestamps and define whether a field represents a date
  or an instant. Existing date-only `yyyy-MM-dd` contracts remain unchanged
  unless versioned.
- Use pagination/filtering consistently when list sizes require it; document
  parameter names, ordering, limits, and response envelope before adding
  paged behavior.
- Document API operations and schemas with OpenAPI. Verify SpringDoc and
  plugin/library compatibility with Boot 4 before selecting a version.

### Validation and errors

- Validate shape and simple constraints at the HTTP boundary with Jakarta
  Validation. Enforce business invariants in the domain/application layer as
  well; input annotations alone do not guarantee a valid state transition.
- Return a consistent RFC 9457 `ProblemDetail`-style error representation
  for new APIs, with a stable machine-readable code and safe detail. Never
  include stack traces, SQL, credentials, or internal topology in responses.
- Use standard meanings: 2xx for completed requests, 400 for malformed or
  invalid input, 401/403 for authentication/authorization failures, 404 for
  missing owned resources, 409 for state or idempotency conflicts, and 5xx
  for server/dependency failures. Preserve existing legacy response semantics
  at compatibility adapters until the change is explicitly approved.
- Map known domain errors centrally per service. Do not catch broad
  exceptions to return success-shaped defaults (for example, zero revenue,
  empty entities, or accepted commands).

## 7. Logging and operational diagnostics

- Use the application logging facade and parameterized messages. Include
  service name, environment, request/correlation ID, and trace context where
  configured.
- Log lifecycle and failure context, but do not log passwords, tokens,
  signing keys, full request bodies, or unnecessary personal/financial data.
- Keep log levels intentional: routine successful operations at normal
  levels, actionable dependency/domain failures with their causal context.
  Do not suppress errors or emit success-shaped fallbacks.
- Avoid high-cardinality IDs as metric labels. Metrics/tracing/exporter
  topology is not established by the current Config/Eureka/Gateway foundation
  and must be designed separately before relying on it in production.
- Health endpoints, if added, must be intentionally configured and exposed
  with appropriate network/security controls; do not expose configuration
  contents or sensitive diagnostics.

## 8. Service communication and consistency

### Synchronous REST and OpenFeign

Use synchronous REST when a caller needs a bounded response to proceed, such
as validating an owned reference or requesting a command result. Use
OpenFeign only where its declarative client model is useful; do not add a
Feign client for every service by default. Calls use stable service IDs and
Spring Cloud LoadBalancer/Eureka resolution, not hardcoded instance IPs.

Every client must have explicit connection and response timeouts, typed
failure mapping, correlation propagation, and authentication appropriate to
the approved security model. Retries must be bounded and limited to
transient, safe operations. Do not automatically retry a non-idempotent
command. Do not return fabricated successful fallbacks when a dependency is
unavailable. Circuit breakers/bulkheads may be added when justified and must
preserve visible failure semantics.

Avoid synchronous chains and reciprocal calls. In particular, payment must
not call procurement synchronously to validate each settlement while
procurement calls payment synchronously for the same workflow. Maintain
purpose-limited local projections from durable facts where eventual
consistency is acceptable.

### Events and asynchronous work

Use asynchronous events when a committed business fact must reach multiple
consumers, when a consumer can tolerate eventual consistency, or when
decoupling long-running lifecycle work. Do not introduce a broker simply to
replace a simple bounded query. No event broker is selected by this document.

Candidate facts already identified include procurement invoice lifecycle,
inventory movement/reservation, and payment settlement/reconciliation facts.
Before publishing one, define its owner, triggering committed state, schema,
retention, consumers, and compatibility policy.

Recommended naming convention: `<owner>.<aggregate>-<past-tense-fact>.v<major>`
(for example, `procurement.invoice-approved.v1`). Use an envelope with a
unique event ID, event type/schema version, UTC occurrence time, aggregate
identifier, correlation ID, and causation ID. Event payloads contain the
minimum immutable facts needed by consumers, not JPA entities or credentials.
Schema changes must be compatible within a major version or introduced as a
new version with a migration/consumer plan.

Use a transactional outbox or an equivalent durable publication mechanism
when a database commit and event publication must not diverge. Consumers must
be idempotent, track processed event IDs, tolerate duplicates/out-of-order
delivery as specified, and expose failed-message handling. Commands that may
be retried (especially inventory or payment-affecting commands) require a
caller-supplied idempotency key and a documented duplicate response.

### Distributed workflows

MySQL transactions end at a service boundary. Do not claim cross-service
ACID transactions or use distributed database transactions as the default.
Model multi-service work as an explicit workflow/Saga with durable states,
timeouts, retry rules, compensation (for example, reservation release or
settlement reversal), and visible handling for partial failure. Define which
read models are eventually consistent and what the user sees while pending.

Axon CQRS is specified for procurement only by the revised architecture.
Keep its command/write model and query projections procurement-owned. Axon
does not itself decide cross-service delivery or consistency. Event-store
placement, operational ownership, backup/replay, and integration with any
cross-service transport remain open decisions.

## 9. Testing and integration acceptance

Every service change is expected to include tests at the layer that protects
the behavior:

1. **Unit/domain tests:** business rules, validation boundaries, rounding,
   state transitions, and error cases without network/database dependence.
2. **Persistence integration tests:** repository mappings and migrations
   against MySQL-compatible infrastructure; use isolated disposable
   databases/containers where available. Do not substitute H2 for MySQL
   behavior without documenting the differences.
3. **HTTP/service tests:** endpoint methods, paths, DTOs, validation,
   status/error bodies, and serialization/date compatibility.
4. **Consumer/provider contract tests:** verify service-owned DTOs, error
   behavior, and version compatibility for each inter-service API. Cover
   event schema compatibility and duplicate delivery where applicable.
5. **Gateway routing tests:** assert the exact route, path-strip count,
   method/query/body/header forwarding, upstream status/body, and that no
   unreviewed discovery routes are exposed.
6. **End-to-end tests:** exercise representative Angular-to-Gateway-to-service
   workflows, including failure and retry outcomes, without replacing unit
   and contract tests.
7. **Migration verification:** test repeatability, source-to-target mapping,
   identifiers/relationships, row counts, and approved monetary/quantity
   reconciliations before cutover.

The existing monolith baseline has 38 tests, with one context startup error
because a Testcontainers test could not find a Docker environment; see
[`03-baseline-validation.md`](./03-baseline-validation.md). Ensure CI/runtime
environments can execute container-dependent tests or clearly report that
coverage as unavailable. Do not claim a green integration baseline when
Docker-backed checks were skipped or blocked.

### Required integration acceptance before routing a service

- Standalone `clean verify` passes with the approved Java/Maven versions.
- The application loads required external configuration, starts, and
  registers with Eureka using its intended service ID.
- Its database is isolated, migrations apply cleanly, and production schema
  management does not use implicit `ddl-auto=update`.
- Owner API and event contracts are documented and tested.
- Representative legacy requests preserve required behavior, including
  statuses, payloads, errors, and dates.
- Gateway route is explicit, has correct precedence, and passes exact path and
  query values. Unextracted routes still reach the monolith.
- Data backfill and reconciliation meet owner-approved thresholds; rollback
  and source write-freeze steps are documented.
- Security, secrets, logs, health endpoints, and failure behavior have been
  reviewed for the environment in which the route will run.

### Pull-request checklist

- [ ] Change belongs to the owning service and does not cross-write another
  service’s data.
- [ ] API, event, database, and configuration changes are documented and
  version-compatible.
- [ ] No JPA entities or repositories cross service boundaries.
- [ ] No secret, password, token, personal/financial payload, or local
  environment file is committed or logged.
- [ ] Validation, errors, idempotency, timeout/retry behavior, and
  authorization are explicit for changed operations.
- [ ] Unit, integration, and applicable contract/Gateway/migration tests pass;
  blocked tests and environment limitations are stated.
- [ ] Schema migration is reviewed, forward-compatible where required, and
  tested from both an empty and supported prior schema.
- [ ] Observability/diagnostic changes avoid sensitive values and
  high-cardinality metric labels.
- [ ] README/OpenAPI/configuration examples match the implementation.
- [ ] No unrelated monolith, Angular, infrastructure, or schema changes are
  included unless the change is explicitly approved for that scope.

## 10. Environment, secrets, and security

- Keep application configuration externalized. Commit safe examples and
  defaults only; do not commit `.env`, passwords, tokens, private keys,
  production connection strings, or real personal/financial data.
- Use separate credentials per service/database with least privilege.
  Rotate credentials through the approved secret-management process.
- Eureka is discovery, not authentication or authorization. Gateway route
  exposure must be explicit. Do not expose Config Server, Eureka dashboard,
  or Gateway publicly without approved controls.
- The current platform foundation is local-development infrastructure and
  does not implement business-service authentication. Before public
  deployment, approve the identity provider/token model, key distribution,
  issuer/audience policy, service-to-service identity, roles, and
  authorization enforcement at both Gateway and service boundaries.
- Never trust caller-supplied identity headers as proof of identity.
  Downstream services must validate credentials/tokens and authorize sensitive
  actions according to the agreed model.
- Use environment-specific, resolvable URLs. `localhost` inside a container
  refers to that container, not another application or the host.

## 11. Git workflow

No repository-level `CONTRIBUTING.md` or general branch/review policy was
found during inspection. The following is therefore a **recommended team
workflow**, not a claim about an existing repository rule:

- Keep `main` releasable. Use short-lived branches such as
  `feature/<service>/<work-item>` and `fix/<service>/<work-item>`.
- Make focused commits with descriptive imperative messages. Do not commit
  secrets, generated build outputs, local database files, or unrelated
  formatting churn.
- Merge through a pull request. Require at least one reviewer familiar with
  the affected service boundary and passing required build/test/security
  checks; increase review coverage for data migrations, auth, and event
  contract changes.
- Include migration/rollback notes for schema or data changes and rollout
  notes for externally visible API/event changes.
- Coordinate versioned contract and migration changes with consumer owners.
  Do not merge a producer-breaking contract before consumers have a compatible
  release path.
- Use the PR checklist in Section 9. Record intentional behavior changes
  separately from compatibility-preserving extraction.

## 12. Open decisions and owners

The roles below are accountable owner categories; named people and approval
dates have not been supplied.

| Decision still required | Proposed owner(s) | Why it blocks or constrains implementation |
|---|---|---|
| Interpret `Stock.qte`, define stock/location/product-position semantics, units, and the behavior of legacy product-to-stock assignment | Inventory and Product owners; purchasing/business owner | Current model has `qte` on `Stock` and a product-to-stock reference; it does not establish per-product quantities, reservations, or receipt semantics. |
| Inspect deployed MySQL DDL/data and confirm physical tables, columns, join tables, constraints, indexes, IDs, and data quality | DBA/data owner with each service owner | Existing inventory is inferred from entities; there is no checked-in versioned source schema. |
| Approve currency, decimal precision/scale, rounding points/mode, and historical `float` conversion/reconciliation | Finance/business owner and data owner | No currency is present in the inspected model; values are Java floats, and deployed SQL types are unknown. This document’s `BigDecimal`/`DECIMAL(19,4)` is a proposal only. |
| Decide identity/token implementation, operator credential migration, roles, key rotation/distribution, and service-to-service authorization | Security owner and User Service owner | Current operator password values have no verified hash or authentication implementation. |
| Select a durable cross-service event transport and standard delivery/retention/operations model | Platform/architecture owner with Procurement, Inventory, Payment, Supplier owners | Candidate events exist, but no broker is approved; do not add event infrastructure before this decision. |
| Select Axon event-store/persistence deployment and define backup, replay, retention, and schema evolution | Procurement owner and platform/data owner | Revised architecture requires CQRS in Procurement but does not choose Axon Server versus procurement-owned persistence. |
| Define invoice lifecycle, approval rules, and whether confirmed goods receipt updates stock; separate purchasing invoices from any future sales order | Procurement/business owner with Inventory owner | Existing `Facture` behavior is incomplete and does not define approval, receiving, or customer orders. |
| Assign recovery-rate reporting ownership and define its cross-service projection/query and error semantics | Procurement and Payment owners; business/reporting owner | Existing report reads invoice and settlement data and returns zero on caught errors; the target must not recreate shared-database joins or silently redefine behavior. |
| Define supplier-performance metrics, authoritative dates/facts, disputes, and projection freshness | Supplier owner and procurement/payment/business owners | These metrics are new; no current implementation or accepted formula was identified. |
| Approve stable-ID/backfill policy and API compatibility/versioning changes, including `stockId` response semantics | API/architecture owner and service owners | Existing API DTOs expose legacy IDs and a stock ID; boundaries and data migration can change how these values are sourced. |
| Select and operationalize the service database migration tool and supported rollout/rollback rules | Platform/data owner | The monolith uses Hibernate `update`; no migration history exists. Flyway/Liquibase is recommended but not selected. |
| Approve branch protection, required CI checks, reviewer count, and merge strategy | Repository/team maintainers | No general contribution or Git review policy was found in the repository. |

## 13. Product Service implementation prerequisites

Do not begin Product Service implementation until its owner and the relevant
cross-service owners have completed or explicitly accepted these gates:

1. **Boundary and data ownership:** Confirm Product owns product/category
   catalog and pricing only. Inventory owns quantities, locations, movements,
   reservations, and low-stock state. Agree how legacy `stockId` fields and
   `PUT /produits/{id}/stock/{stockId}` are represented through a compatibility
   adapter or inventory-owned operation; never preserve a cross-service JPA
   relation.
2. **Source data evidence:** Capture actual MySQL DDL and a consistent data
   snapshot. Verify the product/category table and key names, constraints,
   category references, product-stock relationships, null/duplicate values,
   identifiers, and data volumes. Preserve IDs or approve a complete mapping.
3. **Money and catalog rules:** Agree the currency contract, precision/scale,
   rounding policy, price-change semantics, and historical product/line
   snapshots before changing the current `float` representation or pricing
   calculations. Define product active/deleted behavior and whether historical
   references remain resolvable.
4. **API baseline:** Use
   [`04-existing-api-contracts.md`](./04-existing-api-contracts.md) to create
   characterization/contract tests for `/produits/**` and
   `/categories-produits/**`, including fields, status codes, validation,
   errors, and legacy stock compatibility. The existing product category is
   not writable through the current product JSON contract; adding this
   capability requires explicit API design and acceptance.
5. **Service contract:** Define public legacy-compatible and internal
   product lookup DTOs needed by Procurement. Confirm IDs, code uniqueness,
   catalog state, price/currency/version fields, error mapping, and batch
   lookup needs. Do not expose entities.
6. **Standalone build/configuration:** Create a separate Spring Boot 4.0.9 /
   Spring Cloud 2025.1.4 service build using Java 21, Config Data, and Eureka
   conventions. Do not add it to the infrastructure reactor. Keep database
   credentials outside Config Git.
7. **Persistence and migration:** Select the approved migration tool, create
   Product-owned schema migrations, and test both an empty database and the
   supported prior version. Backfill products/categories and prove ID,
   category, value, and relationship parity without relying on unverified
   Hibernate-generated names.
8. **Acceptance and cutover:** Pass service, persistence, API contract, and
   Gateway tests. Compare source/target rows and representative responses.
   Route only the accepted product/category paths to the service; keep other
   paths on the monolith, document rollback, and stop monolith writes to
   Product-owned records only at the approved ownership cutover.

No prerequisite authorizes implementing Inventory behavior, changing the
monolith or Angular API, changing database schemas in the monolith, or
changing historical business calculations as part of Product Service
extraction.
