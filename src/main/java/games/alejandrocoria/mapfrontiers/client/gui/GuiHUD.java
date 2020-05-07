package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.io.ThemeLoader;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
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
    private List<ConfigData.HUDSlot> slots;
    private int posX = 0;
    private int posY = 0;
    private int nameOffsetY = 0;
    private int bannerOffsetY = 0;
    private int hudWidth = 0;
    private int hudHeight = 0;
    private int bannerScale = 1;
    private boolean initialized = false;

    public GuiHUD(FrontiersOverlayManager frontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        slots = new ArrayList<ConfigData.HUDSlot>();
        frontierName1 = new GuiSimpleLabel(mc.fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, "", 0xffffffff);
        frontierName2 = new GuiSimpleLabel(mc.fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, "", 0xffffffff);
    }

    @SubscribeEvent()
    public void RenderGameOverlayEvent(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT) {
            if (mc.currentScreen != null) {
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
                        updateData();
                    }
                } else if (frontier != null) {
                    frontier = null;
                    frontierHash = 0;
                    updateData();
                }
            }
        }

    }

    public void configUpdated() {
        bannerScale = ConfigData.hud.bannerSize;
        updateData();
    }

    public void draw() {
        if (!initialized) {
            initialized = true;
            configUpdated();
        }

        if (slots.isEmpty()) {
            return;
        }

        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int factor = scaledresolution.getScaleFactor();

        int frameColor = 0xff000000;
        int textColor = 0xffffffff;

        if (Journeymap.getClient().getActiveMiniMapProperties().shape.get() == Shape.Circle) {
            frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
            textColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.foreground);
        } else {
            frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
            textColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.foreground);
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0 / factor, 1.0 / factor, 1.0);

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                drawName(frameColor, textColor);
                break;
            case Owner:
                break;
            case Banner:
                drawBanner(frameColor);
                break;
            case None:
                break;
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawName(int frameColor, int textColor) {
        Gui.drawRect(posX, posY + nameOffsetY, posX + hudWidth, posY + nameOffsetY + 24, frameColor);

        frontierName1.setColor(textColor);
        frontierName2.setColor(textColor);

        frontierName1.drawLabel(mc, 0, 0);
        frontierName2.drawLabel(mc, 0, 0);
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

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                int name1Width = mc.fontRenderer.getStringWidth(frontier.getName1()) + 4;
                int name2Width = mc.fontRenderer.getStringWidth(frontier.getName2()) + 4;
                int nameWidth = Math.max(name1Width, name2Width);
                hudWidth = Math.max(hudWidth, nameWidth);
                hudHeight += 24;
                break;
            case Owner:
                break;
            case Banner:
                hudWidth = Math.max(hudWidth, 22 * bannerScale + 4);
                hudHeight += 40 * bannerScale + 4;
                break;
            case None:
                break;
            }
        }

        ConfigData.Point anchorPos = ConfigData.getHUDAnchor();
        ConfigData.Point originPos = ConfigData.getHUDOrigin(hudWidth, hudHeight);
        posX = anchorPos.x - originPos.x + ConfigData.hud.position.x;
        posY = anchorPos.y - originPos.y + ConfigData.hud.position.y;

        int offsetY = 0;
        nameOffsetY = 0;
        bannerOffsetY = 0;

        for (ConfigData.HUDSlot slot : slots) {
            switch (slot) {
            case Name:
                nameOffsetY = offsetY;

                frontierName1.setX(posX + hudWidth / 2);
                frontierName1.setY(posY + nameOffsetY + 2);
                frontierName1.setText(frontier.getName1());

                frontierName2.setX(posX + hudWidth / 2);
                frontierName2.setY(posY + nameOffsetY + 14);
                frontierName2.setText(frontier.getName2());

                offsetY += 24;
                break;
            case Owner:
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