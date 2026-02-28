# MapFrontiers API architecture proposal (JourneyMap-style)

This document describes a concrete structure for extracting a public API that third-party mods/plugins can consume at compile time, while keeping platform-specific implementation in MapFrontiers.

## Goals

- Publish a standalone `mapfrontiers-api` artifact for external consumers.
- Keep API types free of loader/runtime classes (`ServerPlayer`, `Level`, Bukkit `Player`, etc.).
- Split API surface by side:
  - server-side capabilities (global frontiers, permissions, administration)
  - client-side capabilities (personal frontiers, client-only interactions)
- Keep usage style close to JourneyMap APIs:
  - plugin-style initialization
  - side-specific API handles
  - event subscription callbacks

## Recommended modules

```text
MapFrontiers/
  Api/                      <-- new Gradle module, published
    src/main/java/.../api
  Common/                   <-- core implementation using Api contracts
  Fabric/
  Forge/
  NeoForge/
  Paper/                    <-- future adapter module
```

### Build separation

- `Api` should be a plain `java-library` module.
- `Common` depends on `Api` and implements contracts.
- loader modules (`Fabric`, `Forge`, `NeoForge`, future `Paper`) expose runtime glue and provider bootstrap.

## Public API package layout

```text
games.alejandrocoria.mapfrontiers.api
  MapFrontiersAPI                     (entry point)
  plugin/
    IMapFrontiersPlugin
    IMapFrontiersClientPlugin
    IMapFrontiersServerPlugin
  client/
    IMapFrontiersClientAPI
    ClientFrontierService
  server/
    IMapFrontiersServerAPI
    ServerFrontierService
    ServerPermissionService
  model/
    FrontierId
    FrontierType (GLOBAL, PERSONAL)
    DimensionId
    FrontierShape
    FrontierDataView
    FrontierMutation
  event/
    FrontierCreatedEvent
    FrontierUpdatedEvent
    FrontierDeletedEvent
```

## Core contracts (suggested signatures)

```java
public final class MapFrontiersAPI {
    public static Optional<IMapFrontiersServerAPI> getServerAPI();
    public static Optional<IMapFrontiersClientAPI> getClientAPI();

    // JourneyMap-like plugin registration style
    public static void registerClientPlugin(IMapFrontiersClientPlugin plugin);
    public static void registerServerPlugin(IMapFrontiersServerPlugin plugin);
}
```

```java
public interface IMapFrontiersClientPlugin {
    String getModId();
    void initialize(IMapFrontiersClientAPI api);
}

public interface IMapFrontiersServerPlugin {
    String getModId();
    void initialize(IMapFrontiersServerAPI api);
}
```

```java
public interface IMapFrontiersServerAPI {
    ServerFrontierService frontiers();
    ServerPermissionService permissions();
    EventBus events();
}

public interface IMapFrontiersClientAPI {
    ClientFrontierService frontiers();
    EventBus events();
}
```

```java
public interface ServerFrontierService {
    FrontierDataView createGlobalFrontier(DimensionId dimension, FrontierShape shape, ActorRef actor);
    Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation, ActorRef actor);
    boolean deleteGlobalFrontier(FrontierId frontierId, ActorRef actor);
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);
    List<FrontierDataView> listGlobalFrontiers(DimensionId dimension);
}
```

```java
public interface ClientFrontierService {
    FrontierDataView createPersonalFrontier(DimensionId dimension, FrontierShape shape);
    Optional<FrontierDataView> updatePersonalFrontier(FrontierId frontierId, FrontierMutation mutation);
    boolean deletePersonalFrontier(FrontierId frontierId);
    List<FrontierDataView> listPersonalFrontiers(DimensionId dimension);
}
```

## How this maps to current MapFrontiers logic

- `ServerFrontierService` delegates to existing global operations in `FrontiersManager`.
- `ClientFrontierService` delegates to personal frontier flows and packet-based synchronization.
- Permission checks stay server-authoritative via existing `FrontierSettings` checks.

## Side boundaries

- `server/*` APIs must never be callable from pure client-only runtime.
- `client/*` APIs should avoid granting authority over global data.
- Any client request that impacts shared state must be validated server-side.

## JourneyMap-style usage examples

### Client plugin

```java
public final class MyClientPlugin implements IMapFrontiersClientPlugin {
    @Override
    public String getModId() {
        return "myaddon";
    }

    @Override
    public void initialize(IMapFrontiersClientAPI api) {
        api.events().subscribe(FrontierCreatedEvent.class, event -> {
            // react to visibility/UI updates
        });
    }
}
```

### Server plugin

```java
public final class MyServerPlugin implements IMapFrontiersServerPlugin {
    @Override
    public String getModId() {
        return "myaddon";
    }

    @Override
    public void initialize(IMapFrontiersServerAPI api) {
        api.events().subscribe(FrontierDeletedEvent.class, event -> {
            // audit or sync with claims/economy plugin
        });
    }
}
```

## Migration plan

1. Create `Api` module and publish first `0.x` artifact.
2. Add neutral model objects and side-specific API interfaces.
3. Implement adapters in `Common` using existing `FrontiersManager`/network code.
4. Bootstrap provider registration from loader modules.
5. Add `Paper` module reusing API contracts and business rules.
6. Freeze API (`1.0`) once external integrations validate the model.

## Versioning policy

- Semantic versioning for `mapfrontiers-api`.
- Breaking changes only on major bumps.
- Mark unstable contracts with `@ApiStatus.Experimental` until stable.
