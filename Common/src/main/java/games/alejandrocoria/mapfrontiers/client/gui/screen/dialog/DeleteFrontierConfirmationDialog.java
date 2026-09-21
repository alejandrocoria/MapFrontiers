package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.component.PluginSourceBadge;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DeleteFrontierConfirmationDialog extends DeleteConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.delete_frontier_dialog";
    private static final String SHARED_DESC_KEY = "mapfrontiers.delete_frontier_dialog_desc_shared";
    private final @Nullable String sourcePluginId;

    public DeleteFrontierConfirmationDialog(FrontierOverlay frontier, @Nullable BooleanConfigEntry askConfirmationEntry, Consumer<Response> callback) {
        super(TITLE_KEY, getDescKey(frontier), askConfirmationEntry, callback);
        sourcePluginId = frontier.getSourcePluginId();
    }

    private static String getDescKey(FrontierOverlay frontier) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        PlayerId playerId = new PlayerId(minecraft.player.getUUID());
        if (frontier.getPersonal() && !frontier.getOwner().equals(playerId)) {
            return SHARED_DESC_KEY;
        }

        return null;
    }

    @Override
    protected @Nullable AbstractWidget createTitleSuffixWidget() {
        if (!SourcePluginUiHelper.hasSourcePlugin(sourcePluginId)) {
            return null;
        }

        return new PluginSourceBadge(font, sourcePluginId, false);
    }
}
