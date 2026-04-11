package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
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
    private static final Identifier backgroundTexture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_preview_bg.png");
    private static final int WIDTH = 220;
    private static final int HEIGHT = 150;
    private static final int MAP_SIZE = 150;
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));

    private final IJourneyMapHelper.ICustomPreviewRenderer customPreviewRenderer;
    private final FrontierOverlay previewFrontier;
    private @Nullable FrontierData.PathStyle appliedStyle;
    private float scaleFactor = 1.f;

    public PathStylePreviewWidget() {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        previewFrontier = new FrontierOverlay(createPreviewFrontierData(), null);
        customPreviewRenderer = Services.JOURNEYMAP.createCustomPreviewRenderer();
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

        customPreviewRenderer.setFrontiers(List.of(previewFrontier));
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), 420, 420, 420, 420);
        graphics.horizontalLine(getX(), getX() + getWidth() - 1, getY(), ColorConstants.OPTION_BORDER);
        graphics.horizontalLine(getX(), getX() + getWidth() - 1, getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(getX(), getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(getX() + getWidth() - 1, getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);

        int mapSize = getScaledMapSize();
        int mapX = getX() + (getWidth() - mapSize) / 2;
        int mapY = getY() + (getHeight() - mapSize) / 2;
        customPreviewRenderer.draw(graphics, Minecraft.getInstance().renderBuffers().bufferSource(), mapX, mapY, MAP_SIZE, scaleFactor);
    }

    private void updatePreview() {
        previewFrontier.recalculateOverlays();
        customPreviewRenderer.setFrontiers(List.of(previewFrontier));
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
        frontierData.setName2("Path");
        frontierData.setColor(0xFFAACC60);
        frontierData.setDimension(OVERWORLD);
        PreviewFrontierHelper.setPreviewBanner(frontierData);
        setPreviewVisibility(frontierData);
        frontierData.addPoint(new BlockPos(20, 70, 105));
        frontierData.addPoint(new BlockPos(75, 70, 45));
        frontierData.addPoint(new BlockPos(130, 70, 95));
        return frontierData;
    }

    private static void setPreviewVisibility(FrontierData frontierData) {
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.Frontier, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.Fullscreen, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, true);
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
