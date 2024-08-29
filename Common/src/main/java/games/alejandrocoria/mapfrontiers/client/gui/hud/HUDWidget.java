package games.alejandrocoria.mapfrontiers.client.gui.hud;

import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.common.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class HUDWidget extends AbstractWidgetNoNarration {
    private final HUD hud;
    private final boolean minimapEnabled;
    private Config.Point positionHUD = new Config.Point();
    private final Config.Point grabOffset = new Config.Point();
    private boolean grabbed = false;
    private final Consumer<HUDWidget> callbackHUDUpdated;

    public HUDWidget(HUD hud, boolean minimapEnabled, Consumer<HUDWidget> callbackHUDUpdated) {
        super(0, 0, 0, 0, Component.empty());
        this.hud = hud;
        this.minimapEnabled = minimapEnabled;
        this.callbackHUDUpdated = callbackHUDUpdated;
    }

    public void setPositionHUD(Config.Point positionHUD) {
        this.positionHUD = positionHUD;
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        int factor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int xScaled = (int) mouseX * factor;
        int yScaled = (int) mouseY * factor;

        return this.active && this.visible && hud.isInside(xScaled, yScaled);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int factor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int xScaled = (int) mouseX * factor;
        int yScaled = (int) mouseY * factor;
        grabOffset.x = xScaled - positionHUD.x;
        grabOffset.y = yScaled - positionHUD.y;
        grabbed = true;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        grabbed = false;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (grabbed) {
            Minecraft mc = Minecraft.getInstance();
            float factor = (float) mc.getWindow().getGuiScale();
            mouseX *= factor;
            mouseY *= factor;

            positionHUD.x = (int) mouseX - grabOffset.x;
            positionHUD.y = (int) mouseY - grabOffset.y;

            Config.Point anchorPoint = Config.getHUDAnchor(Config.hudAnchor);
            Config.Point originPoint = Config.getHUDOrigin(Config.hudAnchor, hud.getWidth(), hud.getHeight());

            if (Config.hudAutoAdjustAnchor) {
                Config.HUDAnchor closestAnchor = null;
                int closestDistance = 99999;

                for (Config.HUDAnchor anchor : Config.HUDAnchor.values()) {
                    if ((anchor == Config.HUDAnchor.Minimap || anchor == Config.HUDAnchor.MinimapHorizontal
                            || anchor == Config.HUDAnchor.MinimapVertical) && !minimapEnabled) {
                        continue;
                    }

                    Config.Point anchorP = Config.getHUDAnchor(anchor);
                    Config.Point originP = Config.getHUDOrigin(anchor, hud.getWidth(), hud.getHeight());

                    int distance = Math.abs(anchorP.x - positionHUD.x - originP.x)
                            + Math.abs(anchorP.y - positionHUD.y - originP.y);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestAnchor = anchor;
                    }
                }
                if (closestAnchor != null && closestAnchor != Config.hudAnchor) {
                    Config.hudAnchor = closestAnchor;
                    anchorPoint = Config.getHUDAnchor(Config.hudAnchor);
                    originPoint = Config.getHUDOrigin(Config.hudAnchor, hud.getWidth(), hud.getHeight());
                    callbackHUDUpdated.accept(this);
                }
            }

            Config.Point snapOffset = new Config.Point();
            if (Config.hudSnapToBorder) {
                snapOffset.x = 16;
                snapOffset.y = 16;
                for (Config.HUDAnchor anchor : Config.HUDAnchor.values()) {
                    if (anchor == Config.HUDAnchor.MinimapHorizontal || anchor == Config.HUDAnchor.MinimapVertical) {
                        continue;
                    }

                    Config.Point anchorP = Config.getHUDAnchor(anchor);
                    Config.Point originP = Config.getHUDOrigin(anchor, hud.getWidth(), hud.getHeight());
                    int offsetX = positionHUD.x - anchorP.x + originP.x;
                    int offsetY = positionHUD.y - anchorP.y + originP.y;

                    if (anchor == Config.HUDAnchor.Minimap) {
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

            Config.hudXPosition = positionHUD.x - anchorPoint.x + originPoint.x - snapOffset.x;
            Config.hudYPosition = positionHUD.y - anchorPoint.y + originPoint.y - snapOffset.y;

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
    }
}
