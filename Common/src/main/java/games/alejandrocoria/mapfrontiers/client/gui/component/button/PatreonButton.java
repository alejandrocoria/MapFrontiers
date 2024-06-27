package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PatreonButton extends Button {
    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/patreon.png");
    private static final int textureSizeX = 212;
    private static final int textureSizeY = 50;

    public PatreonButton(int x, int y, OnPress pressedAction) {
        super(x, y, textureSizeX, textureSizeY, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
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
}
