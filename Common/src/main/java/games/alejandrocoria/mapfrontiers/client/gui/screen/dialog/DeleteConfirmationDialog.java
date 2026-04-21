package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;

import java.util.function.Consumer;

public class DeleteConfirmationDialog extends ConfirmationDialog {
    private static final String CONFIRM_KEY = "mapfrontiers.delete";
    private static final String CANCEL_KEY = "gui.cancel";
    private static final String CONFIRM_DONT_ASK_KEY = "mapfrontiers.delete_frontier_dont_ask";

    public DeleteConfirmationDialog(String titleKey, Consumer<Response> callback) {
        super(titleKey, null, CONFIRM_KEY, CANCEL_KEY, CONFIRM_DONT_ASK_KEY, callback);
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
