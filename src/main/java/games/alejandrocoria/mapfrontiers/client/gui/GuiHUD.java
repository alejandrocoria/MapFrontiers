package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.JourneymapClient;
import journeymap.client.io.ThemeLoader;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.Shape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiHUD {
    private static final Minecraft mc = Minecraft.getInstance();

    private final FrontiersOverlayManager frontiersOverlayManager;
    private final FrontiersOverlayManager personalFrontiersOverlayManager;
    private FrontierOverlay frontier;
    private int frontierHash;
    private BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private final GuiSimpleLabel frontierName1;
    private final GuiSimpleLabel frontierName2;
    private final GuiSimpleLabel frontierOwner;
    private final List<ConfigData.HUDSlot> slots;
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
    private boolean minimapEnabled;
    private int minimapSize;
    private Shape minimapShape;
    private Position minimapPosition;
    private String minimapInfo1;
    private String minimapInfo2;
    private String minimapInfo3;
    private String minimapInfo4;
    private int minimapFontScale;
    private int minimapCompassFontScale;

    public static GuiHUD asPreview() {
        GuiHUD guiHUD = new GuiHUD(null, null);
        guiHUD.previewMode = true;

        ItemStack itemBanner = new ItemStack(Items.BLACK_BANNER);
        CompoundTag entityTag = itemBanner.getOrCreateTagElement("BlockEntityTag");
        ListTag patterns = (new BannerPattern.Builder()).addPattern(BannerPatterns.FLOWER, DyeColor.GREEN)
                .addPattern(BannerPatterns.BRICKS, DyeColor.LIGHT_GRAY)
                .addPattern(BannerPatterns.BORDER, DyeColor.LIGHT_BLUE)
                .addPattern(BannerPatterns.TRIANGLE_TOP, DyeColor.LIGHT_BLUE)
                .addPattern(BannerPatterns.TRIANGLE_BOTTOM, DyeColor.BLACK)
                .addPattern(BannerPatterns.STRIPE_BOTTOM, DyeColor.GREEN).toListTag();
        entityTag.put("Patterns", patterns);

        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(new SettingsUser(mc.player));
        frontierData.setName1("Preview Frontier");
        frontierData.setName2("-----------------");
        frontierData.setBanner(itemBanner);

        guiHUD.frontier = new FrontierOverlay(frontierData, null);

        return guiHUD;
    }

    public GuiHUD(@Nullable FrontiersOverlayManager frontiersOverlayManager, @Nullable FrontiersOverlayManager personalFrontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.personalFrontiersOverlayManager = personalFrontiersOverlayManager;
        slots = new ArrayList<>();
        frontierName1 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, Component.empty(),
                GuiColors.WHITE);
        frontierName2 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, Component.empty(),
                GuiColors.WHITE);
        frontierOwner = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, Component.empty(),
                GuiColors.WHITE);

        ClientProxy.subscribeDeletedFrontierEvent(this, frontierID -> frontierChanged());
        ClientProxy.subscribeNewFrontierEvent(this, (frontierOverlay, playerID) -> frontierChanged());
        ClientProxy.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> frontierChanged());
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
                    .getFrontierInPosition(mc.player.level.dimension(), lastPlayerPosition);
            if (newFrontier == null) {
                newFrontier = frontiersOverlayManager.getFrontierInPosition(mc.player.level.dimension(),
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

        FrontierOverlay newFrontier = personalFrontiersOverlayManager.getFrontierInPosition(mc.player.level.dimension(),
                lastPlayerPosition);
        if (newFrontier == null) {
            newFrontier = frontiersOverlayManager.getFrontierInPosition(mc.player.level.dimension(), lastPlayerPosition);
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

    public void drawInGameHUD(PoseStack matrixStack, float partialTicks) {
        if (previewMode) {
            return;
        }

        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            return;
        }

        if (frontier == null) {
            return;
        }

        if (!ConfigData.hudEnabled) {
            return;
        }

        draw(matrixStack, partialTicks);
    }

    public void draw(PoseStack matrixStack, float partialTicks) {
        if (displayWidth != mc.getWindow().getWidth() || displayHeight != mc.getWindow().getHeight()) {
            needUpdate = true;
        }

        if (ConfigData.hudAnchor == ConfigData.HUDAnchor.Minimap || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            if (minimapEnabled != minimapProperties.enabled.get() || minimapSize != minimapProperties.sizePercent.get()
                    || minimapShape != minimapProperties.shape.get() || minimapPosition != minimapProperties.position.get()
                    || minimapInfo1.equals(minimapProperties.info1Label.get()) || minimapInfo2.equals(minimapProperties.info2Label.get())
                    || minimapInfo3.equals(minimapProperties.info3Label.get()) || minimapInfo4.equals(minimapProperties.info4Label.get())
                    || minimapFontScale != minimapProperties.fontScale.get().intValue()
                    || minimapCompassFontScale != minimapProperties.compassFontScale.get().intValue()) {
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

        int frameColor;
        int textNameColor;
        int textOwnerColor;

        if (JourneymapClient.getInstance().getActiveMiniMapProperties().shape.get() == Shape.Circle) {
            frameColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
            textNameColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.highlight);
            textOwnerColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.foreground);
        } else {
            frameColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
            textNameColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.highlight);
            textOwnerColor = GuiColors.colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.foreground);
        }

        matrixStack.pushPose();
        matrixStack.scale(1.0f / factor, 1.0f / factor, 1.0f);
        matrixStack.translate(0.0, 0.0, -100.0);

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                drawName(matrixStack, frameColor, textNameColor, partialTicks);
                break;
            case Owner:
                drawOwner(matrixStack, frameColor, textOwnerColor, partialTicks);
                break;
            case Banner:
                drawBanner(matrixStack, frameColor);
                break;
            case None:
                break;
            }
        }

        matrixStack.popPose();
        GlStateManager._enableBlend();
    }

    private void drawName(PoseStack matrixStack, int frameColor, int textColor, float partialTicks) {
        GuiComponent.fill(matrixStack, posX, posY + nameOffsetY, posX + hudWidth, posY + nameOffsetY + 24 * textScale, frameColor);

        frontierName1.setColor(textColor);
        frontierName2.setColor(textColor);

        frontierName1.render(matrixStack, 0, 0, partialTicks);
        frontierName2.render(matrixStack, 0, 0, partialTicks);
    }

    private void drawOwner(PoseStack matrixStack, int frameColor, int textColor, float partialTicks) {
        GuiComponent.fill(matrixStack, posX, posY + ownerOffsetY, posX + hudWidth, posY + ownerOffsetY + 12 * textScale,
                frameColor);

        frontierOwner.setColor(textColor);
        frontierOwner.render(matrixStack, 0, 0, partialTicks);
    }

    private void drawBanner(PoseStack matrixStack, int frameColor) {
        GuiComponent.fill(matrixStack, posX + hudWidth / 2 - 11 * bannerScale - 2, posY + bannerOffsetY,
                posX + hudWidth / 2 + 11 * bannerScale + 2, posY + bannerOffsetY + 4 + 40 * bannerScale, frameColor);

        frontier.renderBanner(mc, matrixStack, posX + hudWidth / 2 - 11 * bannerScale, posY + bannerOffsetY + 2, bannerScale);
    }

    private void updateData() {
        displayWidth = mc.getWindow().getWidth();
        displayHeight = mc.getWindow().getHeight();

        if (ConfigData.hudAnchor == ConfigData.HUDAnchor.Minimap || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            minimapEnabled = minimapProperties.enabled.get();
            minimapSize = minimapProperties.sizePercent.get();
            minimapShape = minimapProperties.shape.get();
            minimapPosition = minimapProperties.position.get();
            minimapInfo1 = minimapProperties.info1Label.get();
            minimapInfo2 = minimapProperties.info2Label.get();
            minimapInfo3 = minimapProperties.info3Label.get();
            minimapInfo4 = minimapProperties.info4Label.get();
            minimapFontScale = minimapProperties.fontScale.get().intValue();
            minimapCompassFontScale = minimapProperties.compassFontScale.get().intValue();
        }

        slots.clear();

        if (frontier == null) {
            return;
        }

        addSlot(ConfigData.hudSlot1);
        addSlot(ConfigData.hudSlot2);
        addSlot(ConfigData.hudSlot3);

        if (slots.isEmpty()) {
            return;
        }

        hudWidth = 0;
        hudHeight = 0;
        bannerScale = ConfigData.hudBannerSize;

        textScale = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().fontScale.get().intValue();

        for (ConfigData.HUDSlot slot : slots) {
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

        ConfigData.Point anchorPos = ConfigData.getHUDAnchor(ConfigData.hudAnchor);
        ConfigData.Point originPos = ConfigData.getHUDOrigin(ConfigData.hudAnchor, hudWidth, hudHeight);
        posX = anchorPos.x - originPos.x + ConfigData.hudXPosition;
        posY = anchorPos.y - originPos.y + ConfigData.hudYPosition;

        int offsetY = 0;
        nameOffsetY = 0;
        ownerOffsetY = 0;
        bannerOffsetY = 0;

        for (ConfigData.HUDSlot slot : slots) {
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

    private void addSlot(ConfigData.HUDSlot slot) {
        if (slot == ConfigData.HUDSlot.Name) {
            if (frontier.isNamed()) {
                slots.add(slot);
            }
        } else if (slot == ConfigData.HUDSlot.Owner) {
            if (!frontier.getOwner().isEmpty()) {
                slots.add(slot);
            }
        } else if (slot == ConfigData.HUDSlot.Banner) {
            if (frontier.hasBanner()) {
                slots.add(slot);
            }
        }
    }
}
