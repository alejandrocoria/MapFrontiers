package games.alejandrocoria.mapfrontiers.client.gui.component;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@MethodsReturnNonnullByDefault
public class StringWidget extends net.minecraft.client.gui.components.StringWidget {
    public StringWidget(Component message, Font font) {
        this(message, font, 12);
    }
    public StringWidget(Component message, Font font, int height) {
        super(0, 0, font.width(message.getVisualOrderText()), height, message, font);
    }

    @Override
    public @NotNull StringWidget setColor(int color) {
        super.setColor(color);
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int y = this.getY() + (this.getHeight() - 10) / 2;
        guiGraphics.drawString(getFont(), this.getMessage(), this.getX(), y, this.getColor());
    }
}
