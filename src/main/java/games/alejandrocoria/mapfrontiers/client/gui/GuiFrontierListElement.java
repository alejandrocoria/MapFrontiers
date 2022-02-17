package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierListElement extends GuiScrollBox.ScrollElement {
    private final Font font;
    private final FrontierOverlay frontier;
    final List<Widget> buttonList;

    public GuiFrontierListElement(Font font, List<Widget> buttonList, FrontierOverlay frontier) {
        super(600, 21);
        this.font = font;
        this.frontier = frontier;

        this.buttonList = buttonList;
    }

    @Override
    public void delete() {
    }

    public FrontierOverlay getFrontier() {
        return frontier;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = GuiColors.SETTINGS_TEXT;
        if (selected) {
            color = GuiColors.SETTINGS_TEXT_HIGHLIGHT;
        }

        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        String name1 = frontier.getName1();
        String name2 = frontier.getName2();
        if (name1.isEmpty() && name2.isEmpty()) {
            name1 = I18n.get("mapfrontiers.index_unnamed_1", ChatFormatting.ITALIC);
            name2 = I18n.get("mapfrontiers.index_unnamed_2", ChatFormatting.ITALIC);
        }

        font.draw(matrixStack, name1, x + 4, y + 3, color);
        font.draw(matrixStack, name2, x + 4, y + 12, color);
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            return GuiScrollBox.ScrollElement.Action.Clicked;
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }
}
