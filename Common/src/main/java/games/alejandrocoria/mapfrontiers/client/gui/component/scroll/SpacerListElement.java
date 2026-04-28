package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpacerListElement extends ScrollBox.ScrollElement {
    private static final int HEIGHT = 5;

    public SpacerListElement(int width) {
        super(width, HEIGHT);
    }
}
