# Product Service Extraction Design

**Status:** Approved design. The standalone Product Service is implemented;
source data transfer and Gateway cutover remain deferred. This design follows
[`07-microservices-development-standards.md`](./07-microservices-development-standards.md)
and the revised six-service boundaries in
[`02-revised-target-architecture.md`](./02-revised-target-architecture.md).

## 1. Scope and evidence

Product Service owns the product catalog, categories, and catalog price.
Inventory Service owns stock, assignments/positions, quantities, movements,
reservations, and alerts. Procurement owns supplier invoices and invoice
lines. Product identifiers cross those boundaries as values and API/event
contracts, never as cross-service JPA relationships.

The design uses source evidence from:

- Product domain source under
  [`backend/src/main/java/org/example/achat/produit/`](../../backend/src/main/java/org/example/achat/produit/):
  entities, repositories, services, controllers, DTOs, exceptions.
- Product and category tests under
  [`backend/src/test/java/org/example/achat/produit/`](../../backend/src/test/java/org/example/achat/produit/).
- Product use from procurement in
  [`FactureServiceImpl.java`](../../backend/src/main/java/org/example/achat/facture/services/FactureServiceImpl.java)
  and [`DetailFacture.java`](../../backend/src/main/java/org/example/achat/facture/entities/DetailFacture.java).
- Cross-domain development seeding in
  [`DevelopmentDataSeeder.java`](../../backend/src/main/java/org/example/achat/config/DevelopmentDataSeeder.java),
  with fixtures [`products.json`](../../backend/src/main/resources/seed/products.json)
  and [`categories.json`](../../backend/src/main/resources/seed/categories.json).
- The baseline API inventory
  [`04-existing-api-contracts.md`](./04-existing-api-contracts.md) and data
  inventory [`05-database-migration-inventory.md`](./05-database-migration-inventory.md).
- Frontend paths/contracts in
  [`ProduitService`](../../frontend/src/app/core/services/produit.service.ts),
  [`CategorieProduitService`](../../frontend/src/app/core/services/categorie-produit.service.ts),
  [`produit.model.ts`](../../frontend/src/app/core/models/produit.model.ts),
  and [`categorie-produit.model.ts`](../../frontend/src/app/core/models/categorie-produit.model.ts).
- Current Gateway fallback in
  [`api-gateway.yml`](../../config-repo/api-gateway.yml) and the Angular proxy
  [`proxy.conf.json`](../../frontend/proxy.conf.json).

### Database evidence and limitations

The checked-in database inventory identifies `produit` and
`categorie_produit` as **likely Hibernate table names**, with `Long` identity
IDs, not as verified deployed identifiers. Neither entity declares
`@Table`, explicit column names, or explicit join-column names. There are no
checked-in SQL schema files or Flyway/Liquibase migrations in the backend.
The host's MySQL port 3306 was listening during inspection, but no database
metadata query was made; therefore deployed table/column definitions,
constraints, indexes, data, and schema drift remain unknown. The `.env` file
was not inspected.

Treat every table and FK name below as a proposal until a read-only schema
inspection and data-quality review are approved and completed.

## 2. Existing Product domain

### Product and category fields

| Domain record | Source fields and types | Source constraints / behavior |
|---|---|---|
| `Produit` | `idProduit: Long` (identity); `codeProduit: String`; `libelleProduit: String`; `prix: float`; `dateCreation: Date`; `dateDerniereModification: Date` | `codeProduit` and `libelleProduit` use `@NotBlank`; `prix` is primitive `float` with `@NotNull` and `@PositiveOrZero`; dates use `@Temporal(DATE)`. No explicit unique constraint or length/precision is declared. |
| `CategorieProduit` | `idCategorieProduit: Long` (identity); `codeCategorie: String`; `libelleCategorie: String` | Both text fields use `@NotBlank`; no explicit unique constraint or length is declared. |

`Produit.prix` is monetary data represented as Java binary floating point.
There is no currency field or explicit JPA precision/scale. The deployed SQL
type and historical values have not been verified. The standards document
recommends `BigDecimal` and MySQL `DECIMAL(19,4)` as a **pending approval**
baseline, with currency-specific rounding defined by Finance. This is not
authorization to change current arithmetic or reinterpret old values. Product
price migration must use an approved deterministic conversion and preserve
historical invoice-line totals independently.

### Relationships and other-domain references

- **Product to category:** `Produit.categorieProduit` is a lazy `ManyToOne`;
  `CategorieProduit.produits` is its inverse collection. Both are within
  Product Service and may remain a same-database relation. Product response
  exposes `categorieProduitId`.
- **Product to stock:** `Produit.stock` is a lazy `ManyToOne` to `Stock`;
  `Stock.produits` is the inverse collection. This is a current monolith
  relationship, not Product-owned inventory state. Inventory must own the
  product-to-stock/location assignment and quantities. Product DB must not
  contain a JPA relation or FK to Inventory.
- **Invoice details to product:** `DetailFacture.produit` is a `ManyToOne`
  from a Procurement-owned entity to Product. Product's inverse detail
  collection is commented out. In the target, Procurement stores the product
  ID and immutable line snapshots; there is no Product-side cross-database
  relationship.
- **Supplier and user:** No Product entity relationship to Supplier or User
  was found. Procurement owns the combination of supplier, product and
  operator references.
- **Seed data:** The monolith's single `DevelopmentDataSeeder` seeds stocks,
  categories, products, invoices and invoice details together. Product
  fixtures identify stock by `stockLabel` and category by `categoryCode`.
  This loader cannot be copied as-is into Product Service. Split fixtures by
  data owner and represent Inventory/Procurement references through their
  approved contracts or separate migration fixtures.

### Existing behavior and gaps

- Product list/read/create/update/delete are repository-backed. Product
  creation saves the entity; it does not resolve or validate a category or
  stock association from JSON.
- Product update copies code, label, price and `dateDerniereModification`
  from the body to the existing entity. It does not update `dateCreation`,
  stock, or category.
- Stock assignment loads both Product and Stock and persists the JPA relation.
  It belongs to Inventory after extraction, even though its legacy URL starts
  with `/produits`.
- Product response DTO is a scalar projection containing `stockId` and
  `categorieProduitId`; it does not serialize entity relationships.
- Product JSON writes ignore `stock` and `categorieProduit` (`@JsonIgnore`).
  The Angular product form sets stock through the separate assignment call;
  it does not send category assignment. Product category CRUD is separate.
- Category create/update/delete/read is straightforward repository CRUD. No
  duplicate-code check, referential delete policy, pagination, sorting
  contract, or query parameter was found.
- Product deletion only checks whether the Product row exists before calling
  repository delete. Database references from invoice detail or Inventory
  may cause an integrity error, but the deployed constraints and mapped
  response are unknown. Do not silently add cascade deletion.
- A primitive `float` with `@NotNull` cannot by itself prove a JSON property
  was present; absent-property behavior depends on deserialization. Add a
  characterization test before defining the exact missing-price compatibility
  behavior.

## 3. Proposed Product Service boundary and persistence

### Ownership

Product Service owns product and category catalog records, their lifecycle,
catalog validation, and catalog price. It does not own stock quantities,
stock locations, reservations, movements, suppliers, invoices, invoice lines,
operators, or settlements.

Keep the product/category association local to Product Service. Replace
cross-domain mappings with scalar IDs and contracts:

| Source concept | Target owner | Product Service treatment |
|---|---|---|
| Product category | Product | Same-database category relation / FK is allowed. |
| Product stock assignment | Inventory | Not a Product entity relation or Product-owned write. Legacy response compatibility may use an explicitly non-authoritative projection. |
| Invoice-line product reference and historical pricing | Procurement | Procurement stores `productId` and historical line/name/price/currency/discount snapshots; Product has no invoice-line relation. |
| Supplier/user references | Supplier/User/Procurement as applicable | No Product-owned data or relationship. |

### Proposed database

Provision a separately permissioned Product Service database, logical name
`product_db` per the approved standards. The following is an initial logical
schema, not a claim about the existing physical schema:

| Proposed table | Proposed key and columns | Constraints / notes |
|---|---|---|
| `product_categories` | `category_id BIGINT` PK; `code` and `label` non-null strings | Add length limits only after source-data inspection. Do not add a unique `code` constraint until existing duplicates and the business identity rule are resolved. |
| `products` | `product_id BIGINT` PK; `code`, `label`; `price`; date-only `created_on`, `modified_on`; nullable `category_id` FK to `product_categories` | `category_id` is same-service. Recommended future price type is `DECIMAL(19,4)` only after currency/scale approval. No stock FK, invoice FK, or cross-service JPA association. |
| Optional `product_inventory_compatibility` projection | `product_id` and nullable `stock_id` values, with `product_id` identifying the projected Product; no FK to Inventory | Only if legacy `ProduitResponse.stockId` must remain populated. It is a read projection, not the authority for assignment; ownership/update source and consistency mechanism require approval. |

Use the source IDs and field meanings in service-owned entities but prefer
clear service naming (`Product`, `ProductCategory`) and explicit
`@Table`/column declarations in the new service. Existing French JSON names
remain in compatibility DTOs. The source schema may use a different physical
name; map it during the approved migration rather than assuming it.

Use versioned, Product-owned database migration scripts (tool to be selected
per standards) in `src/main/resources/db/migration/`. Production schema
changes must not rely on Hibernate `ddl-auto=update`. Apply category schema
before product schema and define the local category FK after source data is
validated. No Product migration may create or modify monolith tables.

### Application structure

Each proposed package remains within a standalone `product-service` build;
it is not added to the infrastructure Maven reactor.

```text
product-service/
  pom.xml
  mvnw
  mvnw.cmd
  .mvn/wrapper/
  src/main/java/org/example/achat/product/
    ProductServiceApplication.java
    api/
      ProductController.java
      ProductCategoryController.java
      dto/
        ProductRequest.java
        ProductResponse.java
        ProductCategoryRequest.java
        ProductCategoryResponse.java
        InventoryProductReferenceResponse.java
      error/
        ProductExceptionHandler.java
    application/
      ProductService.java
      ProductCategoryService.java
      InventoryCompatibilityPort.java
    domain/
      Product.java
      ProductCategory.java
      ProductNotFoundException.java
      ProductCategoryNotFoundException.java
    infrastructure/
      persistence/
        ProductRepository.java
        ProductCategoryRepository.java
        ProductInventoryProjectionRepository.java   # only if approved
      inventory/
        InventoryCompatibilityAdapter.java            # if required
  src/main/resources/
    application.properties
    db/migration/
  src/test/java/
```

Keep HTTP DTOs separate from JPA entities. Controllers map request DTOs to
application operations; repositories remain Product-owned. The optional
Inventory adapter/projection is a compatibility boundary and should be
omitted if the team accepts changing or relocating the legacy stock fields
and route as a separately versioned behavior change.

### Platform configuration

Use Java 21, Spring Boot 4.0.9, and Spring Cloud BOM 2025.1.4 as approved for
new Cloud applications. Product Service is an independent Maven build and is
not a module of `infrastructure/pom.xml`.

Recommended local configuration shape:

```properties
spring.application.name=product-service
spring.config.import=configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

Put Product-specific datasource URL/name, credentials references, migration
settings, service port, Eureka client URL/advertised address, and operational
settings in a Product Config Server document such as `product-service.yml`
plus deployment environment overrides. Do not store credentials in the
Config Git repository. The import is required outside local development;
any local optional fallback must be deliberately configured and documented.
Assign the Product Service port through the platform's service-port plan;
no business-service port has been approved in the inspected documents.

Register `product-service` with Eureka using its stable service ID. Eureka
does not provide authorization. The Gateway remains the public entrypoint and
only exposes explicitly reviewed Product and Category routes.

### Health and API documentation

Add Actuator only when intentionally configured. Expose only health/liveness/
readiness needed by the deployment, keep detail disclosure restricted, and
do not expose configuration contents. Configure readiness to reflect
database/migration readiness without leaking credentials or connection
details. The final exposed paths/security policy require platform approval.

Publish OpenAPI documentation for public compatibility APIs and the internal
Inventory lookup contract. Verify a Boot 4-compatible SpringDoc version
before adding it. Do not expose internal contracts accidentally through
public API docs or Gateway routes.

## 4. API compatibility matrix

The source controllers are rooted at `/produits` and
`/categories-produits`; the Gateway removes `/api` once. Therefore the
Product Service HTTP paths remain the existing paths below, with no `/api`
context prefix. The Angular app continues to call `/api/produits` and
`/api/categories-produits`; no frontend URL change is required.

### Product endpoints

Request body summary:

```json
{
  "codeProduit": "PROD-001",
  "libelleProduit": "Example product",
  "prix": 12.5
}
```

The current entity also accepts `dateCreation` and
`dateDerniereModification`; responses use `ProduitResponse` and do not include
those dates. `stock` and `categorieProduit` JSON properties are ignored.

| Method / path after Gateway strip | Query/path inputs | Existing request body | Existing success | Existing errors and proposed compatibility |
|---|---|---|---|---|
| `GET /produits` | None | None | 200; array of `{idProduit, codeProduit, libelleProduit, prix, stockId, categorieProduitId}` | No explicit resource error mapping. Keep array and fields. Order is not specified. |
| `GET /produits/{produitId}` | Long path ID | None | 200; same `ProduitResponse` fields | 404 `ProblemDetail` for missing Product, detail `Produit not found with id: <id>`. Preserve response/status in compatibility API. |
| `POST /produits` | None | Entity JSON: nonblank `codeProduit`, nonblank `libelleProduit`, nonnegative `prix`; optional dates; ignored stock/category JSON | 201; created `ProduitResponse` | 400 for bean validation/deserialization failures; validation body is framework-generated, not a stable shared shape. Preserve legacy behavior first; characterize absent `prix`. |
| `PUT /produits/{produitId}` | Long path ID; path ID overwrites request entity ID | Same entity JSON as create | 200; updated `ProduitResponse` | 400 validation/deserialization; 404 missing Product `ProblemDetail`. Update copies code, label, price and modification date only; stock/category/dateCreation are not changed. |
| `DELETE /produits/{produitId}` | Long path ID | None | 204; empty body | 404 missing Product `ProblemDetail`. If existing external references block delete, response is not explicitly mapped; characterize and decide 409/soft-delete separately. |
| `PUT /produits/{produitId}/stock/{stockId}` | Two Long path IDs | None | 200; `ProduitResponse` after assigning stock | 404 if Product or Stock missing, through product advice. **Not a Product-owned write in the target**; route to an Inventory-owned compatibility operation or a specifically approved facade. |

There are no product query parameters in the current controller. Product API
response includes a nullable `stockId`; its target source is an unresolved
cross-service compatibility concern, described in Section 5.

### Category endpoints

Request body:

```json
{
  "codeCategorie": "CAT-OFFICE",
  "libelleCategorie": "Office supplies"
}
```

Both fields are nonblank. Response is
`{idCategorieProduit, codeCategorie, libelleCategorie}`.

| Method / path after Gateway strip | Query/path inputs | Request body | Existing success | Existing errors and proposed compatibility |
|---|---|---|---|---|
| `GET /categories-produits` | None | None | 200; array of category response DTOs | No explicit resource error mapping. Keep the array and fields. |
| `GET /categories-produits/{categorieProduitId}` | Long path ID | None | 200; category response DTO | 404 `ProblemDetail` for missing category, detail `Categorie produit not found with id: <id>`. |
| `POST /categories-produits` | None | `codeCategorie`, `libelleCategorie` | 201; created category DTO | 400 validation/deserialization failure; existing body is framework-generated. |
| `PUT /categories-produits/{categorieProduitId}` | Long path ID; path ID overwrites entity ID | `codeCategorie`, `libelleCategorie` | 200; updated category DTO | 400 validation/deserialization; 404 missing category `ProblemDetail`. |
| `DELETE /categories-produits/{categorieProduitId}` | Long path ID | None | 204; empty body | 404 missing category `ProblemDetail`. Deleting a category referenced by products may fail on DB integrity; deployed constraints/error mapping are unknown. Define a safe 409/deactivation policy without changing legacy behavior silently. |

There are no category query parameters. The Category controller has no
controller-local `@CrossOrigin`; browser traffic is expected through the
same-origin Gateway/proxy. No category-specific backend tests were found.

### Compatibility policy

Implement compatibility DTOs with the existing JSON field names, nullable
fields, numeric JSON shape, date behavior, and status codes. Use a legacy
adapter if the internal domain/entity names differ. Do not return JPA entities
from the new service. New fields, category assignment on Product writes,
duplicate-code rejection, pagination, a new delete policy, and normalized
ProblemDetail bodies are potential improvements, not part of behavior-
preserving extraction unless versioned and approved.

Validation errors are not currently normalized, while not-found errors use
ProblemDetail. A new API may use the common error convention in
[`07-microservices-development-standards.md`](./07-microservices-development-standards.md),
but the legacy routes should preserve the observed error contract until a
deliberate compatibility change is approved.

## 5. Inventory boundary and compatibility design

### Inventory-facing Product reference

Inventory needs only to establish that a product ID exists and is eligible for
the inventory operation; it should not copy Product's JPA entity or own
catalog price. Recommended internal lookup:

```http
GET /internal/v1/products/{productId}
```

Example response:

```json
{
  "productId": 123,
  "code": "PROD-001",
  "active": true,
  "catalogVersion": 1
}
```

Only the stable ID and active/existence state are required for an inventory
position; code/version are optional operational context. Product Service
returns an explicit DTO and an appropriate 404 for an unknown product. The
exact active/deleted lifecycle and whether Inventory calls this API
synchronously or maintains an event-fed local reference projection require
agreement. No stock quantity, stock movement, reservation, or stock ID
management is added to Product Service.

Procurement has a separate need for product name/code and an agreed
price/currency snapshot when creating invoice lines. That contract must
specify currency and catalog/price version and must not recalculate
historical lines. It should not be widened into Inventory's contract without
a consumer need.

### Legacy stock route and `stockId` response

The current product endpoint `PUT /produits/{id}/stock/{stockId}` persists the
monolith's Product-to-Stock association. The frontend calls it from the
product edit flow, and `ProduitResponse.stockId` is used to prefill the
selection. Moving this operation to Product Service would violate ownership.

Recommended compatibility design:

1. Inventory owns the product-to-stock/location assignment and its
   authoritative records.
2. Keep the legacy `PUT /produits/{id}/stock/{stockId}` URL available through
   a dedicated compatibility operation owned by Inventory or an explicitly
   approved API facade. If routed at Gateway, its more-specific route must
   take precedence over the general Product route and the `/api` prefix must
   still be removed only once.
3. If legacy product reads must continue returning `stockId`, source it from
   Inventory through an explicitly designed, non-authoritative compatibility
   read model or a deliberate composition API. Do not add an Inventory JPA
   relationship or authoritative `stockId` column to Product.
4. Because an event transport is not yet selected, the projection refresh
   mechanism and consistency/failure behavior are a blocking decision. Avoid
   a per-product synchronous Inventory call from every product list response
   unless owners explicitly accept its latency and availability coupling.
5. Before this route and response behavior are cut over, confirm whether the
   domain actually treats a stock as a warehouse/location, whether a product
   can have multiple stock positions, and how the legacy single `stockId`
   represents the target model.

The fallback `/api/**` currently targets the monolith. Do not route the
general product prefix to Product Service until the assignment subroute and
`stockId` read behavior have an accepted compatibility solution; otherwise
the Angular product form may regress even if Product CRUD works.

## 6. Database migration plan

No schema or data changes are authorized by this design.

### Source and target ownership

- **Migrate to Product Service:** Product and Product Category rows, including
  their internal association.
- **Do not migrate into Product Service:** Stock rows and stock quantities
  (Inventory); invoices and invoice details (Procurement); supplier/user
  records; settlements.
- **External references to preserve:** Product IDs referenced by invoice
  details; product-to-stock assignments for Inventory; category IDs on
  products. These source references may be actual database constraints or
  only entity mappings; verify them against deployed metadata.
- Inventory's target may need position/assignment records keyed by `productId`
  and its own location/stock ID. Do not create these from guessed source
  semantics.

### Pre-migration discovery

1. Obtain an approved read-only snapshot/metadata session against the correct
   source database; never use an unreviewed local `.env` or expose credentials
   in logs or documentation.
2. Capture actual table/column names and types, PK/FK constraints, indexes,
   nullability, unique constraints, identity state, and product/category/
   stock/invoice-detail counts.
3. Export product/category rows, category links, product-stock links, and
   invoice-detail product references. Record orphans and duplicate product/
   category codes. Do not assume JPA mappings imply enforced database FKs.
4. Establish stable identifier expectations and consumer impact. The
   preferred migration preserves `idProduit` and `idCategorieProduit` as
   Product Service IDs because those values are present in legacy APIs and
   Procurement line references. If remapping is unavoidable, produce a
   durable old-to-new mapping and coordinate Procurement/Inventory references
   before any route cutover.
5. Approve money conversion and stock compatibility policies. Product
   `prix` currently uses `float`; historical invoice details independently
   carry `prixTotalDetail` and discount values. Do not recalculate those
   details from the migrated catalog.

### Copy and verification

1. Create Product-owned tables with reviewed, versioned migrations in an
   empty Product database. Validate migrations on the approved MySQL version.
2. Load categories first, preserving their IDs and fields. Load products next,
   preserving IDs, scalar values, date-only semantics, and same-service
   category references. Do not copy a cross-service stock FK into the Product
   database.
3. Load or initialize the Inventory-owned stock assignment from a separately
   approved mapping. If preserving legacy `stockId` reads during transition,
   initialize the clearly marked compatibility projection without treating
   it as authoritative.
4. Do not drop or rename monolith tables. Keep the monolith authoritative
   until parity is accepted and a cutover is approved. Do not dual-write from
   the monolith and Product Service as a standing strategy.
5. Compare source/target row counts and ID sets; field-level values; category
   references; product-stock assignment pairs; product IDs referenced by
   invoice details; duplicate codes; null/invalid fields; date behavior; and
   price values using the approved deterministic decimal conversion and
   reconciliation tolerance.
6. Produce signed-off reconciliation output, a repeatable migration record,
   cutover order, and rollback criteria. Preserve an immutable source snapshot
   for audit and recovery.

### Duplicates, invalid data, and rollback

- Do not silently deduplicate rows or replace IDs. Preserve source rows in a
  quarantine/report table or migration artifact, then obtain a data-owner
  resolution for each collision or invalid relationship.
- Do not add unique-code constraints until duplicates are measured and
  Product owners define whether code is globally unique, case-sensitive, and
  immutable. Enforce the approved rule in both migration validation and the
  service.
- Do not cascade-delete products/categories to repair references. Define a
  409, archive/deactivation, or remediation process after confirming current
  database constraints and consumer expectations.
- Before route cutover, rollback may route reads/writes back to the monolith
  while the service remains non-authoritative or read-only. After Product
  Service accepts writes, do not simply route back to stale monolith data:
  stop writes and reconcile/replay service-owned changes or use a
  pre-approved reverse-sync plan. Keep the monolith tables; no destructive
  schema change is part of extraction.

## 7. Gateway integration plan

The current Gateway configuration in `config-repo/api-gateway.yml` has one
route, `monolith-fallback`, matching `/api/**`, applying `StripPrefix=1`, and
targeting `MONOLITH_BASE_URL` (default `http://localhost:8089`). The current
Angular proxy targets port 8080 and does not rewrite `/api`.

After Product Service is independently built, registered, migrated, and
accepted, add explicit routes conceptually equivalent to:

| Route | Predicate | Destination | Filter / precedence |
|---|---|---|---|
| Product stock-assignment compatibility route (if required) | More specific `PUT /api/produits/{productId}/stock/{stockId}` | Inventory Service | Highest precedence among Product-prefix routes; preserve the legacy path expected by the Inventory compatibility adapter; remove `/api` exactly once. |
| Product | `/api/produits/**` | `lb://product-service` | Explicit order higher priority than fallback; `StripPrefix=1` exactly once. |
| Product Category | `/api/categories-produits/**` | `lb://product-service` | Explicit order higher priority than fallback; `StripPrefix=1` exactly once. |
| Monolith fallback | `/api/**` | `${MONOLITH_BASE_URL:http://localhost:8089}` | Lower priority than both Product routes; `StripPrefix=1` exactly once. |

The forwarded Product Service paths are `/produits...` and
`/categories-produits...`, matching its controller mappings. Query strings,
methods, request/response bodies, status codes, and required headers must be
preserved. Disable automatic discovery route generation; route only the
reviewed public paths. Validate root collection paths as well as nested paths,
including the route matcher behavior for `/api/produits` without a trailing
slash.

Do not configure the actual Gateway in this step. Do not route the Product
prefix until the more-specific Inventory assignment and `stockId` response
compatibility are resolved. Keep all other domain paths on the monolith until
their own acceptance.

## 8. Testing and acceptance

### Product Service test scope

- **Domain/unit:** nonblank code/label, nonnegative legacy float price, update
  semantics, category ownership, not-found behavior, deletion/reference
  conflicts, and approved price precision/rounding.
- **Repository/MySQL integration:** Product and Category mappings, migration
  from empty schema, category FK behavior, ID preservation, FLOAT schema,
  duplicate-code allowance, and delete constraints. Use MySQL-compatible
  disposable infrastructure; do not assume H2 is equivalent.
- **HTTP compatibility:** the 10 Product/Category CRUD routes implemented
  in this step, including method,
  status, JSON shape/field names/nullability, path-ID precedence, validation,
  error body, and date handling. Characterize missing `prix`, blank fields,
  invalid JSON, duplicate codes, unknown IDs, and delete-with-references. The
  eleventh route, stock assignment, is deferred to the Inventory compatibility
  contract and is not part of this Product Service.
- **Inventory contract:** product reference lookup for known, inactive, and
  unknown IDs; timeout/failure handling; no stock fields or JPA types in the
  contract. Test any approved stock compatibility command/projection
  separately.
- **Procurement contract:** product lookup/snapshot including price version
  and currency once agreed; preserve line snapshots and do not recompute old
  totals.
- **Gateway:** assert routes and explicit precedence; strip `/api` once;
  preserve `produits`, `categories-produits`, queries, methods, bodies,
  statuses, and response fields; route the stock-assignment subpath to
  Inventory if approved; send unextracted paths to the monolith; verify
  automatic discovery routing remains disabled.
- **Migration:** repeatable copy, source-to-target ID sets, category links,
  external reference reconciliation, duplicate/orphan reports, exact money
  conversion tolerance, and tested rollback/freeze procedure.
- **End-to-end:** Angular's unchanged `/api` calls through proxy/Gateway to
  Product Service and Inventory compatibility route, with the monolith
  fallback still handling other domains.

### Existing test evidence

The monolith contains `ProduitServiceImplTest` and
`ProduitRestControllerTest`, covering product CRUD/controller validation,
not-found behavior, and stock assignment. No Product Category-specific
controller or service tests were found. Existing test fixtures also use
`float` prices. Port the behavior as characterization tests before
implementation; do not mistake the current tests for full migration coverage.
The baseline context test has a previously documented Docker/Testcontainers
environment limitation in
[`03-baseline-validation.md`](./03-baseline-validation.md).

### Route-cutover acceptance

Do not cut over until:

- Product Service independently passes `clean verify` on Java 21 and approved
  Boot/Cloud versions.
- Required Config Server properties load and Eureka registers the intended
  service ID.
- Product DB migrations pass on MySQL and no Product database credentials or
  foreign keys expose another service's data.
- All legacy Product and Category contract tests pass, including the agreed
  Inventory compatibility behavior.
- Backfill reconciliation is signed off by data owners.
- Gateway integration tests verify specific-route precedence, exact single
  prefix removal, and monolith fallback for all remaining paths.
- A rollback plan accounts for any writes accepted by Product Service after
  cutover.

## 9. Implementation sequence after design approval

1. **Approve open decisions:** stable IDs, stock assignment/read compatibility,
   Product code uniqueness, delete/reference behavior, currency/decimal
   precision, and source-schema access.
2. **Capture baseline:** add/execute characterization tests for all legacy
   Product and Category contracts, including currently untested category
   behavior and ambiguous validation/delete cases.
3. **Inspect source data:** use a reviewed read-only schema/data snapshot to
   confirm source table/column names, constraints, ID ranges, duplicates,
   invalid values, category and stock links, invoice-detail product
   references, and financial representation.
4. **Create standalone service:** create independent Maven project, entities,
   DTOs, repositories, services, controllers, errors, Config Data, Eureka
   registration, health and OpenAPI configuration, with no Product-to-
   Inventory or Product-to-Procurement JPA links.
5. **Create/version target schema:** implement Product-owned migrations for
   Product and Category only; validate on MySQL and against an empty
   database.
6. **Implement compatibility surface:** deliver legacy routes/DTO/statuses;
   implement the Inventory compatibility boundary only after ownership and
   consistency are approved.
7. **Backfill and reconcile:** load a consistent snapshot preserving IDs;
   validate data and financial/relationship parity without modifying source
   tables or enabling permanent dual writes.
8. **Run integration gates:** service, migration, Inventory/Procurement
   contract, Gateway route precedence, Angular end-to-end, and failure/rollback
   tests.
9. **Cut over deliberately:** route only the accepted Product and Category
   paths (and the specific Inventory assignment subroute, if approved) to
   their owners. Keep the monolith as fallback for all other paths; freeze
   old Product writes at the approved boundary and retain the rollback
   window.

## 10. Risks and unresolved decisions

| Decision / risk | Proposed owner | Required resolution |
|---|---|---|
| Actual deployed schema is unknown; port 3306 listening is not schema evidence. | DBA/data owner | Approved read-only schema/data inspection and snapshot; confirm physical names/types/constraints before migration DDL. |
| Product `prix` uses `float`; currency is absent and SQL scale is unknown. | Finance/data owner and Product owner | Approve currency contract, `BigDecimal` scale/rounding, conversion rules, tolerance, and historical snapshot policy. |
| `Produit.stock` is an external Inventory relation while Product response/form use `stockId`. | Product and Inventory owners, API/Gateway owner | Decide whether the legacy assignment URL routes directly to an Inventory compatibility endpoint and how `stockId` is returned from Product reads without Product owning stock state. |
| Inventory target distinguishes stock/location from per-product positions, but source model does not. | Inventory owner and business owner | Define mapping for each old product-stock link and meaning of source `Stock.qte`; do not infer quantity per product. |
| Product and category code uniqueness is not declared. | Product/business owner | This implementation allows duplicates; inspect source data and agree uniqueness/case/immutability before adding a constraint. |
| Deletion of products/categories with external/local references has no complete API contract. | Product, Procurement, Inventory owners | New Product Category FK conflicts map to 409. Inspect deployed FK behavior and define cross-service Product delete/retire semantics before cutover. |
| Product-category input currently cannot be assigned through Product JSON writes. | Product/API owner | Preserve this in compatibility routes; approve a separate versioned category-assignment capability if required. |
| Price and category projections in supplier/procurement consumers may become stale. | Product plus consumer owners | Define price/version snapshots and event or query contract; select transport separately. |
| Business-service port and migration tool were not specified in the original design. | Platform owner | Step 08B assigns port 8082 and Flyway for the standalone Product schema; CI policy and deployment provisioning remain pending. |
| Source seeder writes across domains in one transaction. | Data/service owners | Split seed fixtures/load procedures by owner; do not reuse the monolith's all-domain seeder in Product Service. |

## 11. Step 08B implementation decisions and remaining gates

The following implementation scope was approved:

- Product Service is standalone, uses port `8082`, Spring Boot `4.0.9`,
  Spring Cloud `2025.1.4`, Java 21, and Flyway. It is not added to the
  infrastructure Maven reactor.
- Product price remains Java/MySQL `float`/`FLOAT` until Finance approves a
  decimal policy. No conversion or data import was performed.
- Product/category code uniqueness is not enforced. Search is deferred;
  collection endpoints retain the legacy unfiltered shape.
- Product Category deletion remains non-cascading and reports an integrity
  conflict as HTTP 409. Cross-service Product deletion policy remains a
  cutover prerequisite because Product Service cannot inspect Inventory or
  Procurement databases.
- The source schema was not supplied or inspected. The service defines its
  own Flyway-managed `products` and `product_categories` tables; these are
  target-service names, not claims about the deployed monolith schema. The
  canonical CSV import template is prepared but has not been executed.
- The legacy `stockId` response component is returned as `null`; the legacy
  stock-assignment endpoint is not implemented in Product Service. No
  Product/Category Gateway route was added. Gateway cutover remains blocked
  until Inventory and Product owners approve the assignment and `stockId`
  compatibility contract.

Before any source-data transfer or authoritative cutover, obtain approved
source DDL/data evidence, confirm price conversion and cross-service delete
semantics, complete reconciliation and rollback checks, and resolve the
Inventory compatibility contract. No monolith, Angular, Inventory, Gateway,
or source database changes are part of this implementation.
