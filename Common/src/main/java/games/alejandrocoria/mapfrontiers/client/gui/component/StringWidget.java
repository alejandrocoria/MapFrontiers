package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StringWidget extends net.minecraft.client.gui.components.StringWidget {
    public enum Align {
        Left, Center, Right
    }

    private float scale = 1.f;
    private final Align align;
    private int color = ColorConstants.STRING_WIDGET_TEXT_DEFAULT;

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

    public StringWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (scale != 1.f) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale, scale);
        }

        int x = Mth.floor(this.getX() / scale);
        int y = Mth.floor((this.getY() + (this.getHeight() - 8) / 2.f) / scale);

        if (align == Align.Left) {
            guiGraphics.text(getFont(), this.getMessage(), x, y, this.getColor());
        } else if (align == Align.Center) {
            guiGraphics.text(getFont(), this.getMessage(), x - getFont().width(getMessage()) / 2, y, this.getColor());
        } else {
            guiGraphics.text(getFont(), this.getMessage(), x - getFont().width(getMessage()), y, this.getColor());
        }

        if (scale != 1.f) {
            guiGraphics.pose().popMatrix();
        }
    }
}
