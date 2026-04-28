package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import java.util.function.Consumer;

public class DeleteCollectionConfirmationDialog extends DeleteConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.delete_collection_dialog";
    private static final String TEXT_KEY = "mapfrontiers.delete_collection_dialog_desc";
    private static final String CONFIRM_DONT_ASK_KEY = "mapfrontiers.delete_collection_dont_ask";

    public DeleteCollectionConfirmationDialog(Consumer<Response> callback) {
        super(TITLE_KEY, TEXT_KEY, CONFIRM_DONT_ASK_KEY, callback);
    }
}
