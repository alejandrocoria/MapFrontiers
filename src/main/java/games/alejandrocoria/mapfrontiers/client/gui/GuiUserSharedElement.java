package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiUserSharedElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer font;
    private final SettingsUserShared user;
    private final UserSharedResponder responder;
    private boolean updateFrontier;
    private boolean updateSettings;
    private GuiButtonIcon buttonDelete;
    private final boolean enabled;
    private int pingBar = 0;
    List<Widget> buttonList;

    public GuiUserSharedElement(FontRenderer font, List<Widget> buttonList, SettingsUserShared user, boolean enabled,
                                boolean removable, UserSharedResponder responder) {
        super(430, 16);
        this.font = font;
        this.user = user;
        this.responder = responder;
        updateFrontier = user.hasAction(SettingsUserShared.Action.UpdateFrontier);
        updateSettings = user.hasAction(SettingsUserShared.Action.UpdateSettings);
        this.enabled = enabled;

        if (removable && enabled) {
            buttonDelete = new GuiButtonIcon(0, 0, GuiButtonIcon.Type.Remove, (button) -> {});
            this.buttonList = buttonList;
            this.buttonList.add(buttonDelete);
        }
    }

    @Override
    public void delete() {
        if (buttonList != null) {
            buttonList.remove(buttonDelete);
        }
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
        if (buttonDelete != null) {
            buttonDelete.x = this.x + 413;
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        if (buttonDelete != null) {
            buttonDelete.y = this.y + 1;
        }
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        if (buttonDelete != null) {
            buttonDelete.visible = isHovered;
        }

        font.draw(matrixStack, user.getUser().toString(), x + 4.f, y + 4.f, GuiColors.SETTINGS_TEXT_HIGHLIGHT);

        drawBox(matrixStack, x + 244, y + 2, updateFrontier);
        drawBox(matrixStack, x + 304, y + 2, updateSettings);

        if (user.isPending()) {
            font.draw(matrixStack, I18n.get("mapfrontiers.pending", TextFormatting.ITALIC), x + 350.f, y + 4.f,
                    GuiColors.SETTINGS_TEXT_PENDING);
        }

        if (pingBar > 0) {
            drawPingLine(matrixStack, x - 11, y + 11, 2);
        }
        if (pingBar > 1) {
            drawPingLine(matrixStack, x - 9, y + 11, 3);
        }
        if (pingBar > 2) {
            drawPingLine(matrixStack, x - 7, y + 11, 4);
        }
        if (pingBar > 3) {
            drawPingLine(matrixStack, x - 5, y + 11, 5);
        }
        if (pingBar > 4) {
            drawPingLine(matrixStack, x - 3, y + 11, 6);
        }
    }

    private void drawPingLine(MatrixStack matrixStack, int posX, int posY, int height) {
        fill(matrixStack, posX, posY - height, posX + 1, posY, GuiColors.SETTINGS_PING_BAR);
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (enabled && visible && isHovered && responder != null) {
            if (mouseX >= x + 220 && mouseX <= x + 280) {
                updateFrontier = !updateFrontier;
                responder.actionChanged(user, SettingsUserShared.Action.UpdateFrontier, updateFrontier);
            } else if (mouseX >= x + 280 && mouseX <= x + 340) {
                updateSettings = !updateSettings;
                responder.actionChanged(user, SettingsUserShared.Action.UpdateSettings, updateSettings);
            }
        }

        if (enabled && visible && isHovered && buttonDelete != null) {
            if (buttonDelete.isMouseOver(mouseX, mouseY)) {
                return GuiScrollBox.ScrollElement.Action.Deleted;
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }

    private void drawBox(MatrixStack matrixStack, int x, int y, boolean checked) {
        fill(matrixStack, x, y, x + 12, y + 12, GuiColors.SETTINGS_CHECKBOX_BORDER);
        fill(matrixStack, x + 1, y + 1, x + 11, y + 11, GuiColors.SETTINGS_CHECKBOX_BG);
        if (checked) {
            fill(matrixStack, x + 2, y + 2, x + 10, y + 10, GuiColors.SETTINGS_CHECKBOX_CHECK);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface UserSharedResponder {
        void actionChanged(SettingsUserShared user, SettingsUserShared.Action action, boolean checked);
    }
}
