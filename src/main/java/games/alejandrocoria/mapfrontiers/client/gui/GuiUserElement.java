package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiUserElement extends GuiScrollBox.ScrollElement {
    private final Font font;
    private final SettingsUser user;
    private final GuiButtonIcon buttonDelete;
    private int pingBar = 0;
    private final Screen screen;

    public GuiUserElement(Font font, Screen screen, SettingsUser user) {
        super(258, 16);
        this.font = font;
        this.user = user;
        this.screen = screen;

        buttonDelete = new GuiButtonIcon(0, 0, GuiButtonIcon.Type.Remove, (button) -> {});
        Screens.getButtons(screen).add(buttonDelete);
    }

    @Override
    public void delete() {
        Screens.getButtons(screen).remove(buttonDelete);
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
        buttonDelete.setX(this.x + 243);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        buttonDelete.setY(this.y + 1);
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = GuiColors.SETTINGS_TEXT;
        if (selected) {
            color = GuiColors.SETTINGS_TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
            buttonDelete.visible = true;
        } else {
            buttonDelete.visible = false;
        }

        font.draw(matrixStack, user.toString(), x + 4.f, y + 4.f, color);

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

    private void drawPingLine(PoseStack matrixStack, int posX, int posY, int height) {
        fill(matrixStack, posX, posY - height, posX + 1, posY, GuiColors.SETTINGS_PING_BAR);
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
