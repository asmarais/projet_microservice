# STOCK-SERVICE-EXTRACTION-PLAN

## 1. Current implementation analysis

The monolith’s Stock module is implemented as CRUD over a `Stock` entity. Its fields and source constraints are:

| Field | Java type | Source declaration and meaning |
|---|---|---|
| `idStock` | `Long` | Identity-generated primary key. |
| `libelleStock` | `String` | Stock label; `@NotBlank`. |
| `qte` | `Integer` | Quantity; `@NotNull` and `@PositiveOrZero`. |
| `qteMin` | `Integer` | Minimum quantity threshold; `@NotNull` and `@PositiveOrZero`. |
| `produits` | `Set<Produit>` | Inverse `@OneToMany(mappedBy = "stock")`; omitted from JSON using `@JsonIgnore`. |

These declarations are in `Stock.java`. The repository uses Spring Data JPA and defines low-stock records as those where `qte < qteMin` in `StockRepository.java`. Equality is not treated as low stock.

The service implements list, create, update, retrieve, delete, and a low-stock report helper in `StockServiceImpl.java`. Updates copy only the label and the two quantity fields. The report helper is **not exposed by the REST controller**.

The controller exposes `/stocks` CRUD and maps entities to a response DTO with `idStock`, `libelleStock`, `qte`, and `qteMin`; product associations are intentionally excluded. There is no separate request DTO or mapper—the controller accepts the entity for writes. See `StockRestController.java` and `StockResponse.java`.

Validation is applied to POST and PUT requests. Missing Stock records produce HTTP 404 with `ProblemDetail`; malformed or invalid request bodies produce HTTP 400. Delete checks whether the Stock exists, but does not explicitly check whether Products reference it. See `StockExceptionHandler.java` and `StockNotFoundException.java`.

### Product relationship and business-rule boundary

The monolith relationship is **one Stock to many Products**: `Produit.stock` is a lazy `@ManyToOne`, and `Stock.produits` is its inverse. The product owns that association in the monolith. The Stock entity itself has **no `productId` field**. The assignment operation is `PUT /produits/{produitId}/stock/{stockId}`; the Product service loads both records and sets the association. See `Produit.java`, `ProduitRestController.java`, and `ProduitServiceImpl.java`.

Therefore, modeling each existing Stock row as having one `productId` would change the current cardinality and cannot be inferred from this source. The existing code also does not establish that `qte` is a quantity per product, per location, or the sum of assigned-product quantities. That meaning must be confirmed before designing per-product inventory positions.

## 2. Existing database schema

The backend uses MySQL; its default URL points to `achatdb`, and JPA `ddl-auto` defaults to `update`. There is no checked-in backend Flyway/Liquibase migration or SQL schema for Stock. Consequently, the physical deployed schema has **not** been verified. See `application.properties` and `05-database-migration-inventory.md`.

From the entities, the likely legacy table is `stock`, and the likely Product-side relation is a foreign-key column on the product table. These are inferences only: the entities do not declare explicit table, column, or join-column names. Column lengths, database nullability, constraints, indexes, foreign-key names, and actual data must be confirmed against the deployed database before migration.

The extracted Product Service has a different, explicit Flyway schema: `products` has `id_product`, `code_product`, `label`, `price`, dates, and `category_id`; there is **no stock column or foreign key**. See `V1__create_product_catalog.sql`. This separation is intentional in the Product Service design.

## 3. Product Service integration analysis

The Product Service’s identifier is `Long` (`idProduit`). Its product response is:

- `idProduit: Long`
- `codeProduit: String`
- `libelleProduit: String`
- `prix: float`
- `stockId: Long`
- `categorieProduitId: Long`

The response DTO is defined in `ProductResponse.java`.

Available Product endpoints are:

| Method | Path | Success |
|---|---|---|
| GET | `/produits` | 200; list of Product responses |
| GET | `/produits/{productId}` | 200; Product response; 404 if absent |
| POST | `/produits` | 201; created Product response |
| PUT | `/produits/{productId}` | 200; updated Product response |
| DELETE | `/produits/{productId}` | 204 |

The Product Service also exposes category CRUD under `/categories-produits`. These endpoints are defined by `ProductController.java` and `ProductCategoryController.java`.

**The usable Product validation call is `GET /produits/{productId}`.** A successful response verifies that the Product ID currently resolves; a 404 means it does not. The Product Service has no active/inactive field or endpoint that would support additional status validation. Its extracted `ProductController` does **not** implement the legacy stock-assignment route.

Although the Product response includes `stockId`, the current Product Service always returns it as `null`; it does not persist or query stock assignments. This is visible in `ProductApplicationService.java` and asserted in `ProductApiContractTest.java`. Stock Service can use the GET endpoint for existence checks, but cannot use its response to discover a Product’s current Stock assignment.

The registered application name is `product-service`, configured in `application.properties` and `product-service.yml`. No OpenFeign configuration was found in the Product Service. Its POM imports Spring Cloud’s BOM and includes Eureka and Config clients, but not OpenFeign; see `pom.xml`.

## 4. Proposed Stock Service architecture

The repository’s approved target architecture calls this service **Inventory Service**, with Eureka name `inventory-service`; it owns Stock and product-to-stock assignments. For this plan, “Stock Service” means that Inventory boundary. See `02-revised-target-architecture.md` and `07-microservices-development-standards.md`.

Keep the currently implemented Stock CRUD and assignment behavior in this service. Do not add movements, reservations, or other unimplemented capabilities as part of this extraction without a separate approved scope.

A candidate independent database is `inventory_db`, consistent with the repository’s service standards. A candidate logical schema is:

| Table | Candidate contents |
|---|---|
| `stocks` | Stock identity, label, quantity, and minimum quantity. Preserve the existing Java types and nonnegative validation. Determine database column lengths and constraints only after inspecting source data and DDL. |
| `stock_product_assignments` | A Stock ID and a scalar Product ID. The Product ID is a value, **not** a cross-service JPA relation or foreign key. A local foreign key from this table to `stocks` is appropriate. |

This separate assignment table preserves the source’s one-Stock-to-many-Products shape without putting a single `productId` on a Stock row. A uniqueness rule on `productId` would preserve the current apparent one-Stock-per-Product behavior, but should be approved only after confirming that the target does not allow multiple positions per Product.

## 5. Proposed API contract

Preserve the currently implemented Stock CRUD paths and JSON field names. These contracts are documented in `04-existing-api-contracts.md`.

| Method and URL | Request | Success response | Validation and errors |
|---|---|---|---|
| `GET /stocks` | None | 200; array of `{idStock, libelleStock, qte, qteMin}` | No Stock-specific error is defined. |
| `GET /stocks/{stockId}` | None | 200; Stock response | 404 if Stock does not exist. |
| `POST /stocks` | `{libelleStock, qte, qteMin}` | 201; created Stock response | 400 if label is blank, either quantity is absent, or either quantity is negative. |
| `PUT /stocks/{stockId}` | `{libelleStock, qte, qteMin}` | 200; updated Stock response | Same validation as POST; 404 if Stock does not exist. |
| `DELETE /stocks/{stockId}` | None | 204; empty | 404 if Stock does not exist. Define and test deletion behavior when Products are assigned; a 409 conflict is a reasonable proposed contract if deletion is disallowed. |
| `PUT /produits/{productId}/stock/{stockId}` | None | 200; Product response, preserving the legacy assignment contract | 404 if Product or Stock does not exist; 503 if Product Service cannot be reached. The assignment must validate the Product before committing. |

The assignment path is already present in the monolith, but is **not** present in the extracted Product Service. The existing frontend calls it, so it needs an Inventory-owned compatibility handler and a more-specific Gateway route, rather than routing that request to Product Service. The Product Service extraction design explicitly defers this compatibility decision; see `08-product-service-extraction-design.md`.

The low-stock report exists only as a service method and is not an existing REST endpoint. This plan does not add a new public endpoint for it.

## 6. Feign communication design

For assignment requests, Stock Service should call Product Service synchronously using OpenFeign and Eureka discovery, targeting the registered service name `product-service` rather than a fixed instance address. The call should be to the existing `GET /produits/{productId}` endpoint only.

Treat outcomes distinctly:

- **Product GET 200:** continue with Stock validation and persist the assignment.
- **Product GET 404:** reject the assignment as a missing Product; do not persist a dangling reference.
- **Product Service timeout, connection failure, or 5xx:** return a dependency failure such as 503 and fail closed. Do not report the assignment as successful or substitute an empty Product.
- **Stock ID absent:** return Stock’s 404 response.

Use explicit connection/response timeouts and typed error mapping; do not automatically retry unsafe commands. The repository’s communication standards require bounded timeouts, visible dependency failures, and bounded retries only for safe transient operations. See `07-microservices-development-standards.md`.

A successful Product lookup only establishes existence at lookup time. Because there is no cross-database foreign key, Product deletion can race with assignment or leave an existing assignment pointing to a deleted Product. Product/Inventory owners must define deletion or retirement semantics; the current Product Service does not check Inventory references.

## 7. Gateway and Eureka changes

The Gateway currently routes Product paths to `lb://PRODUCT-SERVICE`, with discovery-based route creation disabled, and sends unmatched `/api/**` requests to the monolith fallback. Thus `/api/stocks/**` currently reaches the monolith unless an explicit route is added. See `api-gateway.yml`.

Required configuration when the service is approved:

1. Add an explicit `/api/stocks/**` route to `lb://INVENTORY-SERVICE`, stripping `/api` once.
2. Add a more-specific route for the legacy `/api/produits/{productId}/stock/{stockId}` assignment path to Inventory, ahead of the general Product route.
3. Add `inventory-service.yml` to the Config Server Git-backed repository, with its datasource, port, Eureka client, and health settings. Follow the established pattern in `product-service.yml`.
4. Register the application as `inventory-service` and import Config Server configuration as other services do.
5. No Eureka server behavior change is indicated: the current server is configured as a registry, and business services register as clients. Do not enable automatic Gateway discovery routing; keep explicit routes.

## 8. Required Angular changes

The Angular Stock screens use `/api/stocks` through the shared CRUD service. Once the Gateway has the explicit Stock route, those Stock CRUD calls can retain their paths and payloads without a required API-client change. See `stock.service.ts`, `stock.model.ts`, and `crud.service.ts`.

The Product edit form also loads Stock records and calls the legacy assignment path through `ProduitService.assignStock`. That call can continue only if the specific Gateway compatibility route is installed. See `produit-form.ts` and `produit.service.ts`.

There is a separate read-side limitation: Product Service currently returns `stockId: null`, so the form cannot prefill the existing assignment from Product reads. Preserving that user experience requires an approved Inventory-backed read model or composition contract; neither is implemented. The current form also does not submit an unassignment when a user selects “None.” These behaviors must be addressed explicitly rather than assumed to work after routing.

## 9. Data migration plan

1. **Inspect first:** obtain an approved, read-only snapshot and physical DDL for the deployed monolith. Confirm actual tables, columns, foreign keys, data quality, and Stock/Product row counts. The checked-in migration inventory warns that entity mappings do not prove deployed schema.
2. **Confirm Product ID correspondence:** Product Service data transfer and Gateway cutover remain deferred in its extraction design. Confirm whether Product IDs are preserved during that transfer. If not, create and retain an explicit source-to-target Product ID mapping.
3. **Export source records:** export each Stock’s ID, label, `qte`, and `qteMin`, plus the source Product-to-Stock relationship pairs. Do not infer per-product quantities from `Stock.qte`.
4. **Backfill in a controlled window:** freeze source Stock and assignment writes, take a consistent snapshot, import Stock rows preserving IDs where required, then import assignment pairs using validated target Product IDs. If IDs cannot be preserved, use the approved mapping.
5. **Validate references:** confirm Product IDs through Product Service or an approved migration validation process. Quarantine and report missing references; do not silently drop them or import them as if valid.
6. **Reconcile:** compare row counts, Stock IDs, field values, assignment-pair counts, and orphan/duplicate reports. Set the target identity sequence beyond imported IDs.
7. **Cut over and retain rollback:** route the Gateway to Inventory only after reconciliation and compatibility approval. Keep the monolith’s data available under an approved freeze/rollback procedure; do not dual-write without an explicit consistency design.

No source database rows, deployed DDL, or migration results were inspected for this analysis.

## 10. Implementation sequence

1. Approve the Stock-versus-location meaning, assignment cardinality, and quantity semantics.
2. Inspect deployed schema/data and settle ID preservation and Product deletion policy.
3. Define Stock Service API/error contracts, including assignment compatibility and Product lookup failure handling.
4. Create the standalone Inventory Service with its own database and versioned schema migrations.
5. Add the Product Service OpenFeign client and focused tests for Product found, Product missing, and dependency unavailable.
6. Implement and verify Stock CRUD and assignment behavior.
7. Add explicit Gateway routes and Inventory Config Server configuration; verify route precedence and Eureka registration.
8. Update Angular only as required by the approved assignment-write and assignment-read contracts.
9. Backfill, reconcile, cut over, and retain the rollback window.

## 11. Risks and unresolved questions

- **Quantity meaning:** Is `qte` a warehouse total, a location quantity, or something else? The source does not establish this.
- **Assignment cardinality:** Can one Product have multiple stock positions in the target, or must the monolith’s one-Stock-per-Product assignment be preserved?
- **Product ID migration:** Are the Product Service IDs identical to the monolith IDs? The Product extraction design says source data transfer remains deferred.
- **Assignment read compatibility:** How should Product reads expose the currently assigned `stockId` when Product Service returns `null`?
- **Product deletion:** Should Product deletion be rejected, soft-deleted, or allowed to leave a historical/unresolved reference?
- **Stock deletion:** What is the required behavior while Products are assigned?
- **Schema facts:** Actual monolith DDL, constraints, and data have not been verified.
- **API choice:** Confirm whether to retain the legacy assignment route as an Inventory compatibility operation, as recommended by the existing Product extraction design.
- **Service naming:** Repository standards use `inventory-service`; confirm that this matches the requested Stock Service naming.

**Approval requested before implementation.**