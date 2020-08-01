package games.alejandrocoria.mapfrontiers.common.command;

import java.util.Arrays;
import java.util.List;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.PendingShareFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommandAccept extends CommandBase {
    private static final List<String> aliases = Arrays.asList("mfaccept");

    @Override
    public String getName() {
        return "mapfrontiersaccept";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/mfaccept <invitation id>";
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (!world.isRemote) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender), new Object[0]);
            }

            int messageID = parseInt(args[0]);
            PendingShareFrontier pending = FrontiersManager.instance.getPendingShareFrontier(messageID);

            if (pending == null) {
                throw new CommandException("Invitation expired", new Object[0]);
            } else {
                if (pending.targetUser.equals(new SettingsUser(getCommandSenderAsPlayer(sender)))) {
                    FrontierData frontier = FrontiersManager.instance.getFrontierFromID(pending.frontierID);
                    if (frontier == null) {
                        FrontiersManager.instance.removePendingShareFrontier(messageID);
                        throw new CommandException("The frontier no longer exists", new Object[0]);
                    }

                    SettingsUserShared userShared = frontier.getUserShared(pending.targetUser);
                    if (userShared == null) {
                        throw new CommandException("", new Object[0]);
                    }


                    if (FrontiersManager.instance.hasPersonalFrontier(pending.targetUser, frontier.getId())) {
                        FrontiersManager.instance.removePendingShareFrontier(messageID);
                        throw new CommandException("You already have the frontier", new Object[0]);
                    } else {
                        FrontiersManager.instance.addPersonalFrontier(pending.targetUser, frontier);
                    }

                    userShared.setPending(false);
                    frontier.addChange(FrontierData.Change.Shared);

                    FrontiersManager.instance.removePendingShareFrontier(messageID);

                    PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), getCommandSenderAsPlayer(sender));
                    PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier), frontier);

                    // @Note: improve message and localize
                    notifyCommandListener(sender, this, "Accepting frontier " + frontier.getName1() + " " + frontier.getName2());
                } else {
                    throw new CommandException("The invitation is for another player", new Object[0]);
                }
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        try {
            return FrontiersManager.instance.canSendCommandAcceptFrontier(getCommandSenderAsPlayer(sender));
        } catch (PlayerNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

}
