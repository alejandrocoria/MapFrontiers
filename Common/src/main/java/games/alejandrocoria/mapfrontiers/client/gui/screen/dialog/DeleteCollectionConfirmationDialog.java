package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.component.PluginSourceBadge;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import net.minecraft.client.gui.components.AbstractWidget;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DeleteCollectionConfirmationDialog extends DeleteConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.delete_collection_dialog";
    private static final String TEXT_KEY = "mapfrontiers.delete_collection_dialog_desc";
    private final @Nullable String sourcePluginId;

    public DeleteCollectionConfirmationDialog(CollectionData collection, Consumer<Response> callback) {
        super(TITLE_KEY, TEXT_KEY, callback);
        sourcePluginId = collection.getSourcePluginId();
    }

    @Override
    protected @Nullable AbstractWidget createTitleSuffixWidget() {
        if (!SourcePluginUiHelper.hasSourcePlugin(sourcePluginId)) {
            return null;
        }

        return new PluginSourceBadge(font, sourcePluginId, false);
    }
}
