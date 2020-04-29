package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiInGameRenderer {
    private static Minecraft mc = Minecraft.getMinecraft();

    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private GuiSimpleLabel frontierName1;
    private GuiSimpleLabel frontierName2;
    private int bannerSize = 2;

    public GuiInGameRenderer(FrontiersOverlayManager frontiersOverlayManager) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        frontierName1 = new GuiSimpleLabel(mc.fontRenderer, 31, 16, GuiSimpleLabel.Align.Center, "", 0xffffffff);
        frontierName2 = new GuiSimpleLabel(mc.fontRenderer, 31, 28, GuiSimpleLabel.Align.Center, "", 0xffffffff);
    }

    @SubscribeEvent()
    public void RenderGameOverlayEvent(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT) {
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

            frontierName1.setX(posX);
            frontierName1.setY(10);
            frontierName1.setText(frontier.getName1());

            frontierName2.setX(posX);
            frontierName2.setY(22);
            frontierName2.setText(frontier.getName2());

            frontierName1.drawLabel(mc, 0, 0);
            frontierName2.drawLabel(mc, 0, 0);

            if (!frontier.hasBanner()) {
                GlStateManager.popMatrix();
                return;
            }

            frontier.bindBannerTexture(mc);
            Gui.drawModalRectWithCustomSizedTexture(posX - 11 * bannerSize, 34, 0, 3, 22 * bannerSize, 40 * bannerSize,
                    64 * bannerSize, 64 * bannerSize);

            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void livingUpdateEvent(LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            BlockPos currentPlayerPosition = player.getPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX()
                    || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                frontier = frontiersOverlayManager.getFrontierInPosition(player.dimension, lastPlayerPosition);
            }
        }
    }
}