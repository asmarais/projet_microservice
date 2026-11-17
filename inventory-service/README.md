# Inventory Service

Standalone Stock and product-assignment service. It uses its own MySQL
database (`inventory_db`) and registers with Eureka as `inventory-service`.
Product references are stored as scalar `productId` values; product existence
is checked through Product Service for assignment requests.

## Local configuration

The service retrieves configuration from Config Server. Start Config Server,
Eureka, MySQL, and Product Service before the Inventory Service. Configuration
can be overridden with:

- `CONFIG_SERVER_URL`
- `INVENTORY_SERVICE_PORT`
- `INVENTORY_DB_URL`
- `INVENTORY_DB_USERNAME`
- `INVENTORY_DB_PASSWORD`
- `EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE`

Default service port is `8083`. Flyway creates the service-owned schema;
existing/production data is not migrated.

## Build and tests

```powershell
.\mvnw.cmd clean verify
```
