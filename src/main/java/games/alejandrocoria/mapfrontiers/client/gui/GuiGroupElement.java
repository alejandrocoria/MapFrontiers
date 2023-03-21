package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiGroupElement extends GuiScrollBox.ScrollElement {
    private final Font font;
    private final SettingsGroup group;
    private GuiButtonIcon buttonDelete;
    private final Screen screen;

    public GuiGroupElement(Font font, Screen screen, SettingsGroup group) {
        super(160, 16);
        this.font = font;
        this.group = group;
        this.screen = screen;

        if (!group.isSpecial()) {
            buttonDelete = new GuiButtonIcon(0, 0, GuiButtonIcon.Type.Remove, (button) -> {});
            buttonDelete.visible = false;
            Screens.getButtons(screen).add(buttonDelete);
        }
    }

    @Override
    public void delete() {
        if (buttonDelete != null) {
            Screens.getButtons(screen).remove(buttonDelete);
        }
    }

    public SettingsGroup getGroup() {
        return group;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        if (buttonDelete != null) {
            buttonDelete.setX(this.x + 145);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        if (buttonDelete != null) {
            buttonDelete.setY(this.y + 1);
        }
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = GuiColors.SETTINGS_TEXT;
        if (selected) {
            color = GuiColors.SETTINGS_TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        if (buttonDelete != null) {
            buttonDelete.visible = isHovered;
        }

        String text = group.getName();
        if (text.isEmpty()) {
            text = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
        }

        font.draw(matrixStack, text, x + 4, y + 4, color);
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            if (buttonDelete != null && buttonDelete.isMouseOver(mouseX, mouseY)) {
                return GuiScrollBox.ScrollElement.Action.Deleted;
            } else {
                return GuiScrollBox.ScrollElement.Action.Clicked;
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }
}
