package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

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
        super(x, y, width, 12, TextComponent.EMPTY, pressedAction);
        this.font = font;
        options = new ArrayList<>();
    }

    public void addOption(Component text) {
        options.add(text);
    }

    public void addOption(String text) {
        options.add(new TextComponent(text));
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

    public void setColor(int color) {
        this.color = color;
        highlightedColor = color;
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
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int c = color;
        if (!active) {
            c = GuiColors.SETTINGS_TEXT_DARK;
        } else if (isHovered) {
            c = highlightedColor;
        }

        fill(matrixStack, x - 1, y - 1, x + width + 1, y + height + 1, GuiColors.SETTINGS_OPTION_BORDER);
        fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_OPTION_BG);

        font.draw(matrixStack, options.get(selected), x + 4, y + 2, c);
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
