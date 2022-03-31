package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierBook extends Screen {
    private GuiSettingsButton buttonDone;

    public GuiFrontierBook() {
        super(TextComponent.EMPTY);
    }

    @Override
    public void init() {
        addRenderableOnly(new GuiSimpleLabel(font, width / 2, height / 2 - 8, GuiSimpleLabel.Align.Center,
                new TranslatableComponent("mapfrontiers.book_message"), GuiColors.WHITE));

        buttonDone = new GuiSettingsButton(font, width / 2 - 70, height / 2 + 8, 140,
                new TranslatableComponent("gui.done"), this::buttonPressed);

        addRenderableWidget(buttonDone);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }
}
