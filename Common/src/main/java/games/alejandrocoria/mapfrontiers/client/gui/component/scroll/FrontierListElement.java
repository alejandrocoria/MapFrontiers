package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FrontierListElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final FrontierOverlay frontier;
    private final String name1;
    private final String name2;
    private final String type;
    private final String owner;
    private final String dimension;
    private final String vertices;
    private final String chunks;
    private final int offset1;
    private final int offset2;

    public FrontierListElement(Font font, FrontierOverlay frontier) {
        super(450, 24);
        this.font = font;
        this.frontier = frontier;

        if (frontier.isNamed()) {
            name1 = frontier.getName1();
            name2 = frontier.getName2();
        } else {
            name1 = I18n.get("mapfrontiers.unnamed_1", ChatFormatting.ITALIC);
            name2 = I18n.get("mapfrontiers.unnamed_2", ChatFormatting.ITALIC);
        }

        type = I18n.get("mapfrontiers.type", I18n.get(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        owner = I18n.get("mapfrontiers.owner", frontier.getOwner());
        dimension = I18n.get("mapfrontiers.dimension", frontier.getDimension().location().toString());

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            vertices = I18n.get("mapfrontiers.vertices", frontier.getVertexCount());
            chunks = null;
        } else {
            vertices = null;
            chunks = I18n.get("mapfrontiers.chunks", frontier.getChunkCount());
        }

        offset1 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Personal")),
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Global")));

        offset2 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.vertices", 9999),
                I18n.get("mapfrontiers.chunks", 9999));
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected) {
        int color = ColorConstants.TEXT;
        if (selected) {
            color = ColorConstants.TEXT_HIGHLIGHT;
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_SELECTED);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        int hiddenColor = ColorConstants.TEXT_DARK;

        if (frontier.getVisible()) {
            graphics.drawString(font, name1, x + 26, y + 4, color);
            graphics.drawString(font, name2, x + 26, y + 14, color);
        } else {
            graphics.drawString(font, ChatFormatting.STRIKETHROUGH + name1, x + 26, y + 4, hiddenColor);
            graphics.drawString(font, ChatFormatting.STRIKETHROUGH + name2, x + 26, y + 14, hiddenColor);
        }

        graphics.drawString(font, type, x + 170, y + 4, color);
        graphics.drawString(font, dimension, x + 170, y + 14, ColorConstants.TEXT_DIMENSION);

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            graphics.drawString(font, vertices, x + 180 + offset1, y + 4, color);
        } else {
            graphics.drawString(font, chunks, x + 180 + offset1, y + 4, color);
        }

        graphics.drawString(font, owner, x + 190 + offset1 + offset2, y + 4, color);

        graphics.fill(x + 1, y + 1, x + 23, y + 23, ColorConstants.COLOR_INDICATOR_BORDER);
        graphics.fill(x + 2, y + 2, x + 22, y + 22, frontier.getColor() | 0xff000000);
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            return ScrollBox.ScrollElement.Action.Clicked;
        }

        return ScrollBox.ScrollElement.Action.None;
    }
}
