package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.URI;
import java.net.URISyntaxException;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiPatreonButton extends Widget {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/patreon.png");
    private static final int textureSizeX = 212;
    private static final int textureSizeY = 50;

    private final BooleanConsumer callbackFunction;
    private final String uri;

    public GuiPatreonButton(int x, int y, String uri, BooleanConsumer callbackFunction) {
        super(x, y, textureSizeX, textureSizeY, StringTextComponent.EMPTY);
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
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        int factor = (int) mc.getWindow().getGuiScale();

        isHovered = (mouseX >= x - width / 2 / factor && mouseY >= y + 1 / factor && mouseX < x + width / 2 / factor
                && mouseY < y + height / factor);

        if (isHovered) {
            RenderSystem.color4f(0.9f, 0.9f, 0.9f, 1.f);
        } else {
            RenderSystem.color4f(1.f, 1.f, 1.f, 1.f);
        }

        Minecraft.getInstance().getTextureManager().bind(texture);
        blit(matrixStack, x - width / 2 / factor, y, 0, 0, width / factor, height / factor,
                textureSizeX / factor, textureSizeY / factor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new ConfirmOpenLinkScreen(callbackFunction, uri, false));
    }
}
