package games.alejandrocoria.mapfrontiers.client.gui;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
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
            int width = scaledresolution.getScaledWidth();
            int height = scaledresolution.getScaledHeight();
            int factor = scaledresolution.getScaleFactor();

            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0 / factor, 1.0 / factor, 1.0);

            int posX = mc.displayWidth - 250 - 11 * factor;
            String name1 = frontier.getName1();
            String name2 = frontier.getName2();
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
                int name1Width = mc.fontRenderer.getStringWidth(name1) + 4;
                int name2Width = mc.fontRenderer.getStringWidth(name2) + 4;
                int nameWidth = Math.max(22 * bannerScale + 4, Math.max(name1Width, name2Width));

                Gui.drawRect(posX - nameWidth / 2, 8, posX + nameWidth / 2, 32, frameColor);

                frontierName1.setX(posX);
                frontierName1.setY(10);
                frontierName1.setText(name1);
                frontierName1.setColor(textColor);

                frontierName2.setX(posX);
                frontierName2.setY(22);
                frontierName2.setText(name2);
                frontierName2.setColor(textColor);

                frontierName1.drawLabel(mc, 0, 0);
                frontierName2.drawLabel(mc, 0, 0);
            }

            if (frontier.hasBanner()) {
                Gui.drawRect(posX - 11 * bannerScale - 2, 32, posX + 11 * bannerScale + 2, 36 + 40 * bannerScale, frameColor);

                frontier.bindBannerTexture(mc);
                GlStateManager.color(1.f, 1.f, 1.f);
                Gui.drawModalRectWithCustomSizedTexture(posX - 11 * bannerScale, 34, 0, bannerScale, 22 * bannerScale,
                        40 * bannerScale, 64 * bannerScale, 64 * bannerScale);
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

    private static int getIntColor(Theme.ColorSpec colorSpec) {
        int color = colorSpec.getColor();
        color |= Math.round(colorSpec.alpha * 255) << 24;

        return color;
    }
}