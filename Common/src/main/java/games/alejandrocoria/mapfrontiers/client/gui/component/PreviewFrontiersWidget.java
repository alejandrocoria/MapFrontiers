package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapCustomPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PreviewFrontiersWidget extends AbstractWidgetNoNarration {
    private IJourneyMapCustomPreviewRenderer customPreviewRenderer;
    private FrontierOverlay previewFrontier;
    private float scaleFactor = 1.f;

    public PreviewFrontiersWidget() {
        super(0, 0, 300, 300, Component.empty());
        customPreviewRenderer = Services.JOURNEYMAP.createCustomPreviewRenderer();

        HolderLookup<BannerPattern> patternRegistry = Minecraft.getInstance().level.registryAccess().lookup(Registries.BANNER_PATTERN).get();
        BannerPatternLayers patterns = (new BannerPatternLayers.Builder())
                .add(patternRegistry.get(BannerPatterns.FLOWER).get(), DyeColor.GREEN)
                .add(patternRegistry.get(BannerPatterns.BRICKS).get(), DyeColor.LIGHT_GRAY)
                .add(patternRegistry.get(BannerPatterns.BORDER).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_TOP).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_BOTTOM).get(), DyeColor.BLACK)
                .add(patternRegistry.get(BannerPatterns.STRIPE_BOTTOM).get(), DyeColor.GREEN).build();

        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(new SettingsUser(Minecraft.getInstance().player));
        frontierData.setName1("Preview");
        frontierData.setName2("Frontier");
        frontierData.setColor(0xFFCCFF70);
        frontierData.setBanner(DyeColor.BLACK, patterns);
        frontierData.setDimension(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld")));
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, true);

        frontierData.addVertex(new BlockPos(0, 70, 0));
        frontierData.addVertex(new BlockPos(0, 70, 300));
        frontierData.addVertex(new BlockPos(300, 70, 300));
        frontierData.addVertex(new BlockPos(300, 70, 0));

        previewFrontier = new FrontierOverlay(frontierData, null);
        previewFrontier.recalculateOverlays();
    }

    public void configUpdated() {
        previewFrontier.recalculateOverlays();
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF202020);
        graphics.hLine(getX(), getX() + getWidth() - 1, getY(), ColorConstants.OPTION_BORDER);
        graphics.hLine(getX(), getX() + getWidth() - 1, getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.vLine(getX(), getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.vLine(getX() + getWidth() - 1, getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        customPreviewRenderer.draw(graphics, previewFrontier);
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }
}
