# Existing Database Migration Inventory

This is an inventory of the current JPA model and repository configuration to
guide later migration planning. It is **not** a dump or verification of a
deployed MySQL schema.

## 1. Evidence and confidence

### Verified from repository configuration

- The application uses Spring Data JPA and the MySQL Connector/J runtime
  dependency.
- `DB_URL` defaults to
  `jdbc:mysql://localhost:3306/achatdb?serverTimezone=UTC`.
- `spring.jpa.hibernate.ddl-auto` defaults to `update`.
- No checked-in Flyway/Liquibase migration files or SQL schema definition were
  found in the backend.
- The Compose file defines MySQL on host port 3306 with a configurable
  database name defaulting to `mydatabase`; its image is the mutable
  `mysql:latest` tag.
- The entity mappings use generated identity IDs and do not declare
  `@Table`, most `@Column`, or explicit `@JoinColumn` names.

### Inferred, not verified against a running/deployed database

The table and column names below are expected from Spring Boot/Hibernate's
implicit naming strategy and the entity declarations. Hibernate can create or
update a local schema when the application starts, but no database connection
or schema introspection was performed for this inventory. Do **not** assume
these names, constraints, or types exactly match an existing deployment.

### Unknown until database inspection

- Actual database and table names, names/definitions of deployed foreign keys
  and join tables, indexes, nullability, defaults, and column lengths.
- Whether a production/development database has drifted from the current
  entities or has manually added columns/constraints.
- Current row counts, null/invalid values, duplicate keys, and historical
  identifier ranges.
- Whether deployed monetary columns preserve decimal precision despite the
  Java `float` properties.

## 2. Entity/table and primary-key inventory

Table names are shown as **likely Hibernate physical names**, not verified SQL
identifiers. The Java key field and Java type are verified from source. `Long`
with `GenerationType.IDENTITY` is expected to map to a generated integer key
(commonly `BIGINT`), subject to the actual dialect/schema.

| Business entity | Likely table | Primary-key field | Key type | Important mapped fields |
|---|---|---|---|---|
| Product (`Produit`) | `produit` | `idProduit` | `Long`, identity | `codeProduit`, `libelleProduit`, `prix: float`, `dateCreation: Date`, `dateDerniereModification: Date` |
| Product category (`CategorieProduit`) | `categorie_produit` | `idCategorieProduit` | `Long`, identity | `codeCategorie`, `libelleCategorie` |
| Stock (`Stock`) | `stock` | `idStock` | `Long`, identity | `libelleStock`, `qte: Integer`, `qteMin: Integer` |
| Supplier (`Fournisseur`) | `fournisseur` | `idFournisseur` | `Long`, identity | `code`, `libelle`, `categorieFournisseur: enum` |
| Supplier detail (`DetailFournisseur`) | `detail_fournisseur` | `idDetailFournisseur` | `Long`, identity | `email`, `dateDebutCollaboration: Date`, `adresse`, `matricule` |
| Activity sector (`SecteurActivite`) | `secteur_activite` | `idSecteurActivite` | `Long`, identity | `codeSecteurActivite`, `libelleSecteurActivite` |
| Operator (`Operateur`) | `operateur` | `idOperateur` | `Long`, identity | `nom`, `prenom`, `password` |
| Invoice (`Facture`) | `facture` | `idFacture` | `Long`, identity | `montantRemise: float`, `montantFacture: float`, creation/modification dates, `archivee: Boolean` |
| Invoice detail (`DetailFacture`) | `detail_facture` | `idDetailFacture` | `Long`, identity | `qteCommandee: Integer`, `prixTotalDetail: float`, `pourcentageRemise: Integer`, `montantRemise: float` |
| Settlement (`Reglement`) | `reglement` | `idReglement` | `Long`, identity | `montantPaye: float`, `montantRestant: float`, `payee: Boolean`, `dateReglement: Date` |

Inferred unqualified columns normally follow entity field names transformed
from camel case to snake case (for example `montant_paye`), but those physical
names were not checked against MySQL.

## 3. Relationships and probable foreign-key/join-table requirements

These are relationships declared in the JPA entities. The code does not prove
that matching database foreign-key constraints exist in a deployed database.

| Relationship declared in entities | Cardinality / owning side | Data migration implication |
|---|---|---|
| Product → stock | Many products to one stock; `Produit.stock` owns the association | Preserve the stock reference for each product; likely FK in the product table |
| Product → product category | Many products to one category; `Produit.categorieProduit` owns the association | Preserve category references; likely FK in the product table |
| Supplier → supplier detail | One-to-one; `Fournisseur.detailFournisseur` owns it and cascades all operations | Preserve detail identity and supplier association; likely FK in supplier table. Verify uniqueness and nullability in the deployed schema |
| Supplier ↔ activity sector | Many-to-many; `Fournisseur.secteurActivites` owns it | Preserve each supplier/sector pair. A generated join table is expected; its actual name and key column names are undeclared |
| Invoice → supplier | Many invoices to one supplier; `Facture.fournisseur` owns it | Preserve the supplier reference for each invoice; likely FK in the invoice table |
| Invoice detail → invoice | Many details to one invoice; `DetailFacture.facture` owns it | Preserve invoice-line parent keys; likely FK in the detail table |
| Invoice detail → product | Many details to one product; `DetailFacture.produit` owns it | Preserve product references and historical line values; likely FK in the detail table |
| Settlement → invoice | Many settlements to one invoice; `Reglement.facture` owns it and is `@NotNull` | Preserve invoice association for every settlement; likely FK in settlement table |
| Operator → invoices | Unidirectional one-to-many collection at `Operateur.factures`; no `mappedBy` is specified | Hibernate generally needs an association table for this mapping; confirm actual table and any rows before extracting assignments |
| Inverse collections | Category/products, stock/products, invoice/details and settlements, supplier/invoices, detail/supplier, sector/suppliers | These are inverse navigations or collections; avoid duplicating them as independent ownership records |

`Fournisseur.secteurActivites` and `Operateur.factures` do not declare explicit
join-table names. Naming an assumed join table in an ETL job before inspecting
the actual database would be unsafe.

## 4. Monetary, quantity, and date representations

### Monetary values

All monetary fields currently use Java `float` and have no explicit JPA
`precision`/`scale`:

- `Produit.prix`
- `Facture.montantFacture`, `Facture.montantRemise`
- `DetailFacture.prixTotalDetail`, `DetailFacture.montantRemise`
- `Reglement.montantPaye`, `Reglement.montantRestant`

The inferred relational type is floating point, but the actual MySQL column
types and precision are unknown. Floating point is not an exact decimal
representation. Before migration, inspect source columns and sample values,
choose an explicit target monetary representation, and document the conversion
and rounding policy. Do not perform a silent `float` to decimal conversion.

`DetailFacture.pourcentageRemise` is an `Integer`; it is a percentage value,
not an amount. The invoice amount/remise fields and detail amount/remise fields
are separately stored and should be reconciled against business calculations
before any recomputation.

### Stock and invoice-line quantities

- `Stock.qte` and `Stock.qteMin` are Java `Integer` quantities, validated as
  nonnegative.
- `DetailFacture.qteCommandee` is a Java `Integer`.
- The existing stock model represents quantities as whole counts; it does not
  expose decimal quantity, units of measure, reservations, or movement history.
- Product-to-stock is a reference; stock is not a collection of per-product
  quantity records in this entity model. Confirm the real meaning of `qte`
  before designing inventory-service data or an initial stock import.

### Dates

The mapped `java.util.Date` fields use `@Temporal(TemporalType.DATE)`, so the
entity model expresses date-only values for product, supplier-detail, invoice,
and settlement dates. Inspect the deployed SQL types and source timezone/
serialization behavior before converting them.

## 5. Data-integrity and extraction risks

1. **Schema drift is possible:** `ddl-auto=update` changes schemas at startup,
   while no versioned schema history is checked in. Capture the source
   database's actual DDL before creating migration scripts.
2. **No cross-service foreign keys can be preserved as live database
   constraints:** the target architecture assigns products, stock, suppliers,
   invoices, and settlements to separate business services/databases. Export
   source IDs and relationship pairs as migration data, then replace
   cross-boundary joins with stable identifiers and service/event contracts.
3. **Invoice lines need explicit treatment:** invoice details are nested in
   invoice responses, but there is no dedicated details API. Preserve all
   existing detail rows, their product IDs, unit/discount values, and invoice
   parent IDs directly from the source database.
4. **Operator-to-invoice links may be in an implicit association table:**
   identify and export that table before splitting the operator and invoice
   ownership.
5. **Money conversion is a correctness risk:** the Java types are `float`;
   compare raw deployed values and define decimal precision/rounding before
   import.
6. **Physical schema is not verified here:** use read-only database metadata
   queries or a consistent database snapshot to confirm table/column names,
   PK/FK constraints, join tables, data types, and data quality before
   implementing ETL.
