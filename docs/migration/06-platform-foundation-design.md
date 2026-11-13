# Spring Cloud Platform Foundation Design

**Status:** Design for approval only. No infrastructure applications, Maven
files, Gateway routes, application source, or runtime configuration have been
created or changed.

This design supports the three infrastructure applications around the six
business services in
[the revised target architecture](./02-revised-target-architecture.md). The
existing monolith remains an independent application.

## 1. Verified dependency compatibility

### Spring Boot / Spring Cloud matrix

The official Spring Cloud release documentation identifies **Spring Cloud
2025.1.4** as a release train supporting **Spring Boot 4.0.9**. The official
release page lists Config 5.0.6, Netflix 5.0.3, Gateway 5.0.4, and OpenFeign
5.0.4 in that train. The 2025.1.4 BOM and these module POMs are published in
Maven Central.

| Candidate | Verified status |
|---|---|
| Existing monolith: Spring Boot 4.1.1 | Current repository setting; leave untouched |
| Spring Cloud 2025.1.4 | GA release train; official supported Boot version is 4.0.9 |
| Spring Boot 4.1.1 + Spring Cloud 2025.1.4 | **Not an officially supported combination**; do not assume compatibility |
| Spring Boot 4.0.9 + Spring Cloud 2025.1.4 | Supported combination for new Cloud applications |
| Spring Cloud 2026.0 prerelease | Excluded; this design does not select a release candidate |

The direct compatibility answer is therefore **not to put Spring Cloud
2025.1.4 on the current Boot 4.1.1 application**. Before implementation,
approve one of these paths:

1. **Recommended for starting on a GA train:** Keep the monolith on Boot
   4.1.1 and create the new Config Server, Eureka Server, Gateway, and
   Cloud-using future services on Boot 4.0.9 with Cloud 2025.1.4. These are
   independently built and deployed applications, but use different Boot
   minor lines. This is a deliberate choice for new applications, not a
   downgrade of the monolith.
2. **If a single Boot version across applications is mandatory:** Defer
   Spring Cloud implementation until an official GA Spring Cloud release
   train supports Boot 4.1.x. Do not fill the gap with a 2026.0 release
   candidate or by forcing the 2025.1.4 BOM onto Boot 4.1.1.

Boot 4.0.9 is a **minor-line downgrade** from the stated Boot 4.1.1 if applied
to a new application; it is not a patch-only upgrade recommendation. No
Spring Boot version change is proposed for the existing monolith. Boot 4.0.9's
Maven parent is available in Maven Central and Java 21 can be selected in the
new Maven parent configuration.

### BOM-managed components and Maven Central checks

The versions below come from the Spring Cloud 2025.1.4 BOM's imported release
train modules. Direct Maven Central POM URLs were checked for the listed
artifacts; they are not snapshot or milestone coordinates. Application POMs
should omit the individual dependency `<version>` when importing the Cloud
BOM.

| Capability | Dependency coordinates | BOM-managed version | Use |
|---|---|---:|---|
| Config Server | `org.springframework.cloud:spring-cloud-config-server` | 5.0.6 | `config-server` |
| Config Client | `org.springframework.cloud:spring-cloud-starter-config` | 5.0.6 | Eureka, Gateway, and future Cloud-using services |
| Eureka Server | `org.springframework.cloud:spring-cloud-starter-netflix-eureka-server` | 5.0.3 | `discovery-server` |
| Eureka Client | `org.springframework.cloud:spring-cloud-starter-netflix-eureka-client` | 5.0.3 | Gateway and future registered services |
| Gateway Server (WebFlux) | `org.springframework.cloud:spring-cloud-starter-gateway-server-webflux` | 5.0.4 | `api-gateway` |
| OpenFeign | `org.springframework.cloud:spring-cloud-starter-openfeign` | 5.0.4 | Future business services that make declarative service calls |
| Spring Cloud LoadBalancer | `org.springframework.cloud:spring-cloud-starter-loadbalancer` | 5.0.4 | Gateway `lb://` routes and future load-balanced clients |

The BOM also manages Eureka libraries at 2.0.6. Do not declare or override
those transitive versions directly. OpenFeign and LoadBalancer are listed for
compatibility planning; they are **not needed in the three infrastructure
applications** except LoadBalancer in the Gateway for discovery-based routes.
Do not add OpenFeign to Config Server, Eureka Server, or Gateway without a
specific use case.

### Gateway starter and application model for Boot 4

For the API Gateway, use the **Gateway Server WebFlux** starter:
`spring-cloud-starter-gateway-server-webflux:5.0.4`. It is the Boot 4-era
starter artifact explicitly documented for the Gateway Server variant. This
creates a reactive WebFlux application using Reactor and Netty.

Do not use the older `spring-cloud-starter-gateway` coordinate in this
foundation. Do not combine the Gateway WebFlux server with Spring MVC or
`spring-boot-starter-webmvc` in the same Gateway application. The Gateway
WebMVC variant (`spring-cloud-starter-gateway-server-webmvc`) exists, but
WebFlux is the recommended model here for a dedicated proxy application and
for consistency with the documented Gateway Server model.

### Sources

- [Spring Cloud 2025.1 reference and supported Boot version](https://docs.spring.io/spring-cloud-release/reference/2025.1/index.html)
- [Spring Cloud 2025.1.4 official release notes](https://github.com/spring-cloud/spring-cloud-release/releases/tag/v2025.1.4)
- [Spring Cloud Gateway starter documentation](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html)
- [Spring Cloud Gateway project documentation](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud Config project documentation](https://docs.spring.io/spring-cloud-config/reference/)
- [Spring Cloud 2025.1.4 BOM in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-dependencies/2025.1.4/spring-cloud-dependencies-2025.1.4.pom)
- [Boot 4.0.9 parent in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.0.9/spring-boot-starter-parent-4.0.9.pom)
- [Config Server 5.0.6 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-config-server/5.0.6/spring-cloud-config-server-5.0.6.pom)
- [Config Client 5.0.6 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-config/5.0.6/spring-cloud-starter-config-5.0.6.pom)
- [Eureka Server 5.0.3 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-netflix-eureka-server/5.0.3/spring-cloud-starter-netflix-eureka-server-5.0.3.pom)
- [Eureka Client 5.0.3 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-netflix-eureka-client/5.0.3/spring-cloud-starter-netflix-eureka-client-5.0.3.pom)
- [Gateway WebFlux starter 5.0.4 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-gateway-server-webflux/5.0.4/spring-cloud-starter-gateway-server-webflux-5.0.4.pom)
- [OpenFeign starter 5.0.4 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-openfeign/5.0.4/spring-cloud-starter-openfeign-5.0.4.pom)
- [LoadBalancer starter 5.0.4 in Maven Central](https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-starter-loadbalancer/5.0.4/spring-cloud-starter-loadbalancer-5.0.4.pom)

## 2. Recommended Maven structure

Keep the current `backend/` project and its `pom.xml` unchanged. Put the
independently deployable platform applications under a separate infrastructure
root:

```text
backend/                         # Existing monolith; no changes
infrastructure/
  pom.xml                        # Infrastructure parent + reactor aggregator
  mvnw
  mvnw.cmd
  .mvn/wrapper/                  # Pin Maven Wrapper to 3.10.0
  config-server/
    pom.xml
    src/main/java/
    src/main/resources/
  discovery-server/
    pom.xml
    src/main/java/
    src/main/resources/
  api-gateway/
    pom.xml
    src/main/java/
    src/main/resources/
config-repo/                     # Externalized application configuration only
```

`infrastructure/pom.xml` should use `spring-boot-starter-parent` as its
parent, set Java release 21, import the Spring Cloud 2025.1.4 BOM in
`dependencyManagement`, and aggregate only the three infrastructure modules.
Each child has its own Spring Boot Maven plugin and application dependencies,
and inherits the shared parent with a relative parent path. A child can thus
be built on its own or from the reactor, while version policy remains
centralized for these three projects.

Pin a Maven Wrapper for this new build root to Maven 3.10.0 (the version
already used by `backend/`), instead of depending on whichever Maven happens
to be installed. Keep the existing backend wrapper independent. This makes
the infrastructure builds repeatable without extending or changing the
monolith's build.

Do not add the six future business services to this aggregator. If they later
need this same parent, decide whether to promote the version policy into a
separately reusable platform parent or keep their POMs independently managed;
that choice should not couple their deployable artifacts to the infrastructure
reactor.

## 3. Dependency table by application

| Application | Minimum direct Spring dependencies | Intentionally not included |
|---|---|---|
| `config-server` | `spring-cloud-config-server` | Config Client, Eureka Client, Gateway, OpenFeign, database drivers |
| `discovery-server` | `spring-cloud-starter-netflix-eureka-server`; `spring-cloud-starter-config` | Eureka Client as a separate declaration unless required by the server starter, Gateway, OpenFeign, database drivers |
| `api-gateway` | `spring-cloud-starter-gateway-server-webflux`; `spring-cloud-starter-netflix-eureka-client`; `spring-cloud-starter-loadbalancer`; `spring-cloud-starter-config` | Spring MVC/WebMVC, OpenFeign, JPA, MySQL driver |
| Future registered business service (reference only) | `spring-cloud-starter-config`; `spring-cloud-starter-netflix-eureka-client`; add OpenFeign and LoadBalancer only for declarative load-balanced calls | Gateway Server |

Add Spring Boot Actuator only when health/metrics endpoints are intentionally
configured and secured. No tracing, messaging, database, security, or
observability dependency is required to establish these three applications'
initial discovery/configuration/routing foundation.

## 4. Common platform configuration

| Setting | Recommended baseline |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.9 for applications using Spring Cloud 2025.1.4; this choice requires approval and does not change the existing monolith at 4.1.1 |
| Spring Cloud BOM | `org.springframework.cloud:spring-cloud-dependencies:2025.1.4` |
| Maven | Wrapper 3.10.0 at `infrastructure/`; one `pom.xml` per application |
| Dependency management | Boot parent for Boot/plugin management; import Cloud BOM once in the infrastructure parent; omit child dependency versions |
| Config Server | 8888 |
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Existing monolith | 8089 |

Port values should be configurable through environment variables in each app,
with the values above as local-development defaults. The backend remains
reachable directly on 8089 during migration; it is not moved behind the
Gateway until the frontend proxy is deliberately switched.

## 5. Configuration loading strategy

- Use Config Server as the central property source for the three platform
  apps and, later, the six business services.
- Keep shared defaults in `config-repo/application.yml` and app-specific
  configuration in `config-server.yml`, `discovery-server.yml`, and
  `api-gateway.yml`. Use explicit environment profiles for local/test/
  deployment differences.
- Use a Git-backed Config Server repository for repeatable environments.
  `config-repo/` may be a separate repository or a deliberately isolated
  tracked configuration directory. Keep passwords, tokens, private keys,
  and production connection strings out of Git; inject secrets using the
  deployment environment or a dedicated secret manager.
- The Config Server must bootstrap from its own local environment; it must
  not require Config Server to start. This avoids a bootstrap dependency loop.
- Other applications should use Spring Boot's Config Data import mechanism
  (`spring.config.import=configserver:...`). In shared environments, prefer a
  required import so an unavailable/stale central configuration fails
  startup clearly. If local development uses an optional import, keep safe
  local defaults and make the fallback explicit rather than silently
  accepting production defaults.
- Start Config Server before Eureka and Gateway. Define readiness checks and
  startup ordering in a later deployment configuration; do not assume that
  container start order means the Config Server is ready.
- Do not use legacy bootstrap configuration unless a specific feature
  requires it and is verified against the selected release train.

## 6. Eureka registration strategy

- Enable Eureka Server only in `discovery-server` and host its dashboard/API
  on port 8761.
- Configure the Eureka Server not to register itself and not to fetch its own
  registry (`register-with-eureka=false`, `fetch-registry=false`).
- Do not register Config Server with Eureka. It is the bootstrap dependency
  used before clients can load configuration.
- Set a stable `spring.application.name` for each application. The Gateway
  and future business services register as Eureka clients and fetch the
  registry; `discovery-server` is the registry.
- Use Gateway `lb://<service-id>` destinations only for extracted services.
  Include Spring Cloud LoadBalancer so those IDs resolve through Eureka.
- In local development, set explicit advertised host/port values where
  automatic host discovery would advertise an unreachable container or
  network address.
- Keep Gateway's discovery-based automatic route locator disabled. Discovery
  must not make every registered service route public by default; expose
  reviewed routes only.

## 7. Gateway routing strategy (design only)

No route configuration is to be created yet. Preserve the existing REST paths
at the public boundary and remove `/api` inside the Gateway before forwarding:

```text
Browser:  /api/produits/42
Gateway:  matches /api/produits/**, strips the first path segment
Service:  /produits/42
```

Use explicit, domain-scoped routes for extracted services (for example
`/api/produits/**` and `/api/stocks/**`) before a final legacy fallback route
for `/api/**`. Apply the same one-segment prefix removal to each. The fallback
targets the monolith at port 8089 and strips `/api`; as domains are extracted,
replace their explicit route destination with the corresponding Eureka-backed
service while leaving unextracted domains on the monolith. Preserve methods,
query strings, request/response bodies, status codes, and headers needed by
the existing API contracts.

Define explicit route ordering and test representative overlapping paths so
the `/api/**` fallback cannot shadow a more specific service route. Avoid a
catch-all discovery locator. Keep the monolith fallback target externalized
(for example, `MONOLITH_BASE_URL`): `localhost:8089` is correct from the
developer host, but usually incorrect from inside a container.

Gateway routing is a migration bridge, not permission to change business
behavior. Baseline contract and parity tests should be used for each route
before switching its destination.

## 8. Frontend compatibility strategy

The Angular application currently calls relative `/api` URLs. The development
proxy strips `/api` and targets backend port 8089, as recorded in
[the baseline](./03-baseline-validation.md) and
[the existing API contracts](./04-existing-api-contracts.md). Do not change
Angular code or `frontend/proxy.conf.json` as part of this design task.

When the Gateway is implemented and validated, change only the development
proxy target from port 8089 to port 8080 and remove its `/api` rewrite. The
Gateway then receives `/api/...` unchanged and performs the single prefix
removal itself. Do not leave both proxy and Gateway stripping `/api`, which
would turn e.g. `/api/produits` into an incorrect path. In deployed
environments, send `/api` to the Gateway through the edge reverse proxy using
the same single-strip behavior.

Keep client-visible URLs, request/response shapes, and status codes stable
through extraction. Direct access to the monolith on 8089 can remain available
for controlled troubleshooting, but the normal frontend path should move to
the Gateway only after fallback routing and health checks pass.

## 9. Risks and implementation recommendations

1. **Version-line mismatch is the approval gate.** Cloud 2025.1.4 supports
   Boot 4.0.9, while the current monolith uses Boot 4.1.1. Do not build the
   infrastructure on an unsupported pairing. Approve separate Boot lines for
   new Cloud applications or wait for a GA train supporting Boot 4.1.x.
2. **Independent versions require separate validation.** If the new
   applications use Boot 4.0.9 while the monolith stays on 4.1.1, test
   protocol-level API compatibility and independently pin/scan each runtime.
   No monolith dependency update is implied.
3. **Config Server is sensitive infrastructure.** Authenticate access outside
   local development, protect its endpoints, avoid committing secrets, and
   avoid exposing environment/configuration endpoints publicly.
4. **Gateway fallback changes the network path.** Validate route precedence,
   path stripping exactly once, query/header forwarding, connection timeouts,
   and failure behavior before the frontend is pointed at it.
5. **Discovery is not an API authorization boundary.** Disable automatic
   routes and explicitly secure public routes before exposing the Gateway
   outside local development.
6. **Container networking differs from host networking.** Use a resolvable
   service address for Eureka, Config Server, and the monolith fallback in
   each environment; avoid assuming `localhost` inside containers.
7. **The current backend test baseline has one Docker/Testcontainers startup
   error.** Resolve availability of Docker in the validation environment or
   otherwise record that limitation when establishing platform integration
   tests; do not alter the monolith tests as a side effect of this design.
8. **Implementation sequence:** approve the Boot/Cloud line; create the
   separate Maven parent/wrapper and Config Server; validate config retrieval;
   add Eureka and validate registration; add Gateway with only a monolith
   fallback; verify frontend proxy behavior; then add explicit service routes
   incrementally with contract tests. Do not generate any business service in
   the infrastructure-foundation step.
