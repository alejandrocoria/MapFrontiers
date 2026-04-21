package games.alejandrocoria.mapfrontiers.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.AcceptFrontierCopyDialog;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// Copied from ClientCommandAccept because Fabric uses its own classes for client commands
public class FabricClientCommandAccept {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> literalcommandnode = dispatcher
                .register(ClientCommandManager.literal("mapfrontiersacceptcopy")
                        .then(ClientCommandManager.argument("invitation id", IntegerArgumentType.integer(0, 999)).executes(
                                (commandSource) -> acceptInvitation(commandSource.getSource(),
                                        IntegerArgumentType.getInteger(commandSource, "invitation id")))
                        )
                );

        dispatcher.register(ClientCommandManager.literal("mfacceptcopy").redirect(literalcommandnode)
        );
    }

    public static int acceptInvitation(FabricClientCommandSource source, int messageID) {
        FrontierData receivedFrontier = ChatFrontiers.getReceivedFrontier(messageID);
        if (receivedFrontier == null) {
            source.sendError(Component.literal("The frontier no longer exists"));
            return messageID;
        }

        FrontierOverlay currentFrontier = MapFrontiersClient.getCopiedPersonalFrontier(receivedFrontier.getCopiedFromId());
        Minecraft.getInstance().setScreen(null);
        Minecraft.getInstance().schedule(() -> new AcceptFrontierCopyDialog(messageID, receivedFrontier, currentFrontier).display());

        return messageID;
    }
}
