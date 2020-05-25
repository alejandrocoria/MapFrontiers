package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
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
public class GuiUserElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsUser user;
    private GuiButtonIcon buttonDelete;
    private int pingBar = 0;
    List<GuiButton> buttonList;

    public GuiUserElement(FontRenderer fontRenderer, List<GuiButton> buttonList, int id, SettingsUser user,
            ResourceLocation texture, int textureSize) {
        super(258, 16);
        this.fontRenderer = fontRenderer;
        this.user = user;
        buttonDelete = new GuiButtonIcon(id, 0, 0, 13, 13, 494, 132, -23, texture, textureSize);
        this.buttonList = buttonList;
        this.buttonList.add(buttonDelete);
    }

    @Override
    public void delete() {
        buttonList.remove(buttonDelete);
    }

    public SettingsUser getUser() {
        return user;
    }

    public void setPingBar(int value) {
        pingBar = value;

        if (pingBar < 0) {
            pingBar = 0;
        } else if (pingBar > 5) {
            pingBar = 5;
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        buttonDelete.x = this.x + 243;
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
                buttonDelete.visible = true;
            } else {
                buttonDelete.visible = false;
            }

            String text = user.username;
            if (StringUtils.isBlank(text)) {
                if (user.uuid == null) {
                    text = I18n.format("mapfrontiers.unnamed", TextFormatting.ITALIC);
                } else {
                    text = user.uuid.toString();
                }
            }

            fontRenderer.drawString(text, x + 4, y + 4, color);

            if (pingBar > 0) {
                drawPingLine(x - 11, y + 11, 2);
            }
            if (pingBar > 1) {
                drawPingLine(x - 9, y + 11, 3);
            }
            if (pingBar > 2) {
                drawPingLine(x - 7, y + 11, 4);
            }
            if (pingBar > 3) {
                drawPingLine(x - 5, y + 11, 5);
            }
            if (pingBar > 4) {
                drawPingLine(x - 3, y + 11, 6);
            }
        } else {
            hovered = false;
            buttonDelete.visible = false;
        }
    }

    private void drawPingLine(int posX, int posY, int height) {
        Gui.drawRect(posX, posY - height, posX + 1, posY, GuiColors.SETTINGS_PING_BAR);
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
