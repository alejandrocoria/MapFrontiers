package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;

import java.util.function.Consumer;

public class DeleteCollectionConfirmationDialog extends ConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.delete_collection_dialog";
    private static final String TEXT_KEY = "mapfrontiers.delete_collection_dialog_desc";
    private static final String CONFIRM_KEY = "mapfrontiers.delete";
    private static final String CANCEL_KEY = "gui.cancel";
    private static final String CONFIRM_DONT_ASK_KEY = "mapfrontiers.delete_collection_dont_ask";

    public DeleteCollectionConfirmationDialog(Consumer<Response> callback) {
        super(TITLE_KEY, TEXT_KEY, CONFIRM_KEY, CANCEL_KEY, CONFIRM_DONT_ASK_KEY, callback);
    }

    @Override
    protected void initScreen() {
        super.initScreen();
        confirmButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        if (confirmAlternativeButton != null) {
            confirmAlternativeButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        }
    }
}
