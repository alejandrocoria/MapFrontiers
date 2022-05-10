package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiGroupElement extends GuiScrollBox.ScrollElement {
    private final Font font;
    private final SettingsGroup group;
    private final GuiButtonIcon buttonDelete;
    final List<Widget> buttonList;

    public GuiGroupElement(Font font, List<Widget> buttonList, SettingsGroup group) {
        super(160, 16);
        this.font = font;
        this.group = group;

        buttonDelete = new GuiButtonIcon(0, 0, GuiButtonIcon.Type.Remove, (button) -> {});
        buttonDelete.visible = false;

        this.buttonList = buttonList;
        this.buttonList.add(buttonDelete);
    }

    @Override
    public void delete() {
        buttonList.remove(buttonDelete);
    }

    public SettingsGroup getGroup() {
        return group;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        buttonDelete.x = this.x + 145;
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        buttonDelete.y = this.y + 1;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = GuiColors.SETTINGS_TEXT;
        if (selected) {
            color = GuiColors.SETTINGS_TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        buttonDelete.visible = isHovered && !group.isSpecial();

        String text = group.getName();
        if (text.isEmpty()) {
            text = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
        }

        font.draw(matrixStack, text, x + 4, y + 4, color);
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            if (buttonDelete.isMouseOver(mouseX, mouseY)) {
                return GuiScrollBox.ScrollElement.Action.Deleted;
            } else {
                return GuiScrollBox.ScrollElement.Action.Clicked;
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }
}
