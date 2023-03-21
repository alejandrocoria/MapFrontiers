package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiOptionButton extends Button {
    protected final Font font;
    private final List<Component> options;
    private int selected = 0;
    private int color = GuiColors.SETTINGS_TEXT;
    private int highlightedColor = GuiColors.SETTINGS_TEXT_HIGHLIGHT;

    public GuiOptionButton(Font font, int x, int y, int width, Button.OnPress pressedAction) {
        super(x, y, width, 12, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
        this.font = font;
        options = new ArrayList<>();
    }

    public void addOption(Component text) {
        options.add(text);
    }

    public void setSelected(int selected) {
        if (selected < 0) {
            selected = 0;
        } else if (selected >= options.size()) {
            selected = options.size() - 1;
        }

        this.selected = selected;
    }

    public int getSelected() {
        return selected;
    }

    public void setColor(int color, int highlightedColor) {
        this.color = color;
        this.highlightedColor = highlightedColor;
    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible && isHovered) {
            if (delta > 0) {
                ++selected;
                if (selected >= options.size()) {
                    selected = 0;
                }
            } else {
                --selected;
                if (selected < 0) {
                    selected = options.size() - 1;
                }
            }

            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress();
            return true;
        }

        return false;
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int c = color;
        if (!active) {
            c = GuiColors.SETTINGS_TEXT_DARK;
        } else if (isHovered) {
            c = highlightedColor;
        }

        fill(matrixStack, getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, GuiColors.SETTINGS_OPTION_BORDER);
        fill(matrixStack, getX(), getY(), getX() + width, getY() + height, GuiColors.SETTINGS_OPTION_BG);

        font.draw(matrixStack, options.get(selected), getX() + 4, getY() + 2, c);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ++selected;
        if (selected >= options.size()) {
            selected = 0;
        }

        onPress();
    }
}
