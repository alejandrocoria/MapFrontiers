package games.alejandrocoria.mapfrontiers.client.gui;

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
public class GuiInGameRenderer {
    private static Minecraft mc = Minecraft.getMinecraft();

    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private GuiSimpleLabel frontierName1;
    private GuiSimpleLabel frontierName2;
    private int bannerScale = 3;

    public GuiInGameRenderer(FrontiersOverlayManager frontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        frontierName1 = new GuiSimpleLabel(mc.fontRenderer, 31, 16, GuiSimpleLabel.Align.Center, "", 0xffffffff);
        frontierName2 = new GuiSimpleLabel(mc.fontRenderer, 31, 28, GuiSimpleLabel.Align.Center, "", 0xffffffff);
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

            ScaledResolution scaledresolution = new ScaledResolution(mc);
            int factor = scaledresolution.getScaleFactor();

            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0 / factor, 1.0 / factor, 1.0);

            String name1 = frontier.getName1();
            String name2 = frontier.getName2();

            int hudWidth = 0;
            int hudHeight = 0;

            if (!StringUtils.isBlank(name1) || !StringUtils.isBlank(name2)) {
                int name1Width = mc.fontRenderer.getStringWidth(name1) + 4;
                int name2Width = mc.fontRenderer.getStringWidth(name2) + 4;
                hudWidth = Math.max(name1Width, name2Width);
                hudHeight += 24;
            }

            if (frontier.hasBanner()) {
                hudWidth = Math.max(hudWidth, 22 * bannerScale + 4);
                hudHeight += 40 * bannerScale + 4;
            }


            int posX = getXAnchorFromConfig() - getXOriginFromConfig(hudWidth);
            int posY = getYAnchorFromConfig() - getYOriginFromConfig(hudHeight);

            int frameColor = 0xff000000;
            int textColor = 0xffffffff;

            if (Journeymap.getClient().getActiveMiniMapProperties().shape.get() == Shape.Circle) {
                frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
                textColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.foreground);
            } else {
                frameColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
                textColor = getIntColor(ThemeLoader.getCurrentTheme().minimap.square.labelTop.foreground);
            }

            if (!StringUtils.isBlank(name1) || !StringUtils.isBlank(name2)) {
                Gui.drawRect(posX, posY, posX + hudWidth, posY + 24, frameColor);

                frontierName1.setX(posX + hudWidth / 2);
                frontierName1.setY(posY + 2);
                frontierName1.setText(name1);
                frontierName1.setColor(textColor);

                frontierName2.setX(posX + hudWidth / 2);
                frontierName2.setY(posY + 14);
                frontierName2.setText(name2);
                frontierName2.setColor(textColor);

                frontierName1.drawLabel(mc, 0, 0);
                frontierName2.drawLabel(mc, 0, 0);
            }

            if (frontier.hasBanner()) {
                Gui.drawRect(posX + hudWidth / 2 - 11 * bannerScale - 2, posY + 24, posX + hudWidth / 2 + 11 * bannerScale + 2,
                        posY + 28 + 40 * bannerScale, frameColor);

                frontier.bindBannerTexture(mc);
                GlStateManager.color(1.f, 1.f, 1.f);
                Gui.drawModalRectWithCustomSizedTexture(posX + hudWidth / 2 - 11 * bannerScale, posY + 26, 0, bannerScale,
                        22 * bannerScale, 40 * bannerScale, 64 * bannerScale, 64 * bannerScale);
            }

            GlStateManager.popMatrix();
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

                frontier = frontiersOverlayManager.getFrontierInPosition(player.dimension, lastPlayerPosition);
            }
        }
    }

    private int getXAnchorFromConfig() {
        switch (ConfigData.hud.anchor) {
        case ScreenTop:
        case ScreenBottom:
            return mc.displayWidth / 2;
        case ScreenTopRight:
        case ScreenRight:
        case ScreenBottomRight:
            return mc.displayWidth;
        case ScreenBottomLeft:
        case ScreenLeft:
        case ScreenTopLeft:
            return 0;
        case Minimap:
        case MinimapHorizontal:
            // @Incomplete
            return mc.displayWidth - 250;
        case MinimapVertical:
            return mc.displayWidth;
        }

        return 0;
    }

    private int getYAnchorFromConfig() {
        switch (ConfigData.hud.anchor) {
        case ScreenTopLeft:
        case ScreenTop:
        case ScreenTopRight:
            return 0;
        case ScreenBottomLeft:
        case ScreenBottom:
        case ScreenBottomRight:
            return mc.displayHeight;
        case ScreenRight:
        case ScreenLeft:
            return mc.displayHeight / 2;
        case Minimap:
        case MinimapVertical:
            // @Incomplete
            return 250;
        case MinimapHorizontal:
            return 0;
        }

        return 0;
    }

    private int getXOriginFromConfig(int hudWidth) {
        switch (ConfigData.hud.anchor) {
        case ScreenTop:
        case ScreenBottom:
            return hudWidth / 2;
        case ScreenTopRight:
        case ScreenRight:
        case ScreenBottomRight:
            return hudWidth;
        case ScreenBottomLeft:
        case ScreenLeft:
        case ScreenTopLeft:
            return 0;
        case Minimap:
        case MinimapHorizontal:
        case MinimapVertical:
            // @Incomplete
            return hudWidth;
        }

        return 0;
    }

    private int getYOriginFromConfig(int hudHeight) {
        switch (ConfigData.hud.anchor) {
        case ScreenTopLeft:
        case ScreenTop:
        case ScreenTopRight:
            return 0;
        case ScreenBottomLeft:
        case ScreenBottom:
        case ScreenBottomRight:
            return hudHeight;
        case ScreenRight:
        case ScreenLeft:
            return hudHeight / 2;
        case Minimap:
        case MinimapHorizontal:
        case MinimapVertical:
            // @Incomplete
            return 0;
        }

        return 0;
    }

    private static int getIntColor(Theme.ColorSpec colorSpec) {
        int color = colorSpec.getColor();
        color |= Math.round(colorSpec.alpha * 255) << 24;

        return color;
    }
}