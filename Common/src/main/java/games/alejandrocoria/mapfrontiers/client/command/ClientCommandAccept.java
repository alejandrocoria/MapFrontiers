package games.alejandrocoria.mapfrontiers.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.AcceptFrontierCopyDialog;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClientCommandAccept {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = dispatcher
                .register(Commands.literal("mapfrontiersacceptcopy")
                        .then(Commands.argument("invitation id", IntegerArgumentType.integer(0, 999)).executes(
                                (commandSource) -> acceptInvitation(commandSource.getSource(),
                                        IntegerArgumentType.getInteger(commandSource, "invitation id")))
                        )
                );

        dispatcher.register(Commands.literal("mfacceptcopy").redirect(literalcommandnode)
        );
    }

    public static int acceptInvitation(CommandSourceStack source, int messageID) {
        FrontierData receivedFrontier = ChatFrontiers.getReceivedFrontier(messageID);
        if (receivedFrontier == null) {
            source.sendFailure(Component.literal("The frontier no longer exists"));
            return messageID;
        }

        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay currentFrontier = manager.getFrontierCopiedFrom(receivedFrontier.getCopiedFromId());
        Minecraft.getInstance().setScreen(null);
        Minecraft.getInstance().schedule(() -> new AcceptFrontierCopyDialog(messageID, receivedFrontier, currentFrontier).display());

        return messageID;
    }
}
