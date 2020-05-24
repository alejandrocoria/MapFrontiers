package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
import journeymap.client.ui.theme.Theme;
import journeymap.client.ui.theme.ThemeLabelSource;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBanner;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiHUD {
    private static Minecraft mc = Minecraft.getMinecraft();

    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private int frontierHash;
    private BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private GuiSimpleLabel frontierName1;
    private GuiSimpleLabel frontierName2;
    private GuiSimpleLabel frontierOwner;
    private List<ConfigData.HUDSlot> slots;
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
        GuiHUD guiHUD = new GuiHUD(null);
        guiHUD.previewMode = true;

        NBTTagCompound pattern = new NBTTagCompound();
        NBTTagList nbtBanner = new NBTTagList();

        pattern.setInteger("Color", EnumDyeColor.GREEN.getDyeDamage());
        pattern.setString("Pattern", "flo");
        nbtBanner.appendTag(pattern.copy());
        pattern.setInteger("Color", EnumDyeColor.SILVER.getDyeDamage());
        pattern.setString("Pattern", "bri");
        nbtBanner.appendTag(pattern.copy());
        pattern.setInteger("Color", EnumDyeColor.LIGHT_BLUE.getDyeDamage());
        pattern.setString("Pattern", "bo");
        nbtBanner.appendTag(pattern.copy());
        pattern.setInteger("Color", EnumDyeColor.LIGHT_BLUE.getDyeDamage());
        pattern.setString("Pattern", "tts");
        nbtBanner.appendTag(pattern.copy());
        pattern.setInteger("Color", EnumDyeColor.BLACK.getDyeDamage());
        pattern.setString("Pattern", "bt");
        nbtBanner.appendTag(pattern.copy());
        pattern.setInteger("Color", EnumDyeColor.GREEN.getDyeDamage());
        pattern.setString("Pattern", "bs");
        nbtBanner.appendTag(pattern);

        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(new SettingsUser(mc.player));
        frontierData.setName1("Preview Frontier");
        frontierData.setName2("-----------------");
        frontierData.setBanner(ItemBanner.makeBanner(EnumDyeColor.BLACK, nbtBanner));

        guiHUD.frontier = new FrontierOverlay(frontierData, null);

        return guiHUD;
    }

    public GuiHUD(FrontiersOverlayManager frontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        slots = new ArrayList<ConfigData.HUDSlot>();
        frontierName1 = new GuiSimpleLabel(mc.fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, "", 0xffffffff);
        frontierName2 = new GuiSimpleLabel(mc.fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, "", 0xffffffff);
        frontierOwner = new GuiSimpleLabel(mc.fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, "", 0xffffffff);
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

    @SubscribeEvent()
    public void RenderGameOverlayEvent(RenderGameOverlayEvent.Pre event) {
        if (previewMode) {
            return;
        }

        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
            if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)) {
                return;
            }

            if (frontier == null) {
                return;
            }

            if (!ConfigData.hud.enabled) {
                return;
            }

            draw();
        }
    }

    @SubscribeEvent
    public void livingUpdateEvent(LivingUpdateEvent event) {
        if (previewMode) {
            return;
        }

        if (event.getEntityLiving() == mc.player) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            BlockPos currentPlayerPosition = player.getPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX()
                    || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                FrontierOverlay newFrontier = frontiersOverlayManager.getFrontierInPosition(player.dimension, lastPlayerPosition);
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

    public void configUpdated() {
        if (previewMode) {
            updateData();
        } else {
            needUpdate = true;
        }
    }

    public void frontierChanged() {
        FrontierOverlay newFrontier = frontiersOverlayManager.getFrontierInPosition(mc.player.dimension, lastPlayerPosition);
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

    public void draw() {
        if (displayWidth != mc.displayWidth || displayHeight != mc.displayHeight) {
            needUpdate = true;
        }

        if (ConfigData.hud.anchor == ConfigData.HUDAnchor.Minimap
                || ConfigData.hud.anchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hud.anchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            if (minimapEnabled != minimapProperties.enabled.get() || minimapSize != minimapProperties.sizePercent.get().intValue()
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
            updateData();
        }

        if (slots.isEmpty()) {
            return;
        }

        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int factor = scaledresolution.getScaleFactor();

        int frameColor = 0xff000000;
        int textNameColor = 0xffffffff;
        int textOwnerColor = 0xffdfdfdf;

        if (Journeymap.getClient().getActiveMiniMapProperties().shape.get() == Shape.Circle) {
            frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
            textNameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.highlight);
            textOwnerColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.foreground);
        } else {
            frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
            textNameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.highlight);
            textOwnerColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.foreground);
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0 / factor, 1.0 / factor, 1.0);
        GlStateManager.translate(0.0, 0.0, -100.0);

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                drawName(frameColor, textNameColor);
                break;
            case Owner:
                drawOwner(frameColor, textOwnerColor);
                break;
            case Banner:
                drawBanner(frameColor);
                break;
            case None:
                break;
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }

    private void drawName(int frameColor, int textColor) {
        Gui.drawRect(posX, posY + nameOffsetY, posX + hudWidth, posY + nameOffsetY + 24 * textScale, frameColor);

        frontierName1.setColor(textColor);
        frontierName2.setColor(textColor);

        frontierName1.drawLabel(mc, 0, 0);
        frontierName2.drawLabel(mc, 0, 0);
    }

    private void drawOwner(int frameColor, int textColor) {
        Gui.drawRect(posX, posY + ownerOffsetY, posX + hudWidth, posY + ownerOffsetY + 12 * textScale, frameColor);

        frontierOwner.setColor(textColor);
        frontierOwner.drawLabel(mc, 0, 0);
    }

    private void drawBanner(int frameColor) {
        Gui.drawRect(posX + hudWidth / 2 - 11 * bannerScale - 2, posY + bannerOffsetY, posX + hudWidth / 2 + 11 * bannerScale + 2,
                posY + bannerOffsetY + 4 + 40 * bannerScale, frameColor);

        frontier.bindBannerTexture(mc);
        GlStateManager.color(1.f, 1.f, 1.f);
        Gui.drawModalRectWithCustomSizedTexture(posX + hudWidth / 2 - 11 * bannerScale, posY + bannerOffsetY + 2, 0, bannerScale,
                22 * bannerScale, 40 * bannerScale, 64 * bannerScale, 64 * bannerScale);
    }

    private static int getIntColor(Theme.ColorSpec colorSpec) {
        int color = colorSpec.getColor();
        color |= Math.round(colorSpec.alpha * 255) << 24;

        return color;
    }

    private void updateData() {
        displayWidth = mc.displayWidth;
        displayHeight = mc.displayHeight;

        if (ConfigData.hud.anchor == ConfigData.HUDAnchor.Minimap
                || ConfigData.hud.anchor == ConfigData.HUDAnchor.MinimapHorizontal
                || ConfigData.hud.anchor == ConfigData.HUDAnchor.MinimapVertical) {
            MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
            minimapEnabled = minimapProperties.enabled.get();
            minimapSize = minimapProperties.sizePercent.get().intValue();
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

        addSlot(ConfigData.hud.slot1);
        addSlot(ConfigData.hud.slot2);
        addSlot(ConfigData.hud.slot3);

        if (slots.isEmpty()) {
            return;
        }

        hudWidth = 0;
        hudHeight = 0;
        bannerScale = ConfigData.hud.bannerSize;

        textScale = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().fontScale.get().intValue();

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                int name1Width = mc.fontRenderer.getStringWidth(frontier.getName1()) + 3;
                int name2Width = mc.fontRenderer.getStringWidth(frontier.getName2()) + 3;
                int nameWidth = Math.max(name1Width, name2Width) * textScale;
                hudWidth = Math.max(hudWidth, nameWidth);
                hudHeight += 24 * textScale;
                break;
            case Owner:
                if (!frontier.getOwner().isEmpty()) {
                    String owner = getOwnerString();
                    int ownerWidth = (mc.fontRenderer.getStringWidth(owner) + 3) * textScale;
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

        ConfigData.Point anchorPos = ConfigData.getHUDAnchor(ConfigData.hud.anchor);
        ConfigData.Point originPos = ConfigData.getHUDOrigin(ConfigData.hud.anchor, hudWidth, hudHeight);
        posX = anchorPos.x - originPos.x + ConfigData.hud.position.x;
        posY = anchorPos.y - originPos.y + ConfigData.hud.position.y;

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
                frontierName1.setText(frontier.getName1());

                frontierName2.setX(posX + hudWidth / 2);
                frontierName2.setY(posY + nameOffsetY + 14 * textScale);
                frontierName2.setScale(textScale);
                frontierName2.setText(frontier.getName2());

                offsetY += 24 * textScale;
                break;
            case Owner:
                if (!frontier.getOwner().isEmpty()) {
                    String owner = getOwnerString();
                    ownerOffsetY = offsetY;

                    frontierOwner.setX(posX + hudWidth / 2);
                    frontierOwner.setY(posY + ownerOffsetY + 2);
                    frontierOwner.setScale(textScale);
                    frontierOwner.setText(owner);

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