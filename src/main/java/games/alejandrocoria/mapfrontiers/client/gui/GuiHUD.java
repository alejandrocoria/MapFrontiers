package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import journeymap.client.JourneymapClient;
import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.io.ThemeLoader;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.ThemeLabelSource;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
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
    private ThemeLabelSource minimapInfo1;
    private ThemeLabelSource minimapInfo2;
    private ThemeLabelSource minimapInfo3;
    private ThemeLabelSource minimapInfo4;
    private int minimapFontScale;
    private int minimapCompassFontScale;

    public static GuiHUD asPreview() {
        GuiHUD guiHUD = new GuiHUD();

        ItemStack itemBanner = new ItemStack(Items.BLACK_BANNER);
        CompoundNBT entityTag = itemBanner.getOrCreateTagElement("BlockEntityTag");
        ListNBT patterns = (new BannerPattern.Builder()).addPattern(BannerPattern.FLOWER, DyeColor.GREEN)
                .addPattern(BannerPattern.BRICKS, DyeColor.LIGHT_GRAY)
                .addPattern(BannerPattern.BORDER, DyeColor.LIGHT_BLUE)
                .addPattern(BannerPattern.TRIANGLE_TOP, DyeColor.LIGHT_BLUE)
                .addPattern(BannerPattern.TRIANGLE_BOTTOM, DyeColor.BLACK)
                .addPattern(BannerPattern.STRIPE_BOTTOM, DyeColor.GREEN).toListTag();
        entityTag.put("Patterns", patterns);

        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(new SettingsUser(mc.player));
        frontierData.setName1("Preview Frontier");
        frontierData.setName2("-----------------");
        frontierData.setBanner(itemBanner);

        guiHUD.frontier = new FrontierOverlay(frontierData, null);

        return guiHUD;
    }

    public GuiHUD(FrontiersOverlayManager frontiersOverlayManager, FrontiersOverlayManager personalFrontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.personalFrontiersOverlayManager = personalFrontiersOverlayManager;
        slots = new ArrayList<>();
        frontierName1 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
        frontierName2 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
        frontierOwner = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
    }

    public GuiHUD() {
        this.frontiersOverlayManager = null;
        this.personalFrontiersOverlayManager = null;
        previewMode = true;
        slots = new ArrayList<>();
        frontierName1 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
        frontierName2 = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
        frontierOwner = new GuiSimpleLabel(mc.font, 0, 0, GuiSimpleLabel.Align.Center, StringTextComponent.EMPTY,
                GuiColors.WHITE);
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

    @SubscribeEvent
    public void RenderGameOverlayEvent(RenderGameOverlayEvent.Pre event) {
        if (previewMode) {
            return;
        }

        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
            if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
                return;
            }

            if (frontier == null) {
                return;
            }

            if (!ConfigData.hudEnabled) {
                return;
            }

            draw(event.getMatrixStack(), event.getWindow(), event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void livingUpdateEvent(LivingUpdateEvent event) {
        if (previewMode || frontiersOverlayManager == null || personalFrontiersOverlayManager == null) {
            return;
        }

        if (event.getEntityLiving() == mc.player) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();

            BlockPos currentPlayerPosition = player.blockPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX()
                    || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                FrontierOverlay newFrontier = personalFrontiersOverlayManager
                        .getFrontierInPosition(player.level.dimension(), lastPlayerPosition);
                if (newFrontier == null) {
                    newFrontier = frontiersOverlayManager.getFrontierInPosition(player.level.dimension(),
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
    }

    public void configUpdated(MainWindow mainWindow) {
        if (previewMode) {
            updateData(mainWindow);
        } else {
            needUpdate = true;
        }
    }

    public void frontierChanged() {
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

    public void draw(MatrixStack matrixStack, MainWindow mainWindow, float partialTicks) {
        if (displayWidth != mainWindow.getWidth() || displayHeight != mainWindow.getHeight()) {
            needUpdate = true;
        }

        if (ConfigData.hudAnchor == ConfigData.HUDAnchor.Minimap || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            if (minimapEnabled != minimapProperties.enabled.get() || minimapSize != minimapProperties.sizePercent.get()
                    || minimapShape != minimapProperties.shape.get() || minimapPosition != minimapProperties.position.get()
                    || minimapInfo1 != minimapProperties.info1Label.get() || minimapInfo2 != minimapProperties.info1Label.get()
                    || minimapInfo3 != minimapProperties.info1Label.get() || minimapInfo4 != minimapProperties.info1Label.get()
                    || minimapFontScale != minimapProperties.fontScale.get().intValue()
                    || minimapCompassFontScale != minimapProperties.compassFontScale.get().intValue()) {
                needUpdate = true;
            }
        }

        if (needUpdate) {
            needUpdate = false;
            updateData(mainWindow);
        }

        if (slots.isEmpty()) {
            return;
        }

        float factor = (float) mainWindow.getGuiScale();

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
                drawBanner(matrixStack, frameColor, partialTicks);
                break;
            case None:
                break;
            }
        }

        matrixStack.popPose();
        GlStateManager._enableBlend();
    }

    private void drawName(MatrixStack matrixStack, int frameColor, int textColor, float partialTicks) {
        AbstractGui.fill(matrixStack, posX, posY + nameOffsetY, posX + hudWidth, posY + nameOffsetY + 24 * textScale, frameColor);

        frontierName1.setColor(textColor);
        frontierName2.setColor(textColor);

        frontierName1.render(matrixStack, 0, 0, partialTicks);
        frontierName2.render(matrixStack, 0, 0, partialTicks);
    }

    private void drawOwner(MatrixStack matrixStack, int frameColor, int textColor, float partialTicks) {
        AbstractGui.fill(matrixStack, posX, posY + ownerOffsetY, posX + hudWidth, posY + ownerOffsetY + 12 * textScale,
                frameColor);

        frontierOwner.setColor(textColor);
        frontierOwner.render(matrixStack, 0, 0, partialTicks);
    }

    private void drawBanner(MatrixStack matrixStack, int frameColor, float partialTicks) {
        AbstractGui.fill(matrixStack, posX + hudWidth / 2 - 11 * bannerScale - 2, posY + bannerOffsetY,
                posX + hudWidth / 2 + 11 * bannerScale + 2, posY + bannerOffsetY + 4 + 40 * bannerScale, frameColor);

        frontier.renderBanner(mc, matrixStack, posX + hudWidth / 2 - 11 * bannerScale, posY + bannerOffsetY + 2, bannerScale);
    }

    private void updateData(MainWindow mainWindow) {
        displayWidth = mainWindow.getWidth();
        displayHeight = mainWindow.getHeight();

        if (ConfigData.hudAnchor == ConfigData.HUDAnchor.Minimap || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hudAnchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            minimapEnabled = minimapProperties.enabled.get();
            minimapSize = minimapProperties.sizePercent.get();
            minimapShape = minimapProperties.shape.get();
            minimapPosition = minimapProperties.position.get();
            minimapInfo1 = minimapProperties.info1Label.get();
            minimapInfo2 = minimapProperties.info1Label.get();
            minimapInfo3 = minimapProperties.info1Label.get();
            minimapInfo4 = minimapProperties.info1Label.get();
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

                frontierName1.x = posX + hudWidth / 2;
                frontierName1.y = posY + nameOffsetY + 2 * textScale;
                frontierName1.setScale(textScale);
                frontierName1.setText(new StringTextComponent(frontier.getName1()));

                frontierName2.x = posX + hudWidth / 2;
                frontierName2.y = posY + nameOffsetY + 14 * textScale;
                frontierName2.setScale(textScale);
                frontierName2.setText(new StringTextComponent(frontier.getName2()));

                offsetY += 24 * textScale;
                break;
            case Owner:
                if (!frontier.getOwner().isEmpty()) {
                    String owner = getOwnerString();
                    ownerOffsetY = offsetY;

                    frontierOwner.x = posX + hudWidth / 2;
                    frontierOwner.y = posY + ownerOffsetY + 2;
                    frontierOwner.setScale(textScale);
                    frontierOwner.setText(new StringTextComponent(owner));

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
            if (!StringUtils.isBlank(frontier.getName1()) || !StringUtils.isBlank(frontier.getName2())) {
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
