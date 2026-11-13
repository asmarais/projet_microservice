# Existing Application Baseline Validation

**Scope:** Read-only inspection and validation of the current monolith before
microservices work. No Java, Angular, database schema, dependency, or runtime
configuration changes were made for this baseline.

## 1. Application and build baseline

| Item | Observed baseline |
|---|---|
| Backend | One Spring Boot application, Maven artifact `org.example:achat:0.0.1-SNAPSHOT` |
| Java configured | 21 (`java.version` in `backend/pom.xml`) |
| Java installed for validation | Eclipse Temurin OpenJDK `21.0.12.1+1` |
| Spring Boot | 4.1.1 |
| Maven | Wrapper distribution 3.10.0 |
| Backend HTTP port | 8089 by default (`SERVER_PORT` can override it) |
| Frontend | Angular 21; `ng serve` defaults to port 4200 |
| Database | MySQL; default application URL targets `achatdb` on localhost:3306 |
| ORM schema behavior | `spring.jpa.hibernate.ddl-auto=update` by default |
| Default profile | `dev` (`SPRING_PROFILES_ACTIVE` can override it) |

Configuration was read from [the Maven POM](../../backend/pom.xml),
[application.properties](../../backend/src/main/resources/application.properties),
[application-dev.properties](../../backend/src/main/resources/application-dev.properties),
[the backend Compose file](../../backend/compose.yaml), and
[the frontend Angular configuration](../../frontend/angular.json).

## 2. Build and tests

Validation was run from `backend/` with Java 21:

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
```

| Check | Result |
|---|---|
| Package without executing tests | **Succeeded** (`BUILD SUCCESS`) |
| Main-source compilation | **Succeeded**; 67 Java source files compiled |
| Test-source compilation | **Succeeded**; 7 Java test source files compiled |
| Existing test suite | **Did not fully pass**: 38 tests, 0 assertion failures, 1 error |
| Tests completed without error | 37 |
| Blocking test | `AchatApplicationTests.contextLoads` |
| Reported cause | Testcontainers could not find a valid Docker environment while creating the configured MySQL and Grafana LGTM containers |

The Docker CLI returned a version, but the test run still could not connect to a
Docker environment. This is an environment-dependent test startup failure, not
a Java compilation failure. No tests or source were changed to work around it.
See [the test and Testcontainers configuration](../../backend/src/test/java/org/example/achat/AchatApplicationTests.java)
and [TestcontainersConfiguration](../../backend/src/test/java/org/example/achat/TestcontainersConfiguration.java).

## 3. REST surface and frontend proxy

There are **46 mapped business REST endpoints** across the existing controllers.
They cover products, product categories, stocks, suppliers, activity sectors,
operators, invoices, and settlements/reporting. Invoice details have no
standalone controller or endpoint; they are represented as nested invoice
details. The exact route and payload inventory is in
[04-existing-api-contracts.md](./04-existing-api-contracts.md).

The Angular environments set `apiUrl` to `/api`. The development server uses
`frontend/proxy.conf.json`, which forwards `/api` to
`http://localhost:8089` and strips the `/api` prefix. `angular.json` selects
that proxy for `ng serve`. This is a configuration inspection; the frontend
and backend were not launched together for a live HTTP proxy test.

Configured local URLs:

| Application | URL |
|---|---|
| Angular development server | `http://localhost:4200` |
| Spring Boot API | `http://localhost:8089` |
| Spring Boot Swagger UI | `http://localhost:8089/swagger-ui/index.html` |
| Spring Boot OpenAPI JSON | `http://localhost:8089/v3/api-docs` |
| Spring Boot health endpoint | `http://localhost:8089/actuator/health` |

These are configured defaults, not a claim that either server was started or
listened on those ports during this validation.

## 4. Local start instructions

1. Provide a MySQL instance on port 3306 and configure `backend/.env` from
   [backend/.env.example](../../backend/.env.example), or set the corresponding
   environment variables. Ensure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
   match the actual database. The example uses database `mydatabase` and user
   `root`; the application-property defaults instead use `achatdb` and
   `achat_user`.
2. From `backend/`, start the API:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The provided example enables the `dev` profile and `APP_SEED_ENABLED=true`.
   To use the Compose MySQL starter, run `docker compose up -d mysql` from
   `backend/`; it exposes port 3306. Compose initializes the root password
   from `DB_PASSWORD` (or `change-me`) and its database name from
   `MYSQL_DATABASE` (or `mydatabase`).
3. In a separate terminal, from `frontend/`, install frontend dependencies if
   needed and run the Angular development server:

   ```powershell
   npm install
   npm start
   ```

4. Open `http://localhost:4200`. Development API calls use the configured
   `/api` proxy to port 8089.

The Compose database defaults and application-property defaults do not match;
set the environment values explicitly or use the provided `.env.example`
settings so the API connects to the database created by Compose.

## 5. Existing issues relevant before migration

- **Test baseline is incomplete:** the Spring context integration test needs
  working Docker access for Testcontainers. Re-run it in an environment where
  the Docker daemon is available before treating this as a complete behavioral
  baseline.
- **Deployed schema is unverified:** Hibernate `update` is enabled by default,
  and no checked-in Flyway/Liquibase migrations or SQL schema definition were
  found. The annotations in the code are not evidence of the schema currently
  deployed; see [05-database-migration-inventory.md](./05-database-migration-inventory.md).
- **Database configuration must be reconciled:** Compose and the application's
  default JDBC settings select different database names and credentials.
- **Some existing API contracts have known gaps:** settlement creation/update
  cannot supply its required invoice reference through JSON; invoice lookup by
  unknown ID returns HTTP 200 with an empty body; invoice recovery reporting
  catches every exception and returns `0`; invoice details have no standalone
  operations. Details and migration implications are documented in
  [04-existing-api-contracts.md](./04-existing-api-contracts.md).
- **Monetary representation needs a migration decision:** current persisted
  amount and price properties use Java `float`, without explicit SQL precision
  and scale. Do not silently reinterpret or round these values during data
  migration.
- **Operator records contain a password field:** operator responses omit it,
  but the entity accepts it on writes. Authentication and credential handling
  should be explicitly designed before moving operator/user data.

These are baseline findings only. No defect fixes or behavior changes were made.
