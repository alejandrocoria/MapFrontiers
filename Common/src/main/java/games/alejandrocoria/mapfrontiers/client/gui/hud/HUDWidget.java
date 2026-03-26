package games.alejandrocoria.mapfrontiers.client.gui.hud;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class HUDWidget extends AbstractWidgetNoNarration {
    private final HUD hud;
    private final boolean minimapEnabled;
    private HUDPlacementHelper.Point positionHUD = new HUDPlacementHelper.Point();
    private final HUDPlacementHelper.Point grabOffset = new HUDPlacementHelper.Point();
    private boolean grabbed = false;
    private final Consumer<HUDWidget> callbackHUDUpdated;

    public HUDWidget(HUD hud, boolean minimapEnabled, Consumer<HUDWidget> callbackHUDUpdated) {
        super(0, 0, 0, 0, Component.empty());
        this.hud = hud;
        this.minimapEnabled = minimapEnabled;
        this.callbackHUDUpdated = callbackHUDUpdated;
    }

    public void setPositionHUD(HUDPlacementHelper.Point positionHUD) {
        this.positionHUD = positionHUD;
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int factor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int xScaled = (int) mouseX * factor;
        int yScaled = (int) mouseY * factor;

        return this.active && this.visible && hud.isInside(xScaled, yScaled);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int factor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int xScaled = (int) event.x() * factor;
        int yScaled = (int) event.y() * factor;
        grabOffset.x = xScaled - positionHUD.x;
        grabOffset.y = yScaled - positionHUD.y;
        grabbed = true;

        return true;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        grabbed = false;
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (grabbed) {
            Minecraft mc = Minecraft.getInstance();
            float factor = (float) mc.getWindow().getGuiScale();
            double mouseX = event.x() * factor;
            double mouseY = event.y() * factor;

            positionHUD.x = (int) mouseX - grabOffset.x;
            positionHUD.y = (int) mouseY - grabOffset.y;

            HUDPlacementHelper.Point anchorPoint = HUDPlacementHelper.getHUDAnchor(ClientConfig.HUD_ANCHOR.get());
            HUDPlacementHelper.Point originPoint = HUDPlacementHelper.getHUDOrigin(ClientConfig.HUD_ANCHOR.get(), hud.getWidth(), hud.getHeight());

            if (ClientConfig.HUD_AUTO_ADJUST_ANCHOR.get()) {
                ClientConfig.HUDAnchor closestAnchor = null;
                int closestDistance = 99999;

                for (ClientConfig.HUDAnchor anchor : ClientConfig.HUDAnchor.values()) {
                    if ((anchor == ClientConfig.HUDAnchor.Minimap || anchor == ClientConfig.HUDAnchor.MinimapHorizontal
                            || anchor == ClientConfig.HUDAnchor.MinimapVertical) && !minimapEnabled) {
                        continue;
                    }

                    HUDPlacementHelper.Point anchorP = HUDPlacementHelper.getHUDAnchor(anchor);
                    HUDPlacementHelper.Point originP = HUDPlacementHelper.getHUDOrigin(anchor, hud.getWidth(), hud.getHeight());

                    int distance = Math.abs(anchorP.x - positionHUD.x - originP.x)
                            + Math.abs(anchorP.y - positionHUD.y - originP.y);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestAnchor = anchor;
                    }
                }
                if (closestAnchor != null && closestAnchor != ClientConfig.HUD_ANCHOR.get()) {
                    ClientConfig.HUD_ANCHOR.set(closestAnchor);
                    anchorPoint = HUDPlacementHelper.getHUDAnchor(ClientConfig.HUD_ANCHOR.get());
                    originPoint = HUDPlacementHelper.getHUDOrigin(ClientConfig.HUD_ANCHOR.get(), hud.getWidth(), hud.getHeight());
                    callbackHUDUpdated.accept(this);
                }
            }

            HUDPlacementHelper.Point snapOffset = new HUDPlacementHelper.Point();
            if (ClientConfig.HUD_SNAP_TO_BORDER.get()) {
                snapOffset.x = 16;
                snapOffset.y = 16;
                for (ClientConfig.HUDAnchor anchor : ClientConfig.HUDAnchor.values()) {
                    if (anchor == ClientConfig.HUDAnchor.MinimapHorizontal || anchor == ClientConfig.HUDAnchor.MinimapVertical) {
                        continue;
                    }

                    HUDPlacementHelper.Point anchorP = HUDPlacementHelper.getHUDAnchor(anchor);
                    HUDPlacementHelper.Point originP = HUDPlacementHelper.getHUDOrigin(anchor, hud.getWidth(), hud.getHeight());
                    int offsetX = positionHUD.x - anchorP.x + originP.x;
                    int offsetY = positionHUD.y - anchorP.y + originP.y;

                    if (anchor == ClientConfig.HUDAnchor.Minimap) {
                        if (!minimapEnabled) {
                            continue;
                        }

                        int displayWidth = mc.getWindow().getWidth();
                        int displayHeight = mc.getWindow().getHeight();

                        if (anchorP.x < displayWidth / 2 && offsetX >= 16) {
                            continue;
                        } else if (anchorP.x > displayWidth / 2 && offsetX <= -16) {
                            continue;
                        }

                        if (anchorP.y < displayHeight / 2 && offsetY >= 16) {
                            continue;
                        } else if (anchorP.y > displayHeight / 2 && offsetY <= -16) {
                            continue;
                        }
                    }

                    if (Math.abs(offsetX) < Math.abs(snapOffset.x)) {
                        snapOffset.x = offsetX;
                    }
                    if (Math.abs(offsetY) < Math.abs(snapOffset.y)) {
                        snapOffset.y = offsetY;
                    }
                }

                if (snapOffset.x == 16) {
                    snapOffset.x = 0;
                }
                if (snapOffset.y == 16) {
                    snapOffset.y = 0;
                }
            }

            ClientConfig.HUD_X_POSITION.set(positionHUD.x - anchorPoint.x + originPoint.x - snapOffset.x);
            ClientConfig.HUD_Y_POSITION.set(positionHUD.y - anchorPoint.y + originPoint.y - snapOffset.y);

            // We don't fire the config update event on mouse drag because it would write the config to file every frame.
            hud.configUpdated();
            callbackHUDUpdated.accept(this);
        }
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        hud.draw(graphics, partialTicks);
        if (!grabbed) {
            positionHUD.x = hud.getPosX();
            positionHUD.y = hud.getPosY();
        }
    }
}
