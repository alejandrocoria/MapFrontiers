package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
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
public class GuiUserSharedElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsUserShared user;
    private UserSharedResponder responder;
    private boolean updateFrontier;
    private boolean updateSettings;
    private GuiButtonIcon buttonDelete;
    private int pingBar = 0;
    List<GuiButton> buttonList;

    public GuiUserSharedElement(FontRenderer fontRenderer, List<GuiButton> buttonList, int id, SettingsUserShared user,
            UserSharedResponder responder, ResourceLocation texture, int textureSize) {
        super(430, 16);
        this.fontRenderer = fontRenderer;
        this.user = user;
        this.responder = responder;
        updateFrontier = user.hasAction(SettingsUserShared.Action.UpdateFrontier);
        updateSettings = user.hasAction(SettingsUserShared.Action.UpdateSettings);
        buttonDelete = new GuiButtonIcon(id, 0, 0, 13, 13, 494, 132, -23, texture, textureSize);
        this.buttonList = buttonList;
        this.buttonList.add(buttonDelete);
    }

    @Override
    public void delete() {
        buttonList.remove(buttonDelete);
    }

    public SettingsUser getUser() {
        return user.getUser();
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
        buttonDelete.x = this.x + 413;
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

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
                buttonDelete.visible = true;
            } else {
                buttonDelete.visible = false;
            }

            String text = user.getUser().username;
            if (StringUtils.isBlank(text)) {
                if (user.getUser().uuid == null) {
                    text = I18n.format("mapfrontiers.unnamed", TextFormatting.ITALIC);
                } else {
                    text = user.getUser().uuid.toString();
                }
            }

            fontRenderer.drawString(text, x + 4, y + 4, GuiColors.SETTINGS_TEXT_HIGHLIGHT);

            drawBox(x + 244, y + 2, updateFrontier);
            drawBox(x + 304, y + 2, updateSettings);

            if (user.isPending()) {
                fontRenderer.drawString(I18n.format("mapfrontiers.pending", TextFormatting.ITALIC), x + 350, y + 4,
                        GuiColors.SETTINGS_TEXT_PENDING);
            }

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
        if (visible && hovered && responder != null) {
            if (mouseX >= x + 220 && mouseX <= x + 280) {
                updateFrontier = !updateFrontier;
                responder.actionChanged(user, SettingsUserShared.Action.UpdateFrontier, updateFrontier);
            } else if (mouseX >= x + 280 && mouseX <= x + 340) {
                updateSettings = !updateSettings;
                responder.actionChanged(user, SettingsUserShared.Action.UpdateSettings, updateSettings);
            }
        }

        if (visible && hovered) {
            if (buttonDelete.mousePressed(mc, mouseX, mouseY)) {
                return GuiScrollBox.ScrollElement.Action.Deleted;
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }

    private void drawBox(int x, int y, boolean checked) {
        Gui.drawRect(x, y, x + 12, y + 12, GuiColors.SETTINGS_CHECKBOX_BORDER);
        Gui.drawRect(x + 1, y + 1, x + 11, y + 11, GuiColors.SETTINGS_CHECKBOX_BG);
        if (checked) {
            Gui.drawRect(x + 2, y + 2, x + 10, y + 10, GuiColors.SETTINGS_CHECKBOX_CHECK);
        }
    }

    @SideOnly(Side.CLIENT)
    public interface UserSharedResponder {
        public void actionChanged(SettingsUserShared user, SettingsUserShared.Action action, boolean checked);
    }
}
