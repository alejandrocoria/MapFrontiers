package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class PathStylePreviewWidget extends AbstractWidgetNoNarration {
    private static final int WIDTH = 240;
    private static final int HEIGHT = 240;
    private static final int MAP_SIZE = 240;
    private static final int PREVIEW_TEXT_SIZE = 2;
    private static final int PREVIEW_BANNER_SIZE = 1;
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));

    private final FrontierPreviewPanel previewPanel;
    private final FrontierOverlay previewFrontier;
    private @Nullable FrontierData.PathStyle appliedStyle;
    private float scaleFactor = 1.f;

    public PathStylePreviewWidget() {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        previewPanel = new FrontierPreviewPanel();
        previewFrontier = new FrontierOverlay(createPreviewFrontierData(), null);
        previewFrontier.setPreviewLabelSizes(PREVIEW_TEXT_SIZE, PREVIEW_BANNER_SIZE);
        appliedStyle = createPreviewStyle(previewFrontier.getPathStyle());
        updatePreview();
    }

    public void setPathStyle(FrontierData.PathStyle style) {
        FrontierData.PathStyle previewStyle = createPreviewStyle(style);
        if (previewStyle.equals(appliedStyle)) {
            return;
        }

        appliedStyle = new FrontierData.PathStyle(previewStyle);
        previewFrontier.setPathStyle(previewStyle);
        updatePreview();
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale() / scaleFactor;
        setWidth((int) (WIDTH / guiScale));
        setHeight((int) (HEIGHT / guiScale));

        previewPanel.refreshRenderer();
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        previewPanel.drawPanelBackground(graphics, getX(), getY(), getWidth(), getHeight());
        drawPreview(graphics);
        graphics.nextStratum();
        previewPanel.drawPanelBorder(graphics, getX(), getY(), getWidth(), getHeight());
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        int mapSize = getScaledMapSize();
        int mapX = getX() + (getWidth() - mapSize) / 2;
        int mapY = getY() + (getHeight() - mapSize) / 2;
        previewPanel.drawPreview(graphics, mapX, mapY, MAP_SIZE, scaleFactor);
    }

    private void updatePreview() {
        previewPanel.recalculateAndSetFrontiers(List.of(previewFrontier));
    }

    private int getScaledMapSize() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale() / scaleFactor;
        return Math.max(1, Math.round((float) (MAP_SIZE / guiScale)));
    }

    private static FrontierData createPreviewFrontierData() {
        SettingsUser owner = new SettingsUser();
        owner.username = "Player";

        FrontierData frontierData = new FrontierData();
        frontierData.setMode(FrontierData.Mode.Path);
        frontierData.setOwner(owner);
        frontierData.setName1("Preview");
        frontierData.setName2("");
        frontierData.setColor(0xFFAACC60);
        frontierData.setDimension(OVERWORLD);
        PreviewFrontierHelper.setPreviewBanner(frontierData);
        setPreviewVisibility(frontierData);
        frontierData.addPoint(new BlockPos(55, 70, 165));
        frontierData.addPoint(new BlockPos(120, 70, 70));
        frontierData.addPoint(new BlockPos(185, 70, 165));
        return frontierData;
    }

    private static void setPreviewVisibility(FrontierData frontierData) {
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.Frontier, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.Fullscreen, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
    }

    private static FrontierData.PathStyle createPreviewStyle(FrontierData.PathStyle style) {
        FrontierData.PathStyle previewStyle = new FrontierData.PathStyle(style);
        if (!previewStyle.labelAtStart && !previewStyle.labelAtMiddle && !previewStyle.labelAtEnd) {
            previewStyle.labelAtStart = true;
        }
        return previewStyle;
    }
}
