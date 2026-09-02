package games.alejandrocoria.mapfrontiers.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDebugControls;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class ClientCommandPacketDebug {
    private ClientCommandPacketDebug() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mfdebug")
                .then(Commands.literal("packets")
                        .then(Commands.literal("delay")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(context -> ClientPacketDebugControls.setDelay(
                                                IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(Commands.literal("hold").executes(context -> ClientPacketDebugControls.hold()))
                        .then(Commands.literal("resume").executes(context -> ClientPacketDebugControls.resume()))
                        .then(Commands.literal("next").executes(context -> ClientPacketDebugControls.runNext()))
                        .then(Commands.literal("flush").executes(context -> ClientPacketDebugControls.flush()))
                        .then(Commands.literal("clear").executes(context -> ClientPacketDebugControls.clear()))));
    }
}
