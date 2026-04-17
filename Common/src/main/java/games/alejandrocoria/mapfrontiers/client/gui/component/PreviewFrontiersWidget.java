package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PreviewFrontiersWidget extends AbstractWidgetNoNarration {
    private static final int SIZE = 420;
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));

    private final FrontierPreviewPanel previewPanel;
    private final List<FrontierOverlay> previewFrontiers = new ArrayList<>();
    private float scaleFactor = 1;

    public PreviewFrontiersWidget() {
        super(0, 0, SIZE, SIZE, Component.empty());
        previewPanel = new FrontierPreviewPanel();

        SettingsUser owner = new SettingsUser();
        owner.username = "Player";
        previewFrontiers.add(new FrontierOverlay(createBannerFrontier(owner), null));
        previewFrontiers.add(new FrontierOverlay(createLongNameFrontier(owner), null));
        previewFrontiers.add(new FrontierOverlay(createPathFrontier(owner), null));

        configUpdated();
    }

    public void configUpdated() {
        previewPanel.recalculateAndSetFrontiers(previewFrontiers);
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale() / scaleFactor;
        setWidth((int) (SIZE / guiScale));
        setHeight((int) (SIZE / guiScale));

        previewPanel.refreshRenderer();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        previewPanel.drawPanelBackground(graphics, getX(), getY(), getWidth(), getHeight(), SIZE);
        previewPanel.drawPreview(graphics, getX(), getY(), SIZE, scaleFactor);
        graphics.nextStratum();
        previewPanel.drawPanelBorder(graphics, getX(), getY(), getWidth(), getHeight());
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    private static FrontierData createBannerFrontier(SettingsUser owner) {
        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1("Preview");
        frontierData.setName2("Frontier");
        frontierData.setColor(0xFFAACC60);
        PreviewFrontierHelper.setPreviewBanner(frontierData);
        frontierData.setDimension(OVERWORLD);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, true);
        frontierData.addVertex(new BlockPos(10, 70, 10));
        frontierData.addVertex(new BlockPos(10, 70, 410));
        frontierData.addVertex(new BlockPos(270, 70, 410));
        frontierData.addVertex(new BlockPos(270, 70, 10));
        return frontierData;
    }

    private static FrontierData createLongNameFrontier(SettingsUser owner) {
        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1("Long name");
        frontierData.setName2("12345678901234567");
        frontierData.setColor(0xFFA0A0FF);
        frontierData.setDimension(OVERWORLD);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, false);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, false);
        frontierData.addVertex(new BlockPos(240, 70, 280));
        frontierData.addVertex(new BlockPos(300, 70, 245));
        frontierData.addVertex(new BlockPos(360, 70, 280));
        frontierData.addVertex(new BlockPos(360, 70, 350));
        frontierData.addVertex(new BlockPos(300, 70, 385));
        frontierData.addVertex(new BlockPos(240, 70, 350));
        return frontierData;
    }

    private static FrontierData createPathFrontier(SettingsUser owner) {
        FrontierData frontierData = new FrontierData();
        frontierData.setMode(FrontierData.Mode.Path);
        frontierData.setOwner(owner);
        frontierData.setName1("Path");
        frontierData.setName2("Preview");
        frontierData.setColor(0xFFFFC04D);
        frontierData.setDimension(OVERWORLD);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, false);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, false);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, false);
        frontierData.addPoint(new BlockPos(305, 70, 40));
        frontierData.addPoint(new BlockPos(375, 70, 95));
        frontierData.addPoint(new BlockPos(325, 70, 150));
        frontierData.addPoint(new BlockPos(390, 70, 185));
        return frontierData;
    }
}
