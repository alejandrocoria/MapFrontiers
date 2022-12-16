package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierListElement extends GuiScrollBox.ScrollElement {
    private final Font font;
    private final FrontierOverlay frontier;
    String name1;
    String name2;
    String type;
    String owner;
    String dimension;
    String vertices;
    String chunks;
    int offset1;
    int offset2;
    final List<Renderable> buttonList;

    public GuiFrontierListElement(Font font, List<Renderable> buttonList, FrontierOverlay frontier) {
        super(450, 24);
        this.font = font;
        this.frontier = frontier;

        name1 = frontier.getName1();
        name2 = frontier.getName2();
        if (name1.isEmpty() && name2.isEmpty()) {
            name1 = I18n.get("mapfrontiers.unnamed_1", ChatFormatting.ITALIC);
            name2 = I18n.get("mapfrontiers.unnamed_2", ChatFormatting.ITALIC);
        }

        type = I18n.get("mapfrontiers.type", I18n.get(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        owner = I18n.get("mapfrontiers.owner", frontier.getOwner());
        dimension = I18n.get("mapfrontiers.dimension", frontier.getDimension().location().toString());

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            vertices = I18n.get("mapfrontiers.vertices", frontier.getVertexCount());
        } else {
            chunks = I18n.get("mapfrontiers.chunks", frontier.getChunkCount());
        }

        offset1 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Personal")),
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Global")));

        offset2 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.vertices", 9999),
                I18n.get("mapfrontiers.chunks", 9999));

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
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_SELECTED);
        } else if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        int hiddenColor = GuiColors.SETTINGS_TEXT_DARK;

        if (frontier.getVisible()) {
            font.draw(matrixStack, name1, x + 26, y + 4, color);
            font.draw(matrixStack, name2, x + 26, y + 14, color);
        } else {
            font.draw(matrixStack, ChatFormatting.STRIKETHROUGH + name1, x + 26, y + 4, hiddenColor);
            font.draw(matrixStack, ChatFormatting.STRIKETHROUGH + name2, x + 26, y + 14, hiddenColor);
        }

        font.draw(matrixStack, type, x + 170, y + 4, color);
        font.draw(matrixStack, dimension, x + 170, y + 14, GuiColors.SETTINGS_BUTTON_TEXT);

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            font.draw(matrixStack, vertices, x + 180 + offset1, y + 4, color);
        } else {
            font.draw(matrixStack, chunks, x + 180 + offset1, y + 4, color);
        }

        font.draw(matrixStack, owner, x + 190 + offset1 + offset2, y + 4, color);

        fill(matrixStack, x + 1, y + 1, x + 23, y + 23, GuiColors.COLOR_INDICATOR_BORDER);
        fill(matrixStack, x + 2, y + 2, x + 22, y + 22, frontier.getColor() | 0xff000000);
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            return GuiScrollBox.ScrollElement.Action.Clicked;
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }
}
