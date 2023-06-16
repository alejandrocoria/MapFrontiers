package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.platform.Services;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.URI;
import java.net.URISyntaxException;

@ParametersAreNonnullByDefault
public class LinkButton extends AbstractWidgetNoNarration {
    private final BooleanConsumer callbackFunction;
    private final SimpleLabel label;
    private final String uri;

    public LinkButton(Font font, int x, int y, Component text, String uri, BooleanConsumer callbackFunction) {
        super(x, y, font.width(text.getString()) + 8, 16, text);
        this.callbackFunction = callbackFunction;
        setX(getX() - width / 2);
        this.label = new SimpleLabel(font, x, y + 5, SimpleLabel.Align.Center, text, ColorConstants.SIMPLE_BUTTON_TEXT);
        this.uri = uri;
    }

    public void openLink() {
        try {
            Util.getPlatform().openUri(new URI(uri));
        } catch (UnsupportedOperationException | URISyntaxException e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(ColorConstants.LINK_HIGHLIGHT);
        } else {
            label.setColor(ColorConstants.LINK);
        }

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Services.PLATFORM.pushGuiLayer(new ConfirmLinkScreen(callbackFunction, uri, false));
    }
}
