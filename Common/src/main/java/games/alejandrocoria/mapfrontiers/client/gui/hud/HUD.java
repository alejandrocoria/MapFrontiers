package games.alejandrocoria.mapfrontiers.client.gui.hud;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.FrontierDisplayVisibility;
import games.alejandrocoria.mapfrontiers.client.config.HUDAnchor;
import games.alejandrocoria.mapfrontiers.client.config.HUDSlot;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewFrontierHelper;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@ParametersAreNonnullByDefault
public class HUD {
    private static final Minecraft mc = Minecraft.getInstance();

    private FrontierOverlay frontier;
    private int frontierHash;
    private long activeFrontiersRevision = -1L;
    private final EnumMap<HUDSlot, SlotRenderer> slotRenderers;
    private final List<PreparedSlot> preparedSlots;
    private int posX = 0;
    private int posY = 0;
    private int hudWidth = 0;
    private int hudHeight = 0;
    private int textScale = 1;
    private int bannerScale = 1;
    private boolean needUpdate = true;
    private boolean previewMode = false;
    private int displayWidth;
    private int displayHeight;

    public static HUD asPreview() {
        HUD hud = new HUD();
        hud.previewMode = true;

        FrontierData frontierData = new FrontierData();
        SettingsUser owner = PreviewFrontierHelper.createPreviewOwner();
        frontierData.setOwner(owner);
        frontierData.setName1(PreviewFrontierHelper.translate("mapfrontiers.preview_name_1"));
        frontierData.setName2(PreviewFrontierHelper.translate("mapfrontiers.preview_name_2"));
        PreviewFrontierHelper.setPreviewBanner(frontierData);

        hud.frontier = new FrontierOverlay(frontierData, null);

        return hud;
    }

    public HUD() {
        slotRenderers = new EnumMap<>(HUDSlot.class);
        slotRenderers.put(HUDSlot.Collection, new CollectionSlotRenderer());
        slotRenderers.put(HUDSlot.Name, new NameSlotRenderer());
        slotRenderers.put(HUDSlot.Owner, new OwnerSlotRenderer());
        slotRenderers.put(HUDSlot.Banner, new BannerSlotRenderer());
        preparedSlots = new ArrayList<>();

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> frontierChanged());
        MapFrontiersClient.getFrontierEvents().subscribeCreated(this, (frontierOverlay, playerID) -> frontierChanged());
        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> frontierChanged());
        ClientGlobalEvents.subscribeUpdatedConfigEvent(this, this::configUpdated);
    }

    public int getWidth() {
        return hudWidth;
    }

    public int getHeight() {
        return hudHeight;
    }

    public boolean isInside(int x, int y) {
        return x >= posX && x < posX + hudWidth && y >= posY && y < posY + hudHeight;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public void tick() {
        if (previewMode || mc.player == null || ClientConfig.FRONTIER_VISIBILITY.get() == FrontierDisplayVisibility.Never) {
            return;
        }

        long currentActiveFrontiersRevision = MapFrontiersClient.getHudActiveFrontiersRevision();
        if (activeFrontiersRevision != currentActiveFrontiersRevision) {
            activeFrontiersRevision = currentActiveFrontiersRevision;
            List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersForHUD();
            if (!frontiers.isEmpty()) {
                FrontierOverlay newFrontier = frontiers.get(0);
                if (frontierHash != newFrontier.getHash()) {
                    frontier = newFrontier;
                    frontierHash = newFrontier.getHash();
                    needUpdate = true;
                }
            } else if (frontier != null) {
                frontier = null;
                frontierHash = 0;
                needUpdate = true;
            }
        }

        if (frontier != null && frontierHash != frontier.getHash()) {
            frontierHash = frontier.getHash();
            needUpdate = true;
        }
    }

    public void configUpdated() {
        if (previewMode) {
            updateData();
        } else {
            needUpdate = true;
        }
    }

    public void frontierChanged() {
        if (previewMode) {
            return;
        }

        List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersForHUD();
        if (!frontiers.isEmpty()) {
            FrontierOverlay newFrontier = frontiers.get(0);
            if (frontierHash != newFrontier.getHash()) {
                frontier = newFrontier;
                frontierHash = newFrontier.getHash();
                needUpdate = true;
            }
        } else if (frontier != null) {
            frontier = null;
            frontierHash = 0;
            needUpdate = true;
        }
    }

    public void drawInGameHUD(GuiGraphics graphics, float partialTicks) {
        if (previewMode) {
            return;
        }

        if (mc.options.hideGui) {
            return;
        }

        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            return;
        }

        if (frontier == null) {
            return;
        }

        if (!ClientConfig.HUD_ENABLED.get()) {
            return;
        }

        draw(graphics, partialTicks);
    }

    public void draw(GuiGraphics graphics, float partialTicks) {
        if (displayWidth != mc.getWindow().getWidth() || displayHeight != mc.getWindow().getHeight()) {
            needUpdate = true;
        }

        if (ClientConfig.HUD_ANCHOR.get() == HUDAnchor.Minimap || ClientConfig.HUD_ANCHOR.get() == HUDAnchor.MinimapHorizontal
                || ClientConfig.HUD_ANCHOR.get() == HUDAnchor.MinimapVertical) {
            if (Services.JOURNEYMAP.minimapPropertiesChanged()) {
                needUpdate = true;
            }
        }

        if (needUpdate) {
            needUpdate = false;
            updateData();
        }

        if (preparedSlots.isEmpty()) {
            return;
        }

        float factor = (float) mc.getWindow().getGuiScale();

        int frameColor = Services.JOURNEYMAP.minimapLabelBackgroundColor();
        int textNameColor = Services.JOURNEYMAP.minimapLabelHighlightColor();
        int textOwnerColor = Services.JOURNEYMAP.minimapLabelForegroundColor();

        graphics.pose().pushPose();
        graphics.pose().scale(1.0f / factor, 1.0f / factor, 1.0f);

        for (PreparedSlot slot : preparedSlots) {
            slot.render(graphics, frameColor, textNameColor, textOwnerColor, partialTicks);
        }

        graphics.pose().popPose();
    }

    private void updateData() {
        displayWidth = mc.getWindow().getWidth();
        displayHeight = mc.getWindow().getHeight();
        preparedSlots.clear();
        hudWidth = 0;
        hudHeight = 0;

        if (frontier == null) {
            return;
        }

        textScale = ClientConfig.HUD_TEXT_SIZE.get();
        bannerScale = ClientConfig.HUD_BANNER_SIZE.get();

        List<SlotRenderer> visibleSlots = new ArrayList<>();
        for (HUDSlot slot : ClientConfig.getHUDSlots()) {
            SlotRenderer renderer = slotRenderers.get(slot);
            if (renderer != null && renderer.isVisible()) {
                visibleSlots.add(renderer);
            }
        }

        if (visibleSlots.isEmpty()) {
            return;
        }

        for (SlotRenderer renderer : visibleSlots) {
            hudWidth = Math.max(hudWidth, renderer.getWidth());
            hudHeight += renderer.getHeight();
        }

        HUDPlacementHelper.Point anchorPos = HUDPlacementHelper.getHUDAnchor(ClientConfig.HUD_ANCHOR.get());
        HUDPlacementHelper.Point originPos = HUDPlacementHelper.getHUDOrigin(ClientConfig.HUD_ANCHOR.get(), hudWidth, hudHeight);
        posX = anchorPos.x - originPos.x + ClientConfig.HUD_X_POSITION.get();
        posY = anchorPos.y - originPos.y + ClientConfig.HUD_Y_POSITION.get();

        int offsetY = 0;
        for (SlotRenderer renderer : visibleSlots) {
            PreparedSlot preparedSlot = renderer.prepare(offsetY);
            preparedSlots.add(preparedSlot);
            offsetY += preparedSlot.getHeight();
        }
    }

    private String getOwnerString() {
        String ownerString = "";
        if (!StringUtils.isBlank(frontier.getOwner().username)) {
            ownerString = frontier.getOwner().username;
        } else if (frontier.getOwner().uuid != null) {
            ownerString = frontier.getOwner().uuid.toString();
            ownerString = ownerString.substring(0, 8) + "...";
        }

        return ownerString;
    }

    private StringWidget createCenteredWidget(String text, int topY) {
        return createCenteredWidget(Component.literal(text), topY);
    }

    private StringWidget createCenteredWidget(Component text, int topY) {
        StringWidget widget = new StringWidget(text, mc.font, StringWidget.Align.Center);
        widget.setX(posX + hudWidth / 2);
        widget.setY(topY + 2 * textScale);
        widget.setScale(textScale);
        return widget;
    }

    private interface SlotRenderer {
        boolean isVisible();
        int getWidth();
        int getHeight();
        PreparedSlot prepare(int offsetY);
    }

    private interface PreparedSlot {
        int getHeight();
        void render(GuiGraphics graphics, int frameColor, int textNameColor, int textOwnerColor, float partialTicks);
    }

    private class NameSlotRenderer implements SlotRenderer {
        @Override
        public boolean isVisible() {
            return frontier.isNamed();
        }

        @Override
        public int getWidth() {
            int name1Width = mc.font.width(frontier.getName1()) + 3;
            int name2Width = mc.font.width(frontier.getName2()) + 3;
            return Math.max(name1Width, name2Width) * textScale;
        }

        @Override
        public int getHeight() {
            return getLineCount() * 12 * textScale;
        }

        @Override
        public PreparedSlot prepare(int offsetY) {
            List<StringWidget> widgets = new ArrayList<>();
            int currentY = posY + offsetY;

            if (!StringUtils.isBlank(frontier.getName1())) {
                widgets.add(createCenteredWidget(frontier.getName1(), currentY));
                currentY += 12 * textScale;
            }

            if (!StringUtils.isBlank(frontier.getName2())) {
                widgets.add(createCenteredWidget(frontier.getName2(), currentY));
            }

            return new TextPreparedSlot(offsetY, getHeight(), widgets, true);
        }

        private int getLineCount() {
            int lineCount = 0;
            if (!StringUtils.isBlank(frontier.getName1())) {
                ++lineCount;
            }
            if (!StringUtils.isBlank(frontier.getName2())) {
                ++lineCount;
            }
            return lineCount;
        }
    }

    private class CollectionSlotRenderer implements SlotRenderer {
        @Override
        public boolean isVisible() {
            return previewMode || getCollectionName() != null;
        }

        @Override
        public int getWidth() {
            return (mc.font.width(getCollectionComponent()) + 3) * textScale;
        }

        @Override
        public int getHeight() {
            return 12 * textScale;
        }

        @Override
        public PreparedSlot prepare(int offsetY) {
            List<StringWidget> widgets = List.of(createCenteredWidget(getCollectionComponent(), posY + offsetY));
            return new TextPreparedSlot(offsetY, getHeight(), widgets, true);
        }
    }

    private class OwnerSlotRenderer implements SlotRenderer {
        @Override
        public boolean isVisible() {
            return !frontier.getOwner().isEmpty();
        }

        @Override
        public int getWidth() {
            return (mc.font.width(getOwnerString()) + 3) * textScale;
        }

        @Override
        public int getHeight() {
            return 12 * textScale;
        }

        @Override
        public PreparedSlot prepare(int offsetY) {
            List<StringWidget> widgets = List.of(createCenteredWidget(ChatFormatting.ITALIC + getOwnerString(), posY + offsetY));
            return new TextPreparedSlot(offsetY, getHeight(), widgets, false);
        }
    }

    private class BannerSlotRenderer implements SlotRenderer {
        @Override
        public boolean isVisible() {
            return frontier.getBannerRenderer().hasBanner();
        }

        @Override
        public int getWidth() {
            int[] bannerBounds = frontier.getBannerBounds(0, 0, bannerScale);
            return bannerBounds[2] - bannerBounds[0] + 4;
        }

        @Override
        public int getHeight() {
            int[] bannerBounds = frontier.getBannerBounds(0, 0, bannerScale);
            return bannerBounds[3] - bannerBounds[1] + 4;
        }

        @Override
        public PreparedSlot prepare(int offsetY) {
            int bannerX = posX + hudWidth / 2;
            int slotTop = posY + offsetY;
            int bannerLeft = bannerX - 11 * bannerScale;
            int[] localBounds = frontier.getBannerBounds(bannerLeft, 0, bannerScale);
            int bannerY = slotTop + 2 - localBounds[1];
            int[] bannerBounds = frontier.getBannerBounds(bannerLeft, bannerY, bannerScale);
            return new BannerPreparedSlot(getHeight(), bannerX, bannerY, bannerBounds);
        }
    }

    private class TextPreparedSlot implements PreparedSlot {
        private final int offsetY;
        private final int height;
        private final List<StringWidget> widgets;
        private final boolean nameSlot;

        private TextPreparedSlot(int offsetY, int height, List<StringWidget> widgets, boolean nameSlot) {
            this.offsetY = offsetY;
            this.height = height;
            this.widgets = widgets;
            this.nameSlot = nameSlot;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void render(GuiGraphics graphics, int frameColor, int textNameColor, int textOwnerColor, float partialTicks) {
            graphics.fill(posX, posY + offsetY, posX + hudWidth, posY + offsetY + height, frameColor);

            int textColor = nameSlot ? textNameColor : textOwnerColor;
            for (StringWidget widget : widgets) {
                widget.setColor(textColor);
                widget.render(graphics, 0, 0, partialTicks);
            }
        }
    }

    private class BannerPreparedSlot implements PreparedSlot {
        private final int height;
        private final int bannerX;
        private final int bannerY;
        private final int[] bannerBounds;

        private BannerPreparedSlot(int height, int bannerX, int bannerY, int[] bannerBounds) {
            this.height = height;
            this.bannerX = bannerX;
            this.bannerY = bannerY;
            this.bannerBounds = bannerBounds;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void render(GuiGraphics graphics, int frameColor, int textNameColor, int textOwnerColor, float partialTicks) {
            graphics.fill(bannerBounds[0] - 2, bannerBounds[1] - 2, bannerBounds[2] + 2, bannerBounds[3] + 2, frameColor);
            frontier.getBannerRenderer().renderBanner(graphics, bannerX, bannerY, bannerScale);
        }
    }

    private @Nullable String getCollectionName() {
        if (previewMode) {
            return PreviewFrontierHelper.translate("mapfrontiers.preview_collection");
        }

        if (frontier == null || frontier.getCollectionId() == null) {
            return null;
        }

        CollectionData collection = MapFrontiersClient.getCollection(frontier.getCollectionId());
        if (collection == null) {
            return null;
        }

        String name = collection.getName().trim();
        return name.isEmpty() ? null : name;
    }

    private Component getCollectionComponent() {
        String collectionName = getCollectionName();
        if (collectionName == null) {
            return Component.empty();
        }

        return Component.literal(collectionName).withStyle(ChatFormatting.BOLD);
    }
}
