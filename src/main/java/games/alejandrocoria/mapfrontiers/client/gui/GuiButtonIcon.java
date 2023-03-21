package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiButtonIcon extends Button {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/buttons.png");
    private static final int textureSizeX = 26;
    private static final int textureSizeY = 26;

    public enum Type {
        Add, Remove
    }

    private int texY;

    public GuiButtonIcon(int x, int y, Type type, Button.OnPress pressedAction) {
        super(x, y, 13, 13, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);

        switch (type) {
            case Add:
                texY = 0;
                break;
            case Remove:
                texY = 13;
                break;
        }
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        int texX = 0;
        if (isHovered) {
            texX = 13;
        }

        blit(matrixStack, getX(), getY(), texX, texY, width, height, textureSizeX, textureSizeY);
    }
}
