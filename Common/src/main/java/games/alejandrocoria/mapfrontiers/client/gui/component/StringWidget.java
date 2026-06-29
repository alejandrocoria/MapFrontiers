package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
        setColor(ColorConstants.STRING_WIDGET_TEXT_DEFAULT);
        this.align = align;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public StringWidget setColor(int color) {
        super.setColor(color);
        return this;
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        setWidth(getFont().width(message.getVisualOrderText()));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (scale != 1.f) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(scale, scale, 1.0f);
        }

        int x = Mth.floor(this.getX() / scale);
        int y = Mth.floor((this.getY() + (this.getHeight() - 8) / 2.f) / scale);

        if (align == Align.Left) {
            guiGraphics.drawString(getFont(), this.getMessage(), x, y, this.getColor());
        } else if (align == Align.Center) {
            guiGraphics.drawString(getFont(), this.getMessage(), x - getFont().width(getMessage()) / 2, y, this.getColor());
        } else {
            guiGraphics.drawString(getFont(), this.getMessage(), x - getFont().width(getMessage()), y, this.getColor());
        }

        if (scale != 1.f) {
            guiGraphics.pose().popPose();
        }
    }
}
