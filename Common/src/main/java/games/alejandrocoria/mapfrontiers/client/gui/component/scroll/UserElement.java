package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UserElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final SettingsUser user;
    private final IconButton buttonDelete;
    private int pingBar = 0;

    public UserElement(Font font, SettingsUser user) {
        super(258, 15);
        this.font = font;
        this.user = user;

        buttonDelete = new IconButton(IconButton.Type.Remove, (button) -> {});
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
    protected void setX(int x) {
        super.setX(x);
        buttonDelete.setX(this.x + 243);
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        buttonDelete.setY(this.y + 2);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        int color = ColorConstants.TEXT;
        if (selected) {
            color = ColorConstants.TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        if (isHovered || focused) {
            buttonDelete.render(graphics, mouseX, mouseY, partialTicks);
        }

        graphics.drawString(font, SettingsUserFormatter.getDisplayName(user), x + 16, y + 3, color);

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
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            if (buttonDelete.isMouseOver(event.x(), event.y())) {
                return ScrollBox.ScrollElement.Action.Deleted;
            } else {
                return ScrollBox.ScrollElement.Action.Clicked;
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    @Override
    protected boolean canBeDeleted() {
        return true;
    }
}
