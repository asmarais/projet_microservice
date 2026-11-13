# Existing API Contract Inventory

This inventory describes the current Spring MVC controllers, response DTOs,
entities, and Angular service calls. It is not a proposed microservices API.
Paths below are relative to `http://localhost:8089`; the Angular development
proxy presents them through `/api` by removing that prefix.

## 1. Shared contract conventions

- Collection and single-resource reads return HTTP **200**.
- Standard create operations return **201** with the response representation.
- Standard update operations return **200** with the response representation.
- Standard deletes return **204** with no body.
- Controllers with `@Valid` return **400** for request validation failures;
  framework-generated validation error bodies are not normalized by a shared
  application error contract.
- Product/category, stock, supplier/sector, operator, and settlement not-found
  handlers return `ProblemDetail` with **404** and a `detail` message.
- Settlement date-range validation returns `ProblemDetail` with **400**.
- Invoice endpoints have no invoice-specific exception handler. Their
  behavior differs from the CRUD resources and is called out below.
- Unless stated otherwise, endpoints do not declare query parameters. IDs and
  dates in path segments are path parameters, not query parameters.
- Date-only inputs use `yyyy-MM-dd` (ISO date).

## 2. Products

Response: `ProduitResponse` with `idProduit`, `codeProduit`,
`libelleProduit`, `prix`, `stockId`, and `categorieProduitId`. The write body is
the `Produit` entity: `codeProduit`, `libelleProduit`, `prix`, and optionally
`dateCreation` / `dateDerniereModification`. `codeProduit` and
`libelleProduit` are nonblank; `prix` is required and nonnegative. JSON
`stock` and `categorieProduit` are ignored; stock is assigned using the
dedicated route, and category assignment is not exposed.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /produits` | None | None | 200; array of `ProduitResponse` | — |
| `GET /produits/{produitId}` | `produitId` path ID | None | 200; `ProduitResponse` | 404 `ProblemDetail` if missing |
| `POST /produits` | None | Product fields above | 201; created `ProduitResponse` | 400 validation error |
| `PUT /produits/{produitId}` | `produitId` path ID | Product fields above | 200; updated `ProduitResponse` | 400 validation error; 404 if missing |
| `DELETE /produits/{produitId}` | `produitId` path ID | None | 204; empty | 404 if missing |
| `PUT /produits/{produitId}/stock/{stockId}` | `produitId`, `stockId` path IDs | None | 200; updated `ProduitResponse` | 404 if product or stock is missing |

## 3. Product categories

Response: `CategorieProduitResponse` with `idCategorieProduit`,
`codeCategorie`, and `libelleCategorie`. Request body fields are
`codeCategorie` and `libelleCategorie`; both are nonblank.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /categories-produits` | None | None | 200; array of category responses | — |
| `GET /categories-produits/{categorieProduitId}` | `categorieProduitId` path ID | None | 200; category response | 404 `ProblemDetail` if missing |
| `POST /categories-produits` | None | `codeCategorie`, `libelleCategorie` | 201; category response | 400 validation error |
| `PUT /categories-produits/{categorieProduitId}` | `categorieProduitId` path ID | `codeCategorie`, `libelleCategorie` | 200; category response | 400 validation error; 404 if missing |
| `DELETE /categories-produits/{categorieProduitId}` | `categorieProduitId` path ID | None | 204; empty | 404 if missing |

## 4. Stocks

Response: `StockResponse` with `idStock`, `libelleStock`, `qte`, and
`qteMin`. Request body fields are `libelleStock`, `qte`, and `qteMin`.
The label is nonblank; both quantities are required, nonnegative integers.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /stocks` | None | None | 200; array of stock responses | — |
| `GET /stocks/{stockId}` | `stockId` path ID | None | 200; stock response | 404 `ProblemDetail` if missing |
| `POST /stocks` | None | `libelleStock`, `qte`, `qteMin` | 201; stock response | 400 validation error |
| `PUT /stocks/{stockId}` | `stockId` path ID | `libelleStock`, `qte`, `qteMin` | 200; stock response | 400 validation error; 404 if missing |
| `DELETE /stocks/{stockId}` | `stockId` path ID | None | 204; empty | 404 if missing |

The service has a low-stock report helper, but the current REST controller does
not expose it.

## 5. Suppliers

Response: `FournisseurResponse` with `idFournisseur`, `code`, `libelle`,
`categorieFournisseur` (`ORDINAIRE` or `CONVENTIONNE`), `detailFournisseurId`,
and `secteurActiviteIds`. The supplier entity write body accepts `code`,
`libelle`, `categorieFournisseur`, and optional nested `detailFournisseur`
fields: `email`, `dateDebutCollaboration`, `adresse`, and `matricule`.
The detail entity marks `adresse` and `matricule` nonblank and validates
`email`; the supplier association with detail is not explicitly annotated with
cascaded bean validation. `factures` and `secteurActivites` are ignored on
entity JSON input. Use the assignment route for sectors.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /fournisseurs` | None | None | 200; array of supplier responses | — |
| `GET /fournisseurs/{fournisseurId}` | `fournisseurId` path ID | None | 200; supplier response | 404 `ProblemDetail` if missing |
| `POST /fournisseurs` | None | Supplier fields above | 201; supplier response | 400 for request validation failures |
| `PUT /fournisseurs/{fournisseurId}` | `fournisseurId` path ID | Supplier fields above | 200; supplier response | 400 validation error; 404 if missing |
| `DELETE /fournisseurs/{fournisseurId}` | `fournisseurId` path ID | None | 204; empty | 404 if missing |
| `PUT /fournisseurs/{fournisseurId}/secteurs-activite/{secteurActiviteId}` | Both IDs are path parameters | None | 200; updated supplier response | 404 `ProblemDetail` if supplier or sector is missing |

## 6. Activity sectors

Response: `SecteurActiviteResponse` with `idSecteurActivite`,
`codeSecteurActivite`, and `libelleSecteurActivite`. Request body fields are
`codeSecteurActivite` and `libelleSecteurActivite`; both are nonblank.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /secteurs-activite` | None | None | 200; array of sector responses | — |
| `GET /secteurs-activite/{secteurActiviteId}` | `secteurActiviteId` path ID | None | 200; sector response | 404 `ProblemDetail` if missing |
| `POST /secteurs-activite` | None | `codeSecteurActivite`, `libelleSecteurActivite` | 201; sector response | 400 validation error |
| `PUT /secteurs-activite/{secteurActiviteId}` | `secteurActiviteId` path ID | `codeSecteurActivite`, `libelleSecteurActivite` | 200; sector response | 400 validation error; 404 if missing |
| `DELETE /secteurs-activite/{secteurActiviteId}` | `secteurActiviteId` path ID | None | 204; empty | 404 if missing |

## 7. Operators

Response: `OperateurResponse` with `idOperateur`, `nom`, `prenom`, and
`factureIds`; it does not include `password`. Request body fields are
`nom`, `prenom`, and `password`, all marked nonblank. The `factures` entity
association is ignored in JSON input.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /operateurs` | None | None | 200; array of operator responses | — |
| `GET /operateurs/{operateurId}` | `operateurId` path ID | None | 200; operator response | 404 `ProblemDetail` if missing |
| `POST /operateurs` | None | `nom`, `prenom`, `password` | 201; operator response | 400 validation error |
| `PUT /operateurs/{operateurId}` | `operateurId` path ID | `nom`, `prenom`, `password` | 200; operator response | 400 validation error; 404 if missing |
| `DELETE /operateurs/{operateurId}` | `operateurId` path ID | None | 204; empty | 404 if missing |

## 8. Invoices and invoice details

Invoices use the `Facture` JPA entity directly, not a response DTO. Responses
include invoice scalar fields (`idFacture`, amounts, dates, `archivee`) and
`detailsFacture`. Supplier and settlement associations are `@JsonIgnore`.
Nested detail fields are `idDetailFacture`, `qteCommandee`,
`prixTotalDetail`, `pourcentageRemise`, `montantRemise`, and `produit`.
There are **no standalone invoice-detail endpoints**.

The POST body is a `Facture` JSON object with scalar invoice fields and,
optionally, a `detailsFacture` collection. `fournisseur` and `reglements` are
ignored. The handler does not use `@Valid`; its service currently saves only
the `Facture` and does not cascade persistence to details or resolve a supplier.
Do not treat nested-line submission as a reliable invoice-line creation
contract.

| Method and path | Parameters | Request body | Success response | Error responses |
|---|---|---|---|---|
| `GET /facture/retrieve-all-factures` | None | None | 200; array of `Facture` entities, including nested details | No invoice-specific error mapping |
| `GET /facture/retrieve-facture/{facture-id}` | `facture-id` path ID | None | 200; `Facture` entity | Unknown ID currently returns 200 with an empty body |
| `POST /facture/add-facture` | None | Invoice fields above | 200; saved `Facture` entity | No controller validation or invoice-specific error body |
| `PUT /facture/cancel-facture/{facture-id}` | `facture-id` path ID | None | 200; empty body | No invoice-specific error mapping; service does not cleanly reject a missing ID |
| `GET /facture/getFactureByFournisseur/{fournisseur-id}` | `fournisseur-id` path ID | None | 200; array of `Facture` entities | 404 `ProblemDetail` if supplier is missing |
| `PUT /facture/assignOperateurToFacture/{idOperateur}/{idFacture}` | Both IDs are path parameters | None | 200; empty body | 404 if operator is missing; invoice absence is not consistently handled |
| `GET /facture/pourcentageRecouvrement/{startDate}/{endDate}` | `startDate`, `endDate` path parameters (`yyyy-MM-dd`) | None | 200; JSON number (float) | Invalid date conversion can produce 400; service exceptions are caught and returned as `200` with `0` |

`cancel-facture` is an archive operation (sets `archivee=true`), not a physical
delete. Its current service loads a new empty `Facture` when the ID is unknown
and attempts to save it; callers should not rely on 404 behavior.

## 9. Settlements and revenue reporting

Response: `ReglementResponse` with `idReglement`, `montantPaye`,
`montantRestant`, `payee`, `dateReglement`, and `factureId`. The request entity
has the amount and date fields plus a required `facture` association, but
`facture` is `@JsonIgnore`. `@Valid` therefore sees no invoice association in a
normal JSON write; the current POST and PUT operations fail validation with
400. The frontend consequently exposes only settlement listing, details,
filtering, and deletion.

| Method and path | Parameters | Request body / query | Success response | Error responses |
|---|---|---|---|---|
| `GET /reglements` | None | None | 200; array of settlement responses | — |
| `GET /reglements/{reglementId}` | `reglementId` path ID | None | 200; settlement response | 404 `ProblemDetail` if missing |
| `POST /reglements` | None | Settlement entity JSON (invoice association is ignored) | 201 if accepted | 400 validation failure; normal JSON cannot satisfy required `facture` |
| `PUT /reglements/{reglementId}` | `reglementId` path ID | Settlement entity JSON (invoice association is ignored) | 200 if accepted | 400 validation failure; 404 if ID is missing |
| `DELETE /reglements/{reglementId}` | `reglementId` path ID | None | 204; empty | 404 `ProblemDetail` if missing |
| `GET /reglements/facture/{factureId}` | `factureId` path ID | None | 200; array of settlement responses | No explicit missing-invoice validation; empty array is possible |
| `GET /reglements/revenue?startDate={date}&endDate={date}` | `startDate`, `endDate` query parameters (`yyyy-MM-dd`) | None | 200; JSON number (float) | 400 `ProblemDetail` if start is after end; missing/invalid date parameter is a framework 400 |

The revenue endpoint sums `montantPaye` over settlement dates in the inclusive
date range and excludes records linked to archived invoices.
