package games.alejandrocoria.mapfrontiers.client.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiPatreonButton extends AbstractWidget {
    private final BooleanConsumer callbackFunction;
    private final int texX;
    private final int texY;
    private final ResourceLocation texture;
    private final int textureSize;
    private final String uri;

    public GuiPatreonButton(int x, int y, int width, int height, int texX, int texY, ResourceLocation texture, int textureSize,
            String uri, BooleanConsumer callbackFunction) {
        super(x, y, width, height, TextComponent.EMPTY);
        this.callbackFunction = callbackFunction;
        this.texX = texX;
        this.texY = texY;
        this.texture = texture;
        this.textureSize = textureSize;
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
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && isHovered;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        int factor = (int) mc.getWindow().getGuiScale();

        isHovered = (mouseX >= x - width / 2 / factor && mouseY >= y + 1 / factor && mouseX < x + width / 2 / factor
                && mouseY < y + height / factor);

        if (isHovered) {
            RenderSystem.setShaderColor(0.9f, 0.9f, 0.9f, 1f);
        } else {
            RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        blit(matrixStack, x - width / 2 / factor, y, texX / factor, texY / factor, width / factor, height / factor,
                textureSize / factor, textureSize / factor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Minecraft.getInstance().setScreen(new ConfirmLinkScreen(callbackFunction, uri, false));
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_)
    {

    }
}
