package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.util.PlayerNameFormatter;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class UserSharedElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final FrontierUserAccess user;
    private final CheckBoxButton updateFrontier;
    private final CheckBoxButton updateSettings;
    private IconButton buttonDelete;
    private final List<GuiEventListener> children;
    private final boolean enabled;
    private int pingBar = 0;

    public UserSharedElement(Font font, FrontierUserAccess user, boolean enabled, boolean removable, ActionChangedConsumer actionChangedCallback) {
        super(430, 15);
        this.font = font;
        this.user = user;
        updateFrontier = new CheckBoxButton(user.hasAction(FrontierUserAccess.Action.UpdateFrontier),
                (b) -> actionChangedCallback.accept(user, FrontierUserAccess.Action.UpdateFrontier, b.isChecked()));
        updateFrontier.active = enabled;
        updateSettings = new CheckBoxButton(user.hasAction(FrontierUserAccess.Action.UpdateSettings),
                (b) -> actionChangedCallback.accept(user, FrontierUserAccess.Action.UpdateSettings, b.isChecked()));
        updateSettings.active = enabled;

        this.enabled = enabled;

        if (removable && enabled) {
            buttonDelete = new IconButton(IconButton.Type.Remove, (button) -> {});
        }

        children = List.of(updateFrontier, updateSettings);
    }

    public PlayerId getUser() {
        return user.getPlayerId();
    }

    @Override
    public Object getFocusRestoreKey() {
        return getUser();
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
    protected void setX(int x) {
        super.setX(x);
        updateFrontier.setX(x + 244);
        updateSettings.setX(x + 304);
        if (buttonDelete != null) {
            buttonDelete.setX(this.x + 413);
        }
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        updateFrontier.setY(y + 2);
        updateSettings.setY(y + 2);
        if (buttonDelete != null) {
            buttonDelete.setY(this.y + 2);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        if (isHovered || focused) {
            if (buttonDelete != null) {
                buttonDelete.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        }

        graphics.text(font, PlayerNameFormatter.getDisplayName(user.getPlayerId()), x + 16, y + 4, ColorConstants.USER_SHARED_TEXT);

        updateFrontier.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        updateSettings.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        if (user.isPending()) {
            graphics.text(font, I18n.get("mapfrontiers.pending", ChatFormatting.ITALIC), x + 350, y + 3, ColorConstants.TEXT_PENDING);
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

    private void drawPingLine(GuiGraphicsExtractor graphics, int posX, int posY, int height) {
        graphics.fill(posX, posY - height, posX + 1, posY, ColorConstants.PING_BAR);
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (enabled && visible && isHovered) {
            for (GuiEventListener checkBox : children) {
                if (checkBox.mouseClicked(event, doubleClick)) {
                    return ScrollBox.ScrollElement.Action.Handled;
                }
            }

            if (buttonDelete != null) {
                if (buttonDelete.isMouseOver(event.x(), event.y())) {
                    return ScrollBox.ScrollElement.Action.Deleted;
                }
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    @Override
    public List<GuiEventListener> children() {
        if (enabled) {
            return children;
        }
        return super.children();
    }

    @Override
    protected boolean canBeDeleted() {
        return true;
    }

    @FunctionalInterface
    public interface ActionChangedConsumer {
        void accept(FrontierUserAccess user, FrontierUserAccess.Action action, boolean checked);
    }
}
