package games.alejandrocoria.mapfrontiers.client.gui.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class HUD {
    private static final Minecraft mc = Minecraft.getInstance();

    private final FrontiersOverlayManager frontiersOverlayManager;
    private final FrontiersOverlayManager personalFrontiersOverlayManager;
    private FrontierOverlay frontier;
    private int frontierHash;
    private BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private final SimpleLabel frontierName1;
    private final SimpleLabel frontierName2;
    private final SimpleLabel frontierOwner;
    private final List<Config.HUDSlot> slots;
    private int posX = 0;
    private int posY = 0;
    private int nameOffsetY = 0;
    private int ownerOffsetY = 0;
    private int bannerOffsetY = 0;
    private int hudWidth = 0;
    private int hudHeight = 0;
    private int textScale = 1;
    private int bannerScale = 1;
    private boolean needUpdate = true;
    private boolean previewMode = false;
    private int displayWidth;
    private int displayHeight;

    public static HUD asPreview() {
        HUD hud = new HUD(null, null);
        hud.previewMode = true;

        HolderLookup<BannerPattern> patternRegistry = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN).get();
        BannerPatternLayers patterns = (new BannerPatternLayers.Builder())
                .add(patternRegistry.get(BannerPatterns.FLOWER).get(), DyeColor.GREEN)
                .add(patternRegistry.get(BannerPatterns.BRICKS).get(), DyeColor.LIGHT_GRAY)
                .add(patternRegistry.get(BannerPatterns.BORDER).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_TOP).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_BOTTOM).get(), DyeColor.BLACK)
                .add(patternRegistry.get(BannerPatterns.STRIPE_BOTTOM).get(), DyeColor.GREEN).build();

        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(new SettingsUser(mc.player));
        frontierData.setName1("Preview Frontier");
        frontierData.setName2("-----------------");
        frontierData.setBanner(DyeColor.BLACK, patterns);

        hud.frontier = new FrontierOverlay(frontierData, null);

        return hud;
    }

    public HUD(@Nullable FrontiersOverlayManager frontiersOverlayManager, @Nullable FrontiersOverlayManager personalFrontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.personalFrontiersOverlayManager = personalFrontiersOverlayManager;
        slots = new ArrayList<>();
        frontierName1 = new SimpleLabel(mc.font, 0, 0, SimpleLabel.Align.Center, Component.empty(),
                ColorConstants.WHITE);
        frontierName2 = new SimpleLabel(mc.font, 0, 0, SimpleLabel.Align.Center, Component.empty(),
                ColorConstants.WHITE);
        frontierOwner = new SimpleLabel(mc.font, 0, 0, SimpleLabel.Align.Center, Component.empty(),
                ColorConstants.WHITE);

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> frontierChanged());
        ClientEventHandler.subscribeNewFrontierEvent(this, (frontierOverlay, playerID) -> frontierChanged());
        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> frontierChanged());
        ClientEventHandler.subscribeUpdatedConfigEvent(this, this::configUpdated);
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

    public void tick() {
        if (previewMode || frontiersOverlayManager == null || personalFrontiersOverlayManager == null || mc.player == null) {
            return;
        }

        BlockPos currentPlayerPosition = mc.player.blockPosition();

        if (currentPlayerPosition.getX() != lastPlayerPosition.getX()
                || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
            lastPlayerPosition = currentPlayerPosition;

            FrontierOverlay newFrontier = personalFrontiersOverlayManager
                    .getFrontierInPosition(mc.player.level().dimension(), lastPlayerPosition);
            if (newFrontier == null) {
                newFrontier = frontiersOverlayManager.getFrontierInPosition(mc.player.level().dimension(),
                        lastPlayerPosition);
            }
            if (newFrontier != null) {
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

    private void frontierChanged() {
        if (previewMode || frontiersOverlayManager == null || personalFrontiersOverlayManager == null) {
            return;
        }

        FrontierOverlay newFrontier = personalFrontiersOverlayManager.getFrontierInPosition(mc.player.level().dimension(),
                lastPlayerPosition);
        if (newFrontier == null) {
            newFrontier = frontiersOverlayManager.getFrontierInPosition(mc.player.level().dimension(), lastPlayerPosition);
        }

        if (newFrontier != null) {
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

        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            return;
        }

        if (frontier == null) {
            return;
        }

        if (!Config.hudEnabled) {
            return;
        }

        draw(graphics, partialTicks);
    }

    public void draw(GuiGraphics graphics, float partialTicks) {
        if (displayWidth != mc.getWindow().getWidth() || displayHeight != mc.getWindow().getHeight()) {
            needUpdate = true;
        }

        if (Config.hudAnchor == Config.HUDAnchor.Minimap || Config.hudAnchor == Config.HUDAnchor.MinimapHorizontal
                || Config.hudAnchor == Config.HUDAnchor.MinimapVertical) {
            if (Services.JOURNEYMAP.minimapPropertiesChanged()) {
                needUpdate = true;
            }
        }

        if (needUpdate) {
            needUpdate = false;
            updateData();
        }

        if (slots.isEmpty()) {
            return;
        }

        float factor = (float) mc.getWindow().getGuiScale();

        int frameColor = Services.JOURNEYMAP.minimapLabelBackgroundColor();
        int textNameColor = Services.JOURNEYMAP.minimapLabelHighlightColor();
        int textOwnerColor = Services.JOURNEYMAP.minimapLabelForegroundColor();

        graphics.pose().pushPose();
        graphics.pose().scale(1.0f / factor, 1.0f / factor, 1.0f);
        graphics.pose().translate(0.0, 0.0, -100.0);

        for (Config.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                drawName(graphics, frameColor, textNameColor, partialTicks);
                break;
            case Owner:
                drawOwner(graphics, frameColor, textOwnerColor, partialTicks);
                break;
            case Banner:
                drawBanner(graphics, frameColor);
                break;
            case None:
                break;
            }
        }

        graphics.pose().popPose();
        GlStateManager._enableBlend();
    }

    private void drawName(GuiGraphics graphics, int frameColor, int textColor, float partialTicks) {
        graphics.fill(posX, posY + nameOffsetY, posX + hudWidth, posY + nameOffsetY + 24 * textScale, frameColor);

        frontierName1.setColor(textColor);
        frontierName2.setColor(textColor);

        frontierName1.render(graphics, 0, 0, partialTicks);
        frontierName2.render(graphics, 0, 0, partialTicks);
    }

    private void drawOwner(GuiGraphics graphics, int frameColor, int textColor, float partialTicks) {
        graphics.fill(posX, posY + ownerOffsetY, posX + hudWidth, posY + ownerOffsetY + 12 * textScale,
                frameColor);

        frontierOwner.setColor(textColor);
        frontierOwner.render(graphics, 0, 0, partialTicks);
    }

    private void drawBanner(GuiGraphics graphics, int frameColor) {
        graphics.fill(posX + hudWidth / 2 - 11 * bannerScale - 2, posY + bannerOffsetY,
                posX + hudWidth / 2 + 11 * bannerScale + 2, posY + bannerOffsetY + 4 + 40 * bannerScale, frameColor);

        frontier.renderBanner(mc, graphics, posX + hudWidth / 2 - 11 * bannerScale, posY + bannerOffsetY + 2, bannerScale);
    }

    private void updateData() {
        displayWidth = mc.getWindow().getWidth();
        displayHeight = mc.getWindow().getHeight();

        slots.clear();

        if (frontier == null) {
            return;
        }

        addSlot(Config.hudSlot1);
        addSlot(Config.hudSlot2);
        addSlot(Config.hudSlot3);

        if (slots.isEmpty()) {
            return;
        }

        hudWidth = 0;
        hudHeight = 0;
        bannerScale = Config.hudBannerSize;

        textScale = Services.JOURNEYMAP.getMinimapFontScale();

        for (Config.HUDSlot slot : slots) {
            switch (slot) {
                case Name:
                    int name1Width = mc.font.width(frontier.getName1()) + 3;
                    int name2Width = mc.font.width(frontier.getName2()) + 3;
                    int nameWidth = Math.max(name1Width, name2Width) * textScale;
                    hudWidth = Math.max(hudWidth, nameWidth);
                    hudHeight += 24 * textScale;
                    break;
                case Owner:
                    if (!frontier.getOwner().isEmpty()) {
                        String owner = getOwnerString();
                        int ownerWidth = (mc.font.width(owner) + 3) * textScale;
                        hudWidth = Math.max(hudWidth, ownerWidth);
                        hudHeight += 12 * textScale;
                    }
                    break;
                case Banner:
                    hudWidth = Math.max(hudWidth, 22 * bannerScale + 4);
                    hudHeight += 40 * bannerScale + 4;
                    break;
                case None:
                    break;
            }
        }

        Config.Point anchorPos = Config.getHUDAnchor(Config.hudAnchor);
        Config.Point originPos = Config.getHUDOrigin(Config.hudAnchor, hudWidth, hudHeight);
        posX = anchorPos.x - originPos.x + Config.hudXPosition;
        posY = anchorPos.y - originPos.y + Config.hudYPosition;

        int offsetY = 0;
        nameOffsetY = 0;
        ownerOffsetY = 0;
        bannerOffsetY = 0;

        for (Config.HUDSlot slot : slots) {
            switch (slot) {
                case Name:
                    nameOffsetY = offsetY;

                    frontierName1.setX(posX + hudWidth / 2);
                    frontierName1.setY(posY + nameOffsetY + 2 * textScale);
                    frontierName1.setScale(textScale);
                    frontierName1.setText(Component.literal(frontier.getName1()));

                    frontierName2.setX(posX + hudWidth / 2);
                    frontierName2.setY(posY + nameOffsetY + 14 * textScale);
                    frontierName2.setScale(textScale);
                    frontierName2.setText(Component.literal(frontier.getName2()));

                    offsetY += 24 * textScale;
                    break;
                case Owner:
                    if (!frontier.getOwner().isEmpty()) {
                        String owner = getOwnerString();
                        ownerOffsetY = offsetY;

                        frontierOwner.setX(posX + hudWidth / 2);
                        frontierOwner.setY(posY + ownerOffsetY + 2);
                        frontierOwner.setScale(textScale);
                        frontierOwner.setText(Component.literal(ChatFormatting.ITALIC + owner));

                        offsetY += 12 * textScale;
                    }
                    break;
                case Banner:
                    bannerOffsetY = offsetY;
                    offsetY += 40 * bannerScale + 4;
                    break;
                case None:
                    break;
            }
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

    private void addSlot(Config.HUDSlot slot) {
        if (slot == Config.HUDSlot.Name) {
            if (frontier.isNamed()) {
                slots.add(slot);
            }
        } else if (slot == Config.HUDSlot.Owner) {
            if (!frontier.getOwner().isEmpty()) {
                slots.add(slot);
            }
        } else if (slot == Config.HUDSlot.Banner) {
            if (frontier.hasBanner()) {
                slots.add(slot);
            }
        }
    }
}
