package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.List;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiUserElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsUser user;
    private GuiButtonIcon buttonDelete;
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

            int color = 0xbbbbbb;
            if (selected) {
                color = 0xffffff;
            }

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, 0xff222222);
                buttonDelete.visible = true;
            } else {
                buttonDelete.visible = false;
            }

            String text = user.username;
            if (text.isEmpty()) {
                if (user.uuid == null) {
                    text = String.format("%1$sUnnamed", TextFormatting.ITALIC);
                } else {
                    text = user.uuid.toString();
                }
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
