package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiUserElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer font;
    private final SettingsUser user;
    private final GuiButtonIcon buttonDelete;
    private int pingBar = 0;
    final List<Widget> buttonList;

    public GuiUserElement(FontRenderer font, List<Widget> buttonList, SettingsUser user, ResourceLocation texture,
            int textureSize) {
        super(258, 16);
        this.font = font;
        this.user = user;
        buttonDelete = new GuiButtonIcon(0, 0, 13, 13, 494, 132, -23, texture, textureSize, (button) -> {
        });
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
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
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

        String text = user.username;
        if (StringUtils.isBlank(text)) {
            if (user.uuid == null) {
                text = I18n.get("mapfrontiers.unnamed", TextFormatting.ITALIC);
            } else {
                text = user.uuid.toString();
            }
        }

        font.draw(matrixStack, text, x + 4.f, y + 4.f, color);

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
