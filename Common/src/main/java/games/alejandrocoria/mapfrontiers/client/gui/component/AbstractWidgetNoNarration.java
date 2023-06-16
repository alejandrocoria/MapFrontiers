package games.alejandrocoria.mapfrontiers.client.gui.component;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class AbstractWidgetNoNarration extends AbstractWidget {
    public AbstractWidgetNoNarration(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
