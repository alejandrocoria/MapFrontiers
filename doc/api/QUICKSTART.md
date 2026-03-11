# MapFrontiers API Quickstart

This document is a short introduction for a first user of the MapFrontiers API.

MapFrontiers exposes two API entry points:

- client API: for client-side actions such as creating or updating frontiers from a mod running on the client
- server API: for server-side actions such as listing and creating global frontiers

Plugins register themselves through `MapFrontiersAPI` and receive an API instance in `initialize(...)`.

## Add the dependency

The API is published on Modrinth at <https://modrinth.com/mod/mapfrontiers-api>. A typical Gradle setup looks like this:

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}

dependencies {
    implementation "maven.modrinth:mapfrontiers-api:0.1.0-SNAPSHOT"
}
```

The API artifact is intended to be lightweight and independent of Minecraft and modloader runtime classes.

## Register a client plugin

In a client-side entry point of your mod, register your plugin once during mod initialization:

```java
import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPI;

public final class ExampleClientModEntrypoint {
    public static void register() {
        MapFrontiersAPI.registerClientPlugin(new ExampleClientPlugin());
    }
}
```

Then implement a client plugin:

```java
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersClientPlugin;

import java.util.List;

public final class ExampleClientPlugin implements IMapFrontiersClientPlugin {
    private EventBus.Subscription createdSubscription;

    @Override
    public String getModId() {
        return "examplemod";
    }

    @Override
    public void initialize(IMapFrontiersClientAPI api) {
        createdSubscription = api.events().subscribe(FrontierCreatedEvent.class, this::onFrontierCreated);

        FrontierActionResult result = api.frontiers().createPersonalFrontier(
                new DimensionId("minecraft:overworld"),
                FrontierShape.vertex(List.of(
                        new Point2i(0, 0),
                        new Point2i(100, 0),
                        new Point2i(100, 100),
                        new Point2i(0, 100)
                ))
        );

        result.frontierId().ifPresent(frontierId ->
                api.frontiers().updatePersonalFrontier(frontierId, FrontierMutation.name1("Spawn"))
        );
    }

    @Override
    public void shutdown(IMapFrontiersClientAPI api) {
        if (createdSubscription != null) {
            createdSubscription.unsubscribe();
            createdSubscription = null;
        }
    }

    private void onFrontierCreated(FrontierCreatedEvent event) {
        System.out.println("Frontier created: " + event.frontier().id().value());
    }
}
```

Important notes for client plugins:

- many client actions are asynchronous and return `ACCEPTED_ASYNC`
- in singleplayer, requests still go through the logical server
- `FrontierDataView` is a snapshot, not a live object
- sharing from the client API requires MapFrontiers to be present on the server

## Register a server plugin

In a server-side entry point of your mod, register your server plugin:

```java
import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPI;

public final class ExampleServerModEntrypoint {
    public static void register() {
        MapFrontiersAPI.registerServerPlugin(new ExampleServerPlugin());
    }
}
```

Then implement the plugin:

```java
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersServerPlugin;
import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;

import java.util.List;
import java.util.UUID;

public final class ExampleServerPlugin implements IMapFrontiersServerPlugin {
    @Override
    public String getModId() {
        return "examplemod";
    }

    @Override
    public void initialize(IMapFrontiersServerAPI api) {
        api.frontiers().createGlobalFrontier(
                new UserRef(UUID.fromString("11111111-1111-1111-1111-111111111111"), "ServerAdmin"),
                new DimensionId("minecraft:overworld"),
                FrontierShape.vertex(List.of(
                        new Point2i(-50, -50),
                        new Point2i(50, -50),
                        new Point2i(50, 50),
                        new Point2i(-50, 50)
                ))
        );

        int globalCount = api.frontiers().listGlobalFrontiers(new DimensionId("minecraft:overworld")).size();
        System.out.println("Global frontiers in overworld: " + globalCount);
    }

    @Override
    public void shutdown(IMapFrontiersServerAPI api) {
    }
}
```

Important notes for server plugins:

- the server API works on authoritative server state immediately
- at the moment it is focused on global frontiers
- global frontier creation requires an explicit owner

## Basic concepts

- `FrontierShape` describes the frontier geometry, either by vertices or by chunks
- `FrontierMutation` is used for partial updates
- `FrontierDataView` is the read-only view returned by the API
- `EventBus` lets both client and server plugins react to created, updated and deleted frontiers

## Current limitations

- each frontier name field is currently limited to 17 characters
- client-side sharing methods require MapFrontiers to be present on the server
- when MapFrontiers is not present on the server, the GUI can send a copy of a frontier by chat, but that flow is not available through the API

## Next step

After this quickstart, the next thing to look at is the Javadoc in the API interfaces and models:

- `MapFrontiersAPI`
- `IMapFrontiersClientAPI`
- `IMapFrontiersServerAPI`
- `ClientFrontierService`
- `ServerFrontierService`
- `FrontierMutation`
- `FrontierShape`
- `FrontierDataView`
