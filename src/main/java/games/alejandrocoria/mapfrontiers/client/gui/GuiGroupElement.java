package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.List;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiGroupElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsGroup group;
    private GuiButtonIcon buttonDelete;
    List<GuiButton> buttonList;

    public GuiGroupElement(FontRenderer fontRenderer, List<GuiButton> buttonList, int id, SettingsGroup group,
            ResourceLocation texture, int textureSize) {
        super(160, 16);
        this.fontRenderer = fontRenderer;
        this.group = group;

        buttonDelete = new GuiButtonIcon(id, 0, 0, 13, 13, 494, 132, -23, texture, textureSize);
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
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int color = GuiColors.SETTINGS_TEXT;
            if (selected) {
                color = GuiColors.SETTINGS_TEXT_HIGHLIGHT;
            }

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
            }

            if (hovered && !group.isSpecial()) {
                buttonDelete.visible = true;
            } else {
                buttonDelete.visible = false;
            }

            String text = group.getName();
            if (text.isEmpty()) {
                text = I18n.format("mapfrontiers.unnamed", TextFormatting.ITALIC);
            }

            fontRenderer.drawString(text, x + 4, y + 4, color);
        } else {
            hovered = false;
            buttonDelete.visible = false;
        }
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible && hovered) {
            if (buttonDelete.mousePressed(mc, mouseX, mouseY)) {
                return GuiScrollBox.ScrollElement.Action.Deleted;
            } else {
                return GuiScrollBox.ScrollElement.Action.Clicked;
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }
}
