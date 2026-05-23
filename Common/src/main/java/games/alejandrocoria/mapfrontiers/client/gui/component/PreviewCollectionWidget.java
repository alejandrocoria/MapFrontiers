package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierVisibility;
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
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PreviewCollectionWidget extends AbstractWidgetNoNarration {
    private static final int SIZE = 420;
    private static final int PREVIEW_COLLECTION_COLOR = 0xFF60DDCC;
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));

    private final FrontierPreviewPanel previewPanel;
    private final List<FrontierOverlay> previewFrontiers = new ArrayList<>();
    private float scaleFactor = 1.f;

    public PreviewCollectionWidget() {
        super(0, 0, SIZE, SIZE, Component.empty());
        previewPanel = new FrontierPreviewPanel();

        SettingsUser owner = new SettingsUser();
        owner.username = "Player";

        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(30, 70, 45),
                new BlockPos(30, 70, 280),
                new BlockPos(175, 70, 280),
                new BlockPos(175, 70, 45)));
        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(175, 70, 45),
                new BlockPos(175, 70, 175),
                new BlockPos(335, 70, 175),
                new BlockPos(335, 70, 45)));
        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(175, 70, 175),
                new BlockPos(175, 70, 335),
                new BlockPos(375, 70, 335),
                new BlockPos(375, 70, 175)));

        configUpdated();
    }

    public void configUpdated() {
        for (FrontierOverlay frontier : previewFrontiers) {
            frontier.setPreviewCollectionStyle(PREVIEW_COLLECTION_COLOR);
        }
        previewPanel.recalculateAndSetFrontiers(previewFrontiers);
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale() / scaleFactor;
        setWidth((int) (SIZE / guiScale));
        setHeight((int) (SIZE / guiScale));

        previewPanel.refreshRenderer();
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        previewPanel.drawPanelBackground(graphics, getX(), getY(), getWidth(), getHeight(), SIZE);
        previewPanel.drawPreview(graphics, getX(), getY(), SIZE, scaleFactor);
        graphics.nextStratum();
        previewPanel.drawPanelBorder(graphics, getX(), getY(), getWidth(), getHeight());
    }

    private static FrontierOverlay createPreviewFrontier(SettingsUser owner, BlockPos... vertices) {
        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1("Preview");
        frontierData.setColor(0xFFAACC60);
        frontierData.setDimension(OVERWORLD);
        frontierData.setVisibility(FrontierVisibility.Frontier, true);
        frontierData.setVisibility(FrontierVisibility.Fullscreen, true);
        frontierData.setVisibility(FrontierVisibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierVisibility.FullscreenName, false);
        frontierData.setVisibility(FrontierVisibility.FullscreenCollection, false);
        frontierData.setVisibility(FrontierVisibility.FullscreenOwner, false);
        frontierData.setVisibility(FrontierVisibility.FullscreenBanner, false);
        for (BlockPos vertex : vertices) {
            frontierData.addVertex(vertex);
        }

        return new FrontierOverlay(frontierData, null);
    }
}
