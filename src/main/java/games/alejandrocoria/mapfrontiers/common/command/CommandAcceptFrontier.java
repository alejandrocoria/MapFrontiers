package games.alejandrocoria.mapfrontiers.common.command;

import java.util.List;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.PendingShareFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommandAcceptFrontier extends CommandBase {
    @Override
    public String getName() {
        return "acceptfrontier";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/acceptfrontier <invitation id>";
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
                        throw new CommandException("The frontier no longer exists", new Object[0]);
                    }

                    // @Incomplete
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
