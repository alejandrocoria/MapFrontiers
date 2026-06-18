package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SpacerListElement extends ScrollBox.ScrollElement {
    public SpacerListElement(int width, int height) {
        super(width, height);
    }

    @Override
    protected boolean isKeyboardFocusable() {
        return false;
    }
}
