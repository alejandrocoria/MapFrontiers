package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.URI;
import java.net.URISyntaxException;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiLinkButton extends AbstractWidget {
    private final BooleanConsumer callbackFunction;
    private final GuiSimpleLabel label;
    private final String uri;

    public GuiLinkButton(Font font, int x, int y, Component text, String uri, BooleanConsumer callbackFunction) {
        super(x, y, font.width(text.getString()) + 8, 16, text);
        this.callbackFunction = callbackFunction;
        this.setX(x - width / 2);
        this.label = new GuiSimpleLabel(font, x, y + 5, GuiSimpleLabel.Align.Center, text, GuiColors.SETTINGS_BUTTON_TEXT);
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
            label.setColor(GuiColors.SETTINGS_LINK_HIGHLIGHT);
        } else {
            label.setColor(GuiColors.SETTINGS_LINK);
        }

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new ConfirmLinkScreen(callbackFunction, uri, false));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput)
    {

    }
}
