package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GroupElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final SettingsGroup group;
    private IconButton buttonDelete;

    public GroupElement(Font font, SettingsGroup group) {
        super(160, 16);
        this.font = font;
        this.group = group;

        if (!group.isSpecial()) {
            buttonDelete = new IconButton(0, 0, IconButton.Type.Remove, (button) -> {});
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = ColorConstants.TEXT;
        if (selected) {
            color = ColorConstants.TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
            if (buttonDelete != null) {
                buttonDelete.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        String text = group.getName();
        if (text.isEmpty()) {
            text = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
        }

        graphics.drawString(font, text, x + 4, y + 4, color);
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            if (buttonDelete != null && buttonDelete.isMouseOver(mouseX, mouseY)) {
                return ScrollBox.ScrollElement.Action.Deleted;
            } else {
                return ScrollBox.ScrollElement.Action.Clicked;
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }
}
