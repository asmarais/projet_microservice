# Infrastructure Foundation

The independent Maven reactor contains Config Server, Eureka Discovery
Server, and API Gateway. The existing monolith under `backend/` remains
separate and unchanged.

## Project structure

```text
infrastructure/
  pom.xml
  config-server/
  discovery-server/
  api-gateway/
config-repo/
  application.yml
  discovery-server.yml
  api-gateway.yml
```

## Selected versions and dependencies

- Java 21
- Spring Boot 4.0.9
- Spring Cloud BOM 2025.1.4
- Maven Wrapper 3.10.0
- Config Server: `spring-cloud-config-server` 5.0.6
- Eureka Server: `spring-cloud-starter-netflix-eureka-server` 5.0.3
- Config Client: `spring-cloud-starter-config` 5.0.6
- API Gateway: `spring-cloud-starter-gateway-server-webflux` 5.0.4,
  `spring-cloud-starter-netflix-eureka-client` 5.0.3, and
  `spring-cloud-starter-loadbalancer` 5.0.4

The Cloud BOM manages Spring Cloud dependency versions centrally. The official
2025.1.4 release documentation lists Boot 4.0.9 as supported. The monolith
remains on Boot 4.1.1 and is not included in this reactor.

## Configuration repository

Config Server reads the local Git working tree at `config-repo/`, on branch
`main`. The sample `application.yml`, `discovery-server.yml`, and
`api-gateway.yml` contain only safe defaults; never store passwords, tokens,
private keys, or environment secrets in this repository.

The repository is initialized for this workspace. To create it in a fresh
checkout, run these commands from the repository root:

```powershell
git -C .\config-repo init -b main
git -C .\config-repo add application.yml discovery-server.yml api-gateway.yml
git -C .\config-repo -c user.name="Local Config Repository" -c user.email="config-repo@localhost" commit -m "Add local infrastructure configuration"
```

If Git reports that the branch or repository already exists, do not
reinitialize it. Check `git -C .\config-repo status` and confirm the three
configuration files are committed on `main`. To use another local Git
repository, set
`CONFIG_REPO_URI` to its JGit-compatible `file:` URI for Config Server.

## Build and test

Run from the repository root:

```powershell
Push-Location .\infrastructure
try {
    .\mvnw.cmd -version
    .\mvnw.cmd clean verify
} finally {
    Pop-Location
}
```

The reactor includes only `config-server`, `discovery-server`, and
`api-gateway`. Verification compiles all modules and runs the Config Server
endpoint test. Discovery Server and Gateway startup/routing checks are separate
runtime validation steps.

## Startup order and PowerShell commands

Start Config Server first. It reads `config-repo/`; Discovery Server must
retrieve `discovery-server.yml` from Config Server and fails startup if that
configuration server is not available. Config Server has no Config Client
dependency and does not fetch configuration from itself.

In the first PowerShell terminal, from the repository root:

```powershell
Push-Location .\infrastructure\config-server
try {
    ..\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
```

Config Server defaults to port 8888. To override its port in that terminal,
set `$env:CONFIG_SERVER_PORT` before the command.

In a second terminal, set a reachable Config Server URL if the default is not
suitable, then start Eureka:

```powershell
$env:CONFIG_SERVER_URL = "http://localhost:8888"
Push-Location .\infrastructure\discovery-server
try {
    ..\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
```

Eureka defaults to port 8761. For a different port, set
`$env:EUREKA_SERVER_PORT` in this terminal. Host-local `localhost` defaults
are for local processes; when running in containers or other networks, provide
reachable URLs through environment variables. Container networking is not
configured by this project.

In a third PowerShell terminal, configure and start the Gateway:

```powershell
$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE = "http://localhost:8761/eureka/"
$env:MONOLITH_BASE_URL = "http://localhost:8089"
Push-Location .\infrastructure\api-gateway
try {
    ..\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
```

The Gateway defaults to port 8080. `API_GATEWAY_PORT` overrides the port;
`CONFIG_SERVER_URL`, `EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE`, and
`MONOLITH_BASE_URL` override the Config Server, Eureka registry, and monolith
addresses respectively. In local host-based development the monolith fallback
is `http://localhost:8089`.

Inside a container, `localhost` refers to that container, not the host or
another application. A future container deployment must supply a resolvable
monolith service address through `MONOLITH_BASE_URL`; no Docker Compose or
container networking is defined here.

## Runtime verification

Dashboard URL:

```text
http://localhost:8761/
```

Registry REST endpoint:

```text
http://localhost:8761/eureka/apps
```

Before the API Gateway starts, there should be no registered applications.
The Discovery Server has `register-with-eureka=false` and
`fetch-registry=false`, so it does not register itself or fetch its own
registry. Config Server is not an Eureka client. After Gateway starts, expect
the `API-GATEWAY` client registration in the registry. Eureka's REST response
may represent an empty registry with an empty application list before then.

To confirm Eureka's external configuration, query Config Server:

```powershell
$config = Invoke-RestMethod http://localhost:8888/discovery-server/default
$config.propertySources[0].source["server.port"]
$config.propertySources[0].source["eureka.client.register-with-eureka"]
$config.propertySources[0].source["eureka.client.fetch-registry"]
```

Expected output:

```text
8761
false
false
```

The Eureka dashboard and registry must not be exposed publicly without
appropriate security and network controls. Authentication/authorization is
intentionally out of scope here.

## API Gateway routing

The Gateway external configuration is `config-repo/api-gateway.yml`. It
registers `api-gateway` with Eureka, imports configuration through Config
Data, disables the discovery route locator, and declares exactly one route:

| Route ID | Match | Filter | Destination |
|---|---|---|---|
| `monolith-fallback` | `/api/**` | `StripPrefix=1` | `${MONOLITH_BASE_URL:http://localhost:8089}` |

For example, `/api/produits/42?details=true` is forwarded to
`/produits/42?details=true` on the monolith. Gateway's proxy forwards the
incoming method and body and relays the upstream response; no routes are
created automatically from registry entries. The Angular app and its
development proxy are intentionally unchanged and continue to target port
8089. Test the Gateway directly before any separate frontend cutover.

Direct GET smoke test using the existing product-list endpoint:

```powershell
curl.exe -i "http://localhost:8080/api/produits"
```

Direct POST smoke test using the existing product-create endpoint and its
documented JSON fields:

```powershell
$body = '{"codeProduit":"GW-SMOKE","libelleProduit":"Gateway smoke test","prix":0}'
curl.exe -i -X POST "http://localhost:8080/api/produits" -H "Content-Type: application/json" -d $body
```

The POST creates a real product in the monolith if accepted; use only in a
disposable or approved development database and remove the created record
through the existing API when appropriate. Gateway should preserve the
upstream status and response body. Query preservation can be checked with:

```powershell
curl.exe -i "http://localhost:8080/api/produits?smoke=true"
```

Gateway is a local-development foundation and must not be exposed publicly.
Eureka registration is service discovery, not authentication or authorization.
Authentication, authorization, rate limiting, and production edge protection
must be designed before public deployment.

## Troubleshooting

- **Cannot find a valid Git repository:** Initialize `config-repo/` with the
  commands above and ensure all three configuration files are committed to
  `main`.
- **Config endpoint is missing properties:** Check
  `http://localhost:8888/discovery-server/default` and confirm
  `discovery-server.yml` is present in the committed `main` branch.
- **Eureka cannot retrieve configuration:** Start Config Server first and
  verify `CONFIG_SERVER_URL` points to an address reachable from the Eureka
  process.
- **Eureka does not start on 8761:** Check whether the port is occupied or set
  `EUREKA_SERVER_PORT`; confirm Eureka's configuration was committed.
  - **Gateway cannot retrieve configuration:** Start Config Server first and
    confirm `http://localhost:8888/api-gateway/default` serves one route and
    the expected Eureka/monolith URLs.
  - **Gateway cannot connect to Eureka or the monolith:** Check the
    `EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE` and `MONOLITH_BASE_URL` values from
    the Gateway process environment. Do not use host `localhost` for another
    container.
  - **Gateway route appears missing:** Confirm `api-gateway.yml` is committed to
    the Config Server's `main` branch and
    `spring.cloud.gateway.server.webflux.discovery.locator.enabled` is `false`.
  - **Maven cannot resolve dependencies:** Check Maven Central access and run
    `.\mvnw.cmd -U clean verify` from `infrastructure/`.
