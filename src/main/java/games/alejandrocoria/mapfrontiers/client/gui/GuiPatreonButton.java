package games.alejandrocoria.mapfrontiers.client.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiPatreonButton extends GuiButton {
    private GuiYesNoCallback screen;
    private final int texX;
    private final int texY;
    private final ResourceLocation texture;
    private final int textureSize;
    private String uri;

    public GuiPatreonButton(GuiYesNoCallback screen, int id, int x, int y, int width, int height, int texX, int texY,
            ResourceLocation texture, int textureSize, String uri) {
        super(id, x, y, width, height, "");
        this.screen = screen;
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
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            ScaledResolution scaledresolution = new ScaledResolution(mc);
            int factor = scaledresolution.getScaleFactor();

            hovered = (mouseX >= x - width / 2 / factor && mouseY >= y + 1 / factor && mouseX < x + width / 2 / factor
                    && mouseY < y + height / factor);

            if (hovered) {
                GlStateManager.color(.9f, .9f, .9f);
            } else {
                GlStateManager.color(1.f, 1.f, 1.f);
            }

            mc.getTextureManager().bindTexture(texture);

            drawModalRectWithCustomSizedTexture(x - width / 2 / factor, y, texX / factor, texY / factor, width / factor,
                    height / factor, textureSize / factor, textureSize / factor);
        } else {
            hovered = false;
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (enabled && visible && hovered) {
            mc.displayGuiScreen(new GuiConfirmOpenLink(screen, uri, id, false));
            return true;
        }

        return false;
    }
}