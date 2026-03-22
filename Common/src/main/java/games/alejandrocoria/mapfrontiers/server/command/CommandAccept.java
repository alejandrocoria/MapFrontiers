package games.alejandrocoria.mapfrontiers.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class CommandAccept {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = dispatcher
                .register(Commands.literal("mapfrontiersaccept").requires(CommandAccept::checkPermission)
                        .then(Commands.argument("invitation id", IntegerArgumentType.integer(0, 999)).executes(
                                (commandSource) -> acceptInvitation(commandSource.getSource(),
                                        IntegerArgumentType.getInteger(commandSource, "invitation id")))
                        )
                );

        dispatcher.register(Commands.literal("mfaccept").requires(CommandAccept::checkPermission)
                .redirect(literalcommandnode)
        );
    }

    public static int acceptInvitation(CommandSourceStack source, int messageID) throws CommandSyntaxException {
        Level world = source.getLevel();
        if (!world.isClientSide()) {
            if (MapFrontiers.getServerRuntime() == null) {
                return messageID;
            }

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .acceptShareInvitation(source.getPlayerOrException(), messageID);
            if (result.isSuccess()) {
                result.dispatchNetworkActions();
                source.sendSuccess(() ->
                        Component.literal("Accepting frontier " + result.getFrontier().getName1() + " " + result.getFrontier().getName2()),
                        false);
                return messageID;
            }

            if (result.getReason() == ServerFrontierOperationResult.Reason.InvitationExpired) {
                source.sendFailure(Component.literal("Invitation expired"));
            } else if (result.getReason() == ServerFrontierOperationResult.Reason.FrontierMissing) {
                source.sendFailure(Component.literal("The frontier no longer exists"));
            } else if (result.getReason() == ServerFrontierOperationResult.Reason.SharedUserMissing) {
                source.sendFailure(Component.literal(""));
            } else if (result.getReason() == ServerFrontierOperationResult.Reason.AlreadyAccepted) {
                source.sendFailure(Component.literal("You already have the frontier"));
            } else if (result.getReason() == ServerFrontierOperationResult.Reason.WrongTarget) {
                source.sendFailure(Component.literal("The invitation is for another player"));
            } else {
                source.sendFailure(Component.literal("The frontier cannot be accepted"));
            }
        }

        return messageID;
    }

    public static boolean checkPermission(CommandSourceStack source) {
        try {
            return MapFrontiers.getServerRuntime() != null
                    && MapFrontiers.getServerRuntime().getShareService().canSendCommandAcceptFrontier(source.getPlayerOrException());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }
}
