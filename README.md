# OpenLMIS Stock Management Extension Module
## Prerequisites
* Docker 1.11+

## Quick Start
1. Fork/clone this repository from GitHub.

 ```shell
 git clone https://github.com/villagereach/selv-v3-stockmanagement-extension.git
 ```
2. Fork/clone `openlmis-stockmanagement` repository from GitHub.

 ```shell
 git clone https://github.com/OpenLMIS/openlmis-stockmanagement.git
 ```
3. To assemble the outputs of project and create jar file run `docker-compose -f docker-compose.yml run builder`.
4. In `selv-v3-extensions-config` edit paths to local dependencies. 
5. Run builder for `selv-v3-extensions-config` and build image.
6. Run `selv-v3-ref-distro` and check if your changes has been applied.

## <a name="extensions"></a> Example of extensions usage

#### General information
OpenLMIS allows extending the behavior of a service by deploying additional JARs onto its classpath.
The host service (`openlmis-stockmanagement`) scans the `org.openlmis.stockmanagement` base package on
startup, so any Spring `@Component`, `@Service`, `@RestController`, or `@Repository` class shipped
inside an extension JAR under that package — for example `org.openlmis.stockmanagement.extension` —
is picked up automatically.

This repository ships one feature: a facility-wide **CCE capacity** endpoint used by the
"Available CCE Capacity" indicator on the requisition view. The endpoint is additive — it does not
override any existing controller or extension point in `openlmis-stockmanagement`.

```
GET /api/stockCardSummaries/cce/capacity?facilityId={uuid}
    [&nonEmptyOnly=true] [&orderableId={uuid}&orderableId={uuid}...]

{ "totalVolume": 20, "volumeInUse": 5, "availableVolume": 15 }
```

`totalVolume` is the sum of `netVolume` across all `FUNCTIONING` + `ACTIVE` CCE inventory items at
the facility (fetched from `openlmis-cce`). `volumeInUse` is the sum, across every program at the
facility, of `inBoxCubeDimension × stockOnHand / 1000` for refrigerated approved products (those
with `maximumTemperature ≤ 8°C`), using the same `StockCardSummariesService` machinery that backs
`/api/v2/stockCardSummaries`. `availableVolume = totalVolume − volumeInUse`.

The following classes implement the feature:

- **CceCapacityController.java** — REST controller exposing `/api/stockCardSummaries/cce/capacity`.
- **CceCapacityService.java** — orchestrates the calculation: iterates all programs server-side
  (via a service-to-service auth token, so the result does not depend on the requesting user) and
  sums the volume contributed by each.
- **CceService.java** — calls `openlmis-cce` `/api/inventoryItems/volume` to obtain `totalVolume`.
- **CceOrderableReferenceDataService.java** / **CceProgramReferenceDataService.java** — call
  `openlmis-referencedata` for orderables and programs; both extend the upstream
  `BaseReferenceDataService`, so authenticated calls are handled by the framework.
- **CceCapacityRouteRegistrar.java** — on `ApplicationReadyEvent`, publishes the consul KV entry
  `resources/api/stockCardSummaries/cce/capacity → stockmanagement` so nginx routes the new path
  to this service. No manual `consul kv put` is required.

Configuration:

- `cce.url` — base URL of the CCE service. Defaults to `${BASE_URL}` (the standard service-to-service
  routing path via nginx).
- `CONSUL_HOST` / `CONSUL_PORT` — used by `CceCapacityRouteRegistrar`. Default to `consul` and `8500`.

Tests live under `src/test/java` and run with `gradle test`.

#### Naming convention
Class and Spring bean names should be **unique** and in **UpperCamelCase**. When this extension is
deployed alongside `openlmis-stockmanagement`, every Spring bean is loaded into the same context,
so two beans of the same type sharing a default name would cause a startup conflict. The reference
data shims here are prefixed with `Cce` (e.g. `CceOrderableReferenceDataService`) to avoid
colliding with the identically-typed beans already defined upstream.

* Names should describe the behavior or scope being added — for example
  `CceCapacityService` rather than `CapacityService`.
* When wrapping or shadowing an upstream class, use a prefix that scopes the override to its
  feature — for example `Cce` for everything that supports the CCE capacity endpoint.