package games.alejandrocoria.mapfrontiers.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDebugControls;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class FabricClientCommandPacketDebug {
    private FabricClientCommandPacketDebug() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("mfdebug")
                .then(ClientCommands.literal("packets")
                        .then(ClientCommands.literal("delay")
                                .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(context -> ClientPacketDebugControls.setDelay(
                                                IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(ClientCommands.literal("hold").executes(context -> ClientPacketDebugControls.hold()))
                        .then(ClientCommands.literal("resume").executes(context -> ClientPacketDebugControls.resume()))
                        .then(ClientCommands.literal("next").executes(context -> ClientPacketDebugControls.runNext()))
                        .then(ClientCommands.literal("flush").executes(context -> ClientPacketDebugControls.flush()))
                        .then(ClientCommands.literal("clear").executes(context -> ClientPacketDebugControls.clear()))));
    }
}
