package games.alejandrocoria.mapfrontiers.client.gui.component;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StringWidget extends net.minecraft.client.gui.components.StringWidget {
    public enum Align {
        Left, Center, Right
    }

    private float scale = 1.f;
    private final Align align;

    public StringWidget(Component message, Font font) {
        this(message, font, 12, Align.Left);
    }

    public StringWidget(Component message, Font font, Align align) {
        this(message, font, 12, align);
    }

    public StringWidget(Component message, Font font, int height) {
        this(message, font, height, Align.Left);
    }

    public StringWidget(Component message, Font font, int height, Align align) {
        super(0, 0, font.width(message.getVisualOrderText()), height, message, font);
        this.align = align;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public @NotNull StringWidget setColor(int color) {
        super.setColor(color);
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (scale != 1.f) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale, scale);
        }

        int x = Mth.floor(this.getX() / scale);
        int y = Mth.floor((this.getY() + (this.getHeight() - 10) / 2.f) / scale);

        if (align == Align.Left) {
            guiGraphics.drawString(getFont(), this.getMessage(), x, y, this.getColor());
        } else if (align == Align.Center) {
            guiGraphics.drawString(getFont(), this.getMessage(), x - getFont().width(getMessage()) / 2, y, this.getColor());
        } else {
            guiGraphics.drawString(getFont(), this.getMessage(), x - getFont().width(getMessage()), y, this.getColor());
        }

        if (scale != 1.f) {
            guiGraphics.pose().popMatrix();
        }
    }
}
