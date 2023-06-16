package games.alejandrocoria.mapfrontiers.common.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class EventHandler {
    private static final Map<Object, Consumer<CommandDispatcher<CommandSourceStack>>> commandRegistrationEventMap = new HashMap<>();
    private static final Map<Object, Consumer<MinecraftServer>> serverStartingEventMap = new HashMap<>();
    private static final Map<Object, Consumer<MinecraftServer>> serverStoppingEventMap = new HashMap<>();
    private static final Map<Object, BiConsumer<MinecraftServer, ServerPlayer>> playerJoinedEventMap = new HashMap<>();
    private static final Map<Object, Consumer<MinecraftServer>> serverTickEventMap = new HashMap<>();

    public static void subscribeCommandRegistrationEvent(Object object, Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        commandRegistrationEventMap.put(object, callback);
    }

    public static void subscribeServerStartingEvent(Object object, Consumer<MinecraftServer> callback) {
        serverStartingEventMap.put(object, callback);
    }

    public static void subscribeServerStoppingEvent(Object object, Consumer<MinecraftServer> callback) {
        serverStoppingEventMap.put(object, callback);
    }

    public static void subscribePlayerJoinedEvent(Object object, BiConsumer<MinecraftServer, ServerPlayer> callback) {
        playerJoinedEventMap.put(object, callback);
    }

    public static void subscribeServerTickEvent(Object object, Consumer<MinecraftServer> callback) {
        serverTickEventMap.put(object, callback);
    }


    public static void unsuscribeAllEvents(Object object) {
        commandRegistrationEventMap.remove(object);
        serverStartingEventMap.remove(object);
        serverStoppingEventMap.remove(object);
        playerJoinedEventMap.remove(object);
        serverTickEventMap.remove(object);
    }


    public static void postCommandRegistrationEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Consumer<CommandDispatcher<CommandSourceStack>> callback : commandRegistrationEventMap.values()) {
            callback.accept(dispatcher);
        }
    }

    public static void postServerStartingEvent(MinecraftServer server) {
        for (Consumer<MinecraftServer> callback : serverStartingEventMap.values()) {
            callback.accept(server);
        }
    }

    public static void postServerStoppingEvent(MinecraftServer server) {
        for (Consumer<MinecraftServer> callback : serverStoppingEventMap.values()) {
            callback.accept(server);
        }
    }

    public static void postPlayerJoinedEvent(MinecraftServer server, ServerPlayer player) {
        for (BiConsumer<MinecraftServer, ServerPlayer> callback : playerJoinedEventMap.values()) {
            callback.accept(server, player);
        }
    }

    public static void postServerTickEvent(MinecraftServer server) {
        for (Consumer<MinecraftServer> callback : serverTickEventMap.values()) {
            callback.accept(server);
        }
    }
}
