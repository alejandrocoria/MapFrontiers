package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FrontierListElement extends ScrollBox.ScrollElement {
    private static final Identifier NAME_FADE_TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_list/name_fade.png");
    private static final Identifier CHUNK_FILL_TEXTURE = frontierListTexture("chunk_fill.png");
    private static final Identifier CHUNK_OUTLINE_TEXTURE = frontierListTexture("chunk_outline.png");
    private static final Identifier PATH_FILL_TEXTURE = frontierListTexture("path_fill.png");
    private static final Identifier PATH_OUTLINE_TEXTURE = frontierListTexture("path_outline.png");
    private static final Identifier VERTEX_FILL_TEXTURE = frontierListTexture("vertex_fill.png");
    private static final Identifier VERTEX_OUTLINE_TEXTURE = frontierListTexture("vertex_outline.png");
    private static final int NAME_HOVER_X = 24;
    private static final int NAME_X = 26;
    private static final int METADATA_X = 170;
    private static final int OWNER_X_OFFSET = 12;
    private static final int NAME_METADATA_SPACING = 2;
    private static final int MODE_BADGE_X = 2;
    private static final int MODE_BADGE_ICON_Y = 1;
    private static final int MODE_BADGE_COUNT_Y = 16;
    private static final int MODE_BADGE_ICON_WIDTH = 21;
    private static final int MODE_BADGE_ICON_HEIGHT = 14;
    private static final int NAME_LINE_1_Y = 4;
    private static final int NAME_LINE_2_Y = 14;
    private static final int NAME_LINE_BG_TOP_OFFSET = -1;
    private static final int NAME_LINE_BG_BOTTOM_OFFSET = 9;
    private static final int NAME_LINE_BG_FADE_WIDTH = 6;
    private static final String ELLIPSIS = "...";
    private final Font font;
    private final FrontierOverlay frontier;
    private final String name1;
    private final String name2;
    private final String type;
    private final String owner;
    private final String dimension;
    private final int count;
    private final int offset1;

    public FrontierListElement(Font font, FrontierOverlay frontier) {
        super(450, 25);
        this.font = font;
        this.frontier = frontier;

        if (frontier.isNamed()) {
            name1 = frontier.getName1();
            name2 = frontier.getName2();
        } else {
            name1 = I18n.get("mapfrontiers.unnamed_1", ChatFormatting.ITALIC);
            name2 = I18n.get("mapfrontiers.unnamed_2", ChatFormatting.ITALIC);
        }

        type = I18n.get(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global");
        owner = I18n.get("mapfrontiers.owner", frontier.getOwner());
        dimension = I18n.get("mapfrontiers.dimension", frontier.getDimension().identifier().toString());

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            count = frontier.getVertexCount();
        } else if (frontier.getMode() == FrontierData.Mode.Path) {
            count = frontier.getPointCount();
        } else {
            count = frontier.getChunkCount();
        }

        offset1 = StringHelper.getMaxWidth(font, I18n.get("mapfrontiers.config.Personal"), I18n.get("mapfrontiers.config.Global"));
    }

    public FrontierOverlay getFrontier() {
        return frontier;
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        int color = ColorConstants.TEXT;
        if (selected) {
            color = ColorConstants.TEXT_HIGHLIGHT;
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_SELECTED);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        int hiddenColor = ColorConstants.TEXT_DARK;
        int maxNameWidth = METADATA_X - NAME_X - NAME_METADATA_SPACING;
        String visibleName1 = ellipsize(name1, maxNameWidth);
        String visibleName2 = ellipsize(name2, maxNameWidth);
        boolean name1Truncated = !visibleName1.equals(name1);
        boolean name2Truncated = !visibleName2.equals(name2);
        boolean showExpandedNames = isNameAreaHovered(mouseX, mouseY) && (name1Truncated || name2Truncated);

        graphics.text(font, type, x + METADATA_X, y + 4, color);
        graphics.text(font, dimension, x + METADATA_X, y + 14, ColorConstants.TEXT_DIMENSION);
        graphics.text(font, owner, x + METADATA_X + OWNER_X_OFFSET + offset1, y + 4, color);

        drawNameLine(graphics, name1, visibleName1, name1Truncated, showExpandedNames, NAME_LINE_1_Y,
                frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier), color, hiddenColor);
        drawNameLine(graphics, name2, visibleName2, name2Truncated, showExpandedNames, NAME_LINE_2_Y,
                frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier), color, hiddenColor);

        drawModeBadge(graphics, selected);
    }

    private void drawModeBadge(GuiGraphicsExtractor graphics, boolean selected) {
        Identifier fillTexture;
        Identifier outlineTexture;
        switch (frontier.getMode()) {
            case Vertex -> {
                fillTexture = VERTEX_FILL_TEXTURE;
                outlineTexture = VERTEX_OUTLINE_TEXTURE;
            }
            case Path -> {
                fillTexture = PATH_FILL_TEXTURE;
                outlineTexture = PATH_OUTLINE_TEXTURE;
            }
            case Chunk -> {
                fillTexture = CHUNK_FILL_TEXTURE;
                outlineTexture = CHUNK_OUTLINE_TEXTURE;
            }
            default -> throw new IllegalStateException("Unexpected frontier mode: " + frontier.getMode());
        }

        int iconX = x + MODE_BADGE_X;
        int iconY = y + MODE_BADGE_ICON_Y;
        graphics.blit(RenderPipelines.GUI_TEXTURED, fillTexture, iconX, iconY, 0, 0, MODE_BADGE_ICON_WIDTH,
                MODE_BADGE_ICON_HEIGHT, MODE_BADGE_ICON_WIDTH, MODE_BADGE_ICON_HEIGHT, frontier.getColor() | 0xff000000);
        int outlineColor = selected ? ColorConstants.WHITE : ColorConstants.TEXT_DARK;
        graphics.blit(RenderPipelines.GUI_TEXTURED, outlineTexture, iconX, iconY, 0, 0, MODE_BADGE_ICON_WIDTH,
                MODE_BADGE_ICON_HEIGHT, MODE_BADGE_ICON_WIDTH, MODE_BADGE_ICON_HEIGHT, outlineColor);

        String countText = Integer.toString(count);
        int countX = iconX + (MODE_BADGE_ICON_WIDTH - font.width(countText)) / 2;
        graphics.text(font, countText, countX, y + MODE_BADGE_COUNT_Y, ColorConstants.TEXT_DIMENSION);
    }

    private void drawNameLine(GuiGraphicsExtractor graphics,
                              String fullName,
                              String visibleName,
                              boolean truncated,
                              boolean showExpandedNames,
                              int lineY,
                              boolean visible,
                              int visibleColor,
                              int hiddenColor) {
        String renderedName = showExpandedNames && truncated ? fullName : visibleName;

        if (showExpandedNames && truncated) {
            int textWidth = font.width(fullName);
            int bgLeft = x + NAME_X - 1;
            int bgOpaqueRight = bgLeft + textWidth + 2;
            graphics.fill(bgLeft, y + lineY + NAME_LINE_BG_TOP_OFFSET, bgOpaqueRight, y + lineY + NAME_LINE_BG_BOTTOM_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
            graphics.blit(RenderPipelines.GUI_TEXTURED, NAME_FADE_TEXTURE, bgOpaqueRight, y + lineY + NAME_LINE_BG_TOP_OFFSET, 0, 0,
                    NAME_LINE_BG_FADE_WIDTH, NAME_LINE_BG_BOTTOM_OFFSET - NAME_LINE_BG_TOP_OFFSET, NAME_LINE_BG_FADE_WIDTH,
                    NAME_LINE_BG_BOTTOM_OFFSET - NAME_LINE_BG_TOP_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
        }

        if (visible) {
            graphics.text(font, renderedName, x + NAME_X, y + lineY, visibleColor);
        } else {
            graphics.text(font, ChatFormatting.STRIKETHROUGH + renderedName, x + NAME_X, y + lineY, hiddenColor);
        }
    }

    private String ellipsize(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(ELLIPSIS, maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS;
    }

    private boolean isNameAreaHovered(int mouseX, int mouseY) {
        return mouseX >= x + NAME_HOVER_X && mouseY >= y && mouseX < x + METADATA_X && mouseY < y + height;
    }

    private static Identifier frontierListTexture(String fileName) {
        return Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_list/" + fileName);
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            return ScrollBox.ScrollElement.Action.Clicked;
        }

        return ScrollBox.ScrollElement.Action.None;
    }
}
