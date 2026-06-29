package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlayKey;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PreviewCollectionWidget extends AbstractWidgetNoNarration {
    private static final int SIZE = 420;
    private static final int PREVIEW_COLLECTION_COLOR = 0xFF60DDCC;
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));

    private final FrontierPreviewPanel previewPanel;
    private final List<FrontierOverlay> previewFrontiers = new ArrayList<>();
    private final CollectionData previewCollectionData;
    private final CollectionOverlay previewCollectionOverlay;
    private float scaleFactor = 1.f;

    public PreviewCollectionWidget() {
        super(0, 0, SIZE, SIZE, Component.empty());
        previewPanel = new FrontierPreviewPanel();

        SettingsUser owner = PreviewFrontierHelper.createPreviewOwner();
        previewCollectionData = createPreviewCollection(owner);
        previewCollectionOverlay = new CollectionOverlay(new CollectionOverlayKey(previewCollectionData.getId(), OVERWORLD), null,
                previewCollectionData, List.of());

        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(30, 70, 45),
                new BlockPos(30, 70, 280),
                new BlockPos(175, 70, 280),
                new BlockPos(175, 70, 45)));
        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(175, 70, 45),
                new BlockPos(175, 70, 155),
                new BlockPos(335, 70, 155),
                new BlockPos(335, 70, 45)));
        previewFrontiers.add(createPreviewFrontier(owner,
                new BlockPos(175, 70, 155),
                new BlockPos(175, 70, 335),
                new BlockPos(375, 70, 335),
                new BlockPos(375, 70, 155)));

        configUpdated();
    }

    public void configUpdated() {
        for (FrontierOverlay frontier : previewFrontiers) {
            frontier.setPreviewCollectionStyle(PREVIEW_COLLECTION_COLOR);
        }
        previewCollectionOverlay.refreshMembersAndCollection(previewCollectionData, previewFrontiers);
        previewCollectionOverlay.rebuildOverlayNow();
        previewPanel.recalculateAndSetTerritories(previewFrontiers, List.of(previewCollectionOverlay));
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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        previewPanel.drawPanelBackground(graphics, getX(), getY(), getWidth(), getHeight(), SIZE);
        previewPanel.drawPreview(graphics, getX(), getY(), SIZE, scaleFactor);
        previewPanel.drawPanelBorder(graphics, getX(), getY(), getWidth(), getHeight());
    }

    private static FrontierOverlay createPreviewFrontier(SettingsUser owner, BlockPos... vertices) {
        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1(PreviewFrontierHelper.translate("mapfrontiers.preview_name_1"));
        frontierData.setName2(PreviewFrontierHelper.translate("mapfrontiers.preview_name_2"));
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

    private static CollectionData createPreviewCollection(SettingsUser owner) {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setOwner(owner);
        collection.setName(PreviewFrontierHelper.translate("mapfrontiers.preview_collection"));
        collection.setColor(PREVIEW_COLLECTION_COLOR);
        collection.getVisibilityData().setVisible(true);
        collection.getVisibilityData().setFullscreenZoom(512);
        PreviewFrontierHelper.setPreviewBanner(collection);
        return collection;
    }
}
