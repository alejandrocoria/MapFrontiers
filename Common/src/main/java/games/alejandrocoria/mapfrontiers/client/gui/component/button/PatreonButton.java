package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.platform.Services;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.URI;
import java.net.URISyntaxException;

@ParametersAreNonnullByDefault
public class PatreonButton extends AbstractWidgetNoNarration {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/patreon.png");
    private static final int textureSizeX = 212;
    private static final int textureSizeY = 50;

    private final BooleanConsumer callbackFunction;
    private final String uri;

    public PatreonButton(int x, int y, String uri, BooleanConsumer callbackFunction) {
        super(x, y, textureSizeX, textureSizeY, Component.empty());
        this.callbackFunction = callbackFunction;
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
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && isHovered;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        int factor = (int) mc.getWindow().getGuiScale();

        isHovered = (mouseX >= getX() - width / 2 / factor && mouseY >= getY() + 1 / factor && mouseX < getX() + width / 2 / factor
                && mouseY < getY() + height / factor);

        if (isHovered) {
            RenderSystem.setShaderColor(0.9f, 0.9f, 0.9f, 1.f);
        } else {
            RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        }

        graphics.blit(texture, getX() - width / 2 / factor, getY(), 0, 0, width / factor, height / factor,
                textureSizeX / factor, textureSizeY / factor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Services.PLATFORM.pushGuiLayer(new ConfirmLinkScreen(callbackFunction, uri, false));
    }
}
