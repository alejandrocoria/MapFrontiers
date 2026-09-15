package games.alejandrocoria.mapfrontiers.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDebugControls;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class FabricClientCommandPacketDebug {
    private FabricClientCommandPacketDebug() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("mfdebug")
                .then(ClientCommandManager.literal("packets")
                        .then(ClientCommandManager.literal("delay")
                                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(context -> ClientPacketDebugControls.setDelay(
                                                IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(ClientCommandManager.literal("hold").executes(context -> ClientPacketDebugControls.hold()))
                        .then(ClientCommandManager.literal("resume").executes(context -> ClientPacketDebugControls.resume()))
                        .then(ClientCommandManager.literal("next").executes(context -> ClientPacketDebugControls.runNext()))
                        .then(ClientCommandManager.literal("flush").executes(context -> ClientPacketDebugControls.flush()))
                        .then(ClientCommandManager.literal("clear").executes(context -> ClientPacketDebugControls.clear()))));
    }
}
