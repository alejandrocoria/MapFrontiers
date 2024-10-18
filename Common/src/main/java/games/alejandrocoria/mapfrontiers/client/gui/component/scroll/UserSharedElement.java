package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class UserSharedElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final SettingsUserShared user;
    private final ActionChangedConsumer actionChangedCallback;
    private boolean updateFrontier;
    private boolean updateSettings;
    private IconButton buttonDelete;
    private final boolean enabled;
    private int pingBar = 0;

    public UserSharedElement(Font font, SettingsUserShared user, boolean enabled, boolean removable, ActionChangedConsumer actionChangedCallback) {
        super(430, 16);
        this.font = font;
        this.user = user;
        this.actionChangedCallback = actionChangedCallback;
        updateFrontier = user.hasAction(SettingsUserShared.Action.UpdateFrontier);
        updateSettings = user.hasAction(SettingsUserShared.Action.UpdateSettings);
        this.enabled = enabled;

        if (removable && enabled) {
            buttonDelete = new IconButton(0, 0, IconButton.Type.Remove, (button) -> {});
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
            buttonDelete.setX(this.x + 413);
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
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
            if (buttonDelete != null) {
                buttonDelete.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        graphics.drawString(font, user.getUser().toString(), x + 16, y + 4, ColorConstants.TEXT_HIGHLIGHT);

        drawBox(graphics, x + 244, y + 2, updateFrontier);
        drawBox(graphics, x + 304, y + 2, updateSettings);

        if (user.isPending()) {
            graphics.drawString(font, I18n.get("mapfrontiers.pending", ChatFormatting.ITALIC), x + 350, y + 4, ColorConstants.TEXT_PENDING);
        }

        if (pingBar > 0) {
            drawPingLine(graphics, x + 3, y + 11, 2);
        }
        if (pingBar > 1) {
            drawPingLine(graphics, x + 5, y + 11, 3);
        }
        if (pingBar > 2) {
            drawPingLine(graphics, x + 7, y + 11, 4);
        }
        if (pingBar > 3) {
            drawPingLine(graphics, x + 9, y + 11, 5);
        }
        if (pingBar > 4) {
            drawPingLine(graphics, x + 11, y + 11, 6);
        }
    }

    private void drawPingLine(GuiGraphics graphics, int posX, int posY, int height) {
        graphics.fill(posX, posY - height, posX + 1, posY, ColorConstants.PING_BAR);
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (enabled && visible && isHovered && actionChangedCallback != null) {
            if (mouseX >= x + 220 && mouseX <= x + 280) {
                updateFrontier = !updateFrontier;
                actionChangedCallback.accept(user, SettingsUserShared.Action.UpdateFrontier, updateFrontier);
            } else if (mouseX >= x + 280 && mouseX <= x + 340) {
                updateSettings = !updateSettings;
                actionChangedCallback.accept(user, SettingsUserShared.Action.UpdateSettings, updateSettings);
            }
        }

        if (enabled && visible && isHovered && buttonDelete != null) {
            if (buttonDelete.isMouseOver(mouseX, mouseY)) {
                return ScrollBox.ScrollElement.Action.Deleted;
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    private void drawBox(GuiGraphics graphics, int x, int y, boolean checked) {
        graphics.fill(x, y, x + 12, y + 12, ColorConstants.CHECKBOX_BORDER);
        graphics.fill(x + 1, y + 1, x + 11, y + 11, ColorConstants.CHECKBOX_BG);
        if (checked) {
            graphics.fill(x + 2, y + 2, x + 10, y + 10, ColorConstants.CHECKBOX_CHECK);
        }
    }

    @FunctionalInterface
    public interface ActionChangedConsumer {
        void accept(SettingsUserShared user, SettingsUserShared.Action action, boolean checked);
    }
}
