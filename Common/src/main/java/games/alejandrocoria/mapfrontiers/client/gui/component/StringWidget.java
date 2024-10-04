package games.alejandrocoria.mapfrontiers.client.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class StringWidget extends net.minecraft.client.gui.components.StringWidget {
    public StringWidget(Component message, Font font) {
        super(0, 0, font.width(message.getVisualOrderText()), 12, message, font);
    }

    @Override
    public @NotNull StringWidget setColor(int color) {
        super.setColor(color);
        return this;
    }
}
