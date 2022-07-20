package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.ui.minimap.MiniMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiHUDWidget extends AbstractWidget {
    private final GuiHUD guiHUD;
    private final MiniMap minimap;
    private ConfigData.Point positionHUD = new ConfigData.Point();
    private final ConfigData.Point grabOffset = new ConfigData.Point();
    private boolean grabbed = false;
    private final Consumer<GuiHUDWidget> callbackHUDUpdated;

    public GuiHUDWidget(GuiHUD guiHUD, MiniMap minimap, Consumer<GuiHUDWidget> callbackHUDUpdated) {
        super(0, 0, 0, 0, Component.empty());
        this.guiHUD = guiHUD;
        this.minimap = minimap;
        this.callbackHUDUpdated = callbackHUDUpdated;
    }

    public void setPositionHUD(ConfigData.Point positionHUD) {
        this.positionHUD = positionHUD;
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        int factor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int xScaled = (int) mouseX * factor;
        int yScaled = (int) mouseY * factor;

        return this.active && this.visible && guiHUD.isInside(xScaled, yScaled);
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

            ConfigData.Point anchorPoint = ConfigData.getHUDAnchor(ConfigData.hudAnchor);
            ConfigData.Point originPoint = ConfigData.getHUDOrigin(ConfigData.hudAnchor, guiHUD.getWidth(), guiHUD.getHeight());

            if (ConfigData.hudAutoAdjustAnchor) {
                ConfigData.HUDAnchor closestAnchor = null;
                int closestDistance = 99999;

                for (ConfigData.HUDAnchor anchor : ConfigData.HUDAnchor.values()) {
                    if ((anchor == ConfigData.HUDAnchor.Minimap || anchor == ConfigData.HUDAnchor.MinimapHorizontal
                            || anchor == ConfigData.HUDAnchor.MinimapVertical) && minimap == null) {
                        continue;
                    }

                    ConfigData.Point anchorP = ConfigData.getHUDAnchor(anchor);
                    ConfigData.Point originP = ConfigData.getHUDOrigin(anchor, guiHUD.getWidth(), guiHUD.getHeight());

                    int distance = Math.abs(anchorP.x - positionHUD.x - originP.x)
                            + Math.abs(anchorP.y - positionHUD.y - originP.y);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestAnchor = anchor;
                    }
                }
                if (closestAnchor != null && closestAnchor != ConfigData.hudAnchor) {
                    ConfigData.hudAnchor = closestAnchor;
                    anchorPoint = ConfigData.getHUDAnchor(ConfigData.hudAnchor);
                    originPoint = ConfigData.getHUDOrigin(ConfigData.hudAnchor, guiHUD.getWidth(), guiHUD.getHeight());
                    callbackHUDUpdated.accept(this);
                }
            }

            ConfigData.Point snapOffset = new ConfigData.Point();
            if (ConfigData.hudSnapToBorder) {
                snapOffset.x = 16;
                snapOffset.y = 16;
                for (ConfigData.HUDAnchor anchor : ConfigData.HUDAnchor.values()) {
                    if (anchor == ConfigData.HUDAnchor.MinimapHorizontal || anchor == ConfigData.HUDAnchor.MinimapVertical) {
                        continue;
                    }

                    ConfigData.Point anchorP = ConfigData.getHUDAnchor(anchor);
                    ConfigData.Point originP = ConfigData.getHUDOrigin(anchor, guiHUD.getWidth(), guiHUD.getHeight());
                    int offsetX = positionHUD.x - anchorP.x + originP.x;
                    int offsetY = positionHUD.y - anchorP.y + originP.y;

                    if (anchor == ConfigData.HUDAnchor.Minimap) {
                        if (minimap == null) {
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

            ConfigData.hudXPosition = positionHUD.x - anchorPoint.x + originPoint.x - snapOffset.x;
            ConfigData.hudYPosition = positionHUD.y - anchorPoint.y + originPoint.y - snapOffset.y;

            guiHUD.configUpdated();
            callbackHUDUpdated.accept(this);
        }
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        guiHUD.draw(matrixStack, partialTicks);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_)
    {

    }
}
