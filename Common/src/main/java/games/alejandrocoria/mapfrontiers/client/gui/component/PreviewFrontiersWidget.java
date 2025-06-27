package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.mixin.GuiGraphicsAccessor;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderType;
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
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PreviewFrontiersWidget extends AbstractWidgetNoNarration {
    private static final ResourceLocation backgroundTexture = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_preview_bg.png");
    private static final int SIZE = 420;

    private final IJourneyMapHelper.ICustomPreviewRenderer customPreviewRenderer;
    private final List<FrontierOverlay> previewFrontiers = new ArrayList<>();
    private float scaleFactor = 1;

    public PreviewFrontiersWidget() {
        super(0, 0, SIZE, SIZE, Component.empty());

        HolderLookup<BannerPattern> patternRegistry = Minecraft.getInstance().level.registryAccess().lookup(Registries.BANNER_PATTERN).get();
        BannerPatternLayers patterns = (new BannerPatternLayers.Builder())
                .add(patternRegistry.get(BannerPatterns.FLOWER).get(), DyeColor.GREEN)
                .add(patternRegistry.get(BannerPatterns.BRICKS).get(), DyeColor.LIGHT_GRAY)
                .add(patternRegistry.get(BannerPatterns.BORDER).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_TOP).get(), DyeColor.LIGHT_BLUE)
                .add(patternRegistry.get(BannerPatterns.TRIANGLE_BOTTOM).get(), DyeColor.BLACK)
                .add(patternRegistry.get(BannerPatterns.STRIPE_BOTTOM).get(), DyeColor.GREEN).build();

        SettingsUser owner = new SettingsUser();
        owner.username = "Player";
        FrontierData frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1("Preview");
        frontierData.setName2("Frontier");
        frontierData.setColor(0xFFAACC60);
        frontierData.setBanner(DyeColor.BLACK, patterns);
        frontierData.setDimension(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld")));
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, true);
        frontierData.addVertex(new BlockPos(10, 70, 10));
        frontierData.addVertex(new BlockPos(10, 70, 410));
        frontierData.addVertex(new BlockPos(270, 70, 410));
        frontierData.addVertex(new BlockPos(270, 70, 10));
        previewFrontiers.add(new FrontierOverlay(frontierData, null));

        frontierData = new FrontierData();
        frontierData.setOwner(owner);
        frontierData.setName1("Long name");
        frontierData.setName2("12345678901234567");
        frontierData.setColor(0xFFA0A0FF);
        frontierData.setDimension(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld")));
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenDay, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenName, true);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenOwner, false);
        frontierData.setVisibility(FrontierData.VisibilityData.Visibility.FullscreenBanner, false);
        frontierData.addVertex(new BlockPos(240, 70, 280));
        frontierData.addVertex(new BlockPos(300, 70, 245));
        frontierData.addVertex(new BlockPos(360, 70, 280));
        frontierData.addVertex(new BlockPos(360, 70, 350));
        frontierData.addVertex(new BlockPos(300, 70, 385));
        frontierData.addVertex(new BlockPos(240, 70, 350));
        previewFrontiers.add(new FrontierOverlay(frontierData, null));

        customPreviewRenderer = Services.JOURNEYMAP.createCustomPreviewRenderer();
        configUpdated();
    }

    public void configUpdated() {
        for (FrontierOverlay frontierOverlay : previewFrontiers) {
            frontierOverlay.recalculateOverlays();
        }

        customPreviewRenderer.setFrontiers(previewFrontiers);
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale() / scaleFactor;
        setWidth((int) (SIZE / guiScale));
        setHeight((int) (SIZE / guiScale));

        customPreviewRenderer.setFrontiers(previewFrontiers);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(RenderType::guiTextured, backgroundTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), SIZE, SIZE, SIZE, SIZE);
        graphics.hLine(getX(), getX() + getWidth() - 1, getY(), ColorConstants.OPTION_BORDER);
        graphics.hLine(getX(), getX() + getWidth() - 1, getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.vLine(getX(), getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.vLine(getX() + getWidth() - 1, getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);

        customPreviewRenderer.draw(graphics, ((GuiGraphicsAccessor) graphics).getBufferSource(), getX(), getY(), SIZE, scaleFactor);
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }
}
