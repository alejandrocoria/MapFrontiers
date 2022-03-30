package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierBook extends Screen {
    private GuiSettingsButton buttonDone;

    public GuiFrontierBook() {
        super(StringTextComponent.EMPTY);
    }

    @Override
    public void init() {
        buttons.add(new GuiSimpleLabel(font, width / 2, height / 2 - 8, GuiSimpleLabel.Align.Center,
                new TranslationTextComponent("mapfrontiers.book_message"), GuiColors.WHITE));

        buttonDone = new GuiSettingsButton(font, width / 2 - 70, height / 2 + 8, 140,
                new TranslationTextComponent("gui.done"), this::buttonPressed);

        addButton(buttonDone);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }
}
