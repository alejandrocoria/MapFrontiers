package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.frontier.MarkerImageConstants;
import games.alejandrocoria.mapfrontiers.client.frontier.PathMarkerCatalog;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PathMarkerSelectorWidget extends AbstractWidgetNoNarration {
    private static final int CELL_SIZE = 18;
    private static final int CELL_SPACING = 2;

    private Consumer<Identifier> onPress;
    private Identifier selectedId;

    public PathMarkerSelectorWidget(Identifier selectedId, Consumer<Identifier> onPress) {
        super(0, 0, getSelectorWidth(), CELL_SIZE, Component.empty());
        this.selectedId = selectedId;
        this.onPress = onPress;
    }

    public void setOnPress(Consumer<Identifier> onPress) {
        this.onPress = onPress;
    }

    public void setSelectedId(Identifier selectedId) {
        this.selectedId = selectedId;
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int hoveredIndex = getHoveredIndex(event.x(), event.y());
        if (hoveredIndex < 0 || hoveredIndex >= PathMarkerCatalog.BUILT_INS.size()) {
            return false;
        }

        selectedId = PathMarkerCatalog.BUILT_INS.get(hoveredIndex).id();
        onPress.accept(selectedId);
        return true;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int hoveredIndex = getHoveredIndex(mouseX, mouseY);

        int x = getX();
        for (PathMarkerCatalog.Entry entry : PathMarkerCatalog.BUILT_INS) {
            boolean selected = entry.id().equals(selectedId);
            boolean hovered = hoveredIndex >= 0 && PathMarkerCatalog.BUILT_INS.get(hoveredIndex) == entry;

            int borderColor = selected ? ColorConstants.OPTION_BORDER_FOCUSED : hovered ? ColorConstants.OPTION_BORDER : ColorConstants.CHECKBOX_BORDER;
            graphics.fill(x, getY(), x + CELL_SIZE, getY() + CELL_SIZE, borderColor);
            graphics.fill(x + 1, getY() + 1, x + CELL_SIZE - 1, getY() + CELL_SIZE - 1, ColorConstants.PATH_MARKER_SELECTOR_BG);

            if (entry.texture() != null) {
                int markerX = x + (CELL_SIZE - MarkerImageConstants.DISPLAY_SIZE) / 2;
                int markerY = getY() + (CELL_SIZE - MarkerImageConstants.DISPLAY_SIZE) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), markerX, markerY, 0, 0,
                        MarkerImageConstants.DISPLAY_SIZE, MarkerImageConstants.DISPLAY_SIZE,
                        MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE,
                        MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE);
            }

            x += CELL_SIZE + CELL_SPACING;
        }
    }

    private int getHoveredIndex(double mouseX, double mouseY) {
        if (!active || mouseX < getX() || mouseX >= getX() + getWidth() || mouseY < getY() || mouseY >= getY() + getHeight()) {
            return -1;
        }

        double localX = mouseX - getX();
        int step = CELL_SIZE + CELL_SPACING;
        int index = (int) (localX / step);
        double cellOffset = localX - index * step;
        if (index < 0 || index >= PathMarkerCatalog.BUILT_INS.size() || cellOffset >= CELL_SIZE) {
            return -1;
        }

        return index;
    }

    private static int getSelectorWidth() {
        return PathMarkerCatalog.BUILT_INS.size() * CELL_SIZE + (PathMarkerCatalog.BUILT_INS.size() - 1) * CELL_SPACING;
    }
}
