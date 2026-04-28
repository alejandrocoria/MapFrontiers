package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class DeleteFrontierConfirmationDialog extends DeleteConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.delete_frontier_dialog";
    private static final String SHARED_DESC_KEY = "mapfrontiers.delete_frontier_dialog_desc_shared";

    public DeleteFrontierConfirmationDialog(FrontierOverlay frontier, Consumer<Response> callback) {
        super(TITLE_KEY, getDescKey(frontier), callback);
    }

    private static String getDescKey(FrontierOverlay frontier) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        SettingsUser playerUser = new SettingsUser(minecraft.player);
        if (frontier.getPersonal() && !frontier.getOwner().equals(playerUser)) {
            return SHARED_DESC_KEY;
        }

        return null;
    }
}
