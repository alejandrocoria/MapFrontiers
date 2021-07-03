package games.alejandrocoria.mapfrontiers.client.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiLinkButton extends Widget {
    private final BooleanConsumer callbackFunction;
    private final GuiSimpleLabel label;
    private final String uri;

    public GuiLinkButton(FontRenderer font, int x, int y, ITextComponent text, String uri,
            BooleanConsumer callbackFunction) {
        super(x, y, font.width(text.getString()) + 8, 16, text);
        this.callbackFunction = callbackFunction;
        this.x -= width / 2;
        this.label = new GuiSimpleLabel(font, x, y + 5, GuiSimpleLabel.Align.Center,
                /* TextFormatting.UNDERLINE + */ text, GuiColors.SETTINGS_BUTTON_TEXT);
        this.uri = uri;
    }

    public void openLink() {
        try {
            Desktop.getDesktop().browse(new URI(uri));
        } catch (IOException | URISyntaxException e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(GuiColors.SETTINGS_LINK_HIGHLIGHT);
        } else {
            label.setColor(GuiColors.SETTINGS_LINK);
        }

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Minecraft.getInstance().setScreen(new ConfirmOpenLinkScreen(callbackFunction, uri, false));
    }
}
