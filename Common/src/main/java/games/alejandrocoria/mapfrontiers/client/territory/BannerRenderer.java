package games.alejandrocoria.mapfrontiers.client.territory;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.mixin.client.CubeInvoker;
import games.alejandrocoria.mapfrontiers.mixin.client.GuiGraphicsAccessor;
import games.alejandrocoria.mapfrontiers.mixin.client.SpriteContentsInvoker;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BannerRenderer {
    private static final AtomicInteger NEXT_TEXTURE_INSTANCE_ID = new AtomicInteger();

    private final int textureInstanceId = NEXT_TEXTURE_INSTANCE_ID.incrementAndGet();
    private Identifier textureLocation;
    private int rotation;

    public void createTexture(UUID id, BannerData bannerData) {
        releaseTexture();

        rotation = bannerData.rotation;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        ListTag patterns = bannerData.patterns;
        BannerPatternLayers patternLayers = BannerPatternLayers.EMPTY;
        if (patterns != null) {
            if (level.registryAccess().lookup(Registries.BANNER_PATTERN).isEmpty()) {
                MapFrontiers.LOGGER.error("Banner pattern registry is unavailable while creating a banner texture.");
                return;
            }

            Optional<BannerPatternLayers> bannerPatterns = BannerPatternLayers.CODEC.parse(
                    level.registryAccess().createSerializationContext(NbtOps.INSTANCE), patterns).result();
            if (bannerPatterns.isPresent()) {
                patternLayers = bannerPatterns.get();
            } else {
                MapFrontiers.LOGGER.error("Failed to parse normalized banner patterns while creating a banner texture. patterns={}", patterns);
                return;
            }
        }

        ModelPart bannerModelPart = mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG).getChild("flag");
        float[] flagUV = {0, 0, 0, 0};
        bannerModelPart.visit(new PoseStack(), (pose, path, i, cube) -> {
            for (ModelPart.Polygon polygon : ((CubeInvoker) cube).mapfrontiers$getPolygon()) {
                if (polygon.normal().z() < 0) {
                    flagUV[0] = polygon.vertices()[0].u();
                    flagUV[1] = polygon.vertices()[0].v();
                    flagUV[2] = polygon.vertices()[2].u();
                    flagUV[3] = polygon.vertices()[2].v();
                }
            }
        });

        if (flagUV[0] == flagUV[2] || flagUV[1] == flagUV[3]) {
            MapFrontiers.LOGGER.error("Failed to resolve banner flag UVs while creating a banner texture.");
            return;
        }

        TextureAtlasSprite base = mc.getAtlasManager().get(Sheets.BANNER_BASE);
        SpriteContents baseSprite = base.contents();
        int width = (int) (Math.abs(flagUV[0] - flagUV[2]) * baseSprite.width());
        int height = (int) (Math.abs(flagUV[1] - flagUV[3]) * baseSprite.height());
        NativeImage tempBannerImage = new NativeImage(width, height, false);
        try {
            generateBannerLayer(tempBannerImage, flagUV, baseSprite, bannerData.baseColor);

            for (int i = 0; i < patternLayers.layers().size(); ++i) {
                BannerPatternLayers.Layer layer = patternLayers.layers().get(i);
                Identifier patternTextureLocation = layer.pattern().value().assetId().withPrefix("entity/banner/");
                TextureAtlasSprite sprite = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BANNER_PATTERNS).getSprite(patternTextureLocation);

                generateBannerLayer(tempBannerImage, flagUV, sprite.contents(), layer.color());
            }

            textureLocation = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "banner/" + id + "/" + textureInstanceId);
            DynamicTexture texture = new DynamicTexture(() -> textureLocation.toString(), tempBannerImage.mappedCopy(ARGB::opaque));
            mc.getTextureManager().register(textureLocation, texture);
        } finally {
            tempBannerImage.close();
        }
    }

    private static void generateBannerLayer(NativeImage bannerImage, float[] flagUV, SpriteContents sprite, DyeColor dye) {
        NativeImage spriteImage = ((SpriteContentsInvoker) sprite).mapfrontiers$getOriginalImage();
        for (int y = 0; y < bannerImage.getHeight(); ++y) {
            for (int x = 0; x < bannerImage.getWidth(); ++x) {
                int u = (int) (Mth.lerp((x + 0.5f) / bannerImage.getWidth(), flagUV[2], flagUV[0]) * sprite.width());
                int v = (int) (Mth.lerp((y + 0.5f) / bannerImage.getHeight(), flagUV[1], flagUV[3]) * sprite.height());
                int color = ARGB.multiply(spriteImage.getPixel(u, v), dye.getTextureDiffuseColor());
                blendPixel(bannerImage, x, y, color);
            }
        }
    }

    private static void blendPixel(NativeImage image, int x, int y, int color) {
        int i = image.getPixel(x, y);
        float f = (float) ARGB.alpha(color) / 255.0F;
        float f1 = (float) ARGB.red(color) / 255.0F;
        float f2 = (float) ARGB.green(color) / 255.0F;
        float f3 = (float) ARGB.blue(color) / 255.0F;
        float f4 = (float) ARGB.alpha(i) / 255.0F;
        float f5 = (float) ARGB.red(i) / 255.0F;
        float f6 = (float) ARGB.green(i) / 255.0F;
        float f7 = (float) ARGB.blue(i) / 255.0F;
        float f8 = 1.0F - f;
        float f9 = f * f + f4 * f8;
        float f10 = f1 * f + f5 * f8;
        float f11 = f2 * f + f6 * f8;
        float f12 = f3 * f + f7 * f8;
        if (f9 > 1.0F) {
            f9 = 1.0F;
        }
        if (f10 > 1.0F) {
            f10 = 1.0F;
        }
        if (f11 > 1.0F) {
            f11 = 1.0F;
        }
        if (f12 > 1.0F) {
            f12 = 1.0F;
        }

        int j = (int) (f9 * 255.0F);
        int k = (int) (f10 * 255.0F);
        int l = (int) (f11 * 255.0F);
        int i1 = (int) (f12 * 255.0F);
        image.setPixel(x, y, ARGB.color(j, k, l, i1));
    }

    public void renderBanner(GuiGraphicsExtractor graphics, int centerX, int y, int scale) {
        if (textureLocation == null) {
            return;
        }

        int width = 20 * scale;
        int height = 40 * scale;
        int x = centerX - width / 2;
        float centerY = y + height / 2f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().rotate((float) Math.toRadians(rotation));
        graphics.pose().translate(-centerX, -centerY);

        ((GuiGraphicsAccessor) graphics).innerBlitInvoker(RenderPipelines.GUI_TEXTURED, textureLocation, x, x + width, y, y + height, 0, 1, 0, 1, ColorConstants.TEXTURE_TINT_NONE);

        graphics.pose().popMatrix();
    }

    public boolean hasBanner() {
        return textureLocation != null;
    }

    public @Nullable MapImage createJourneyMapImage(double anchorX, double anchorY, int displayWidth, int displayHeight, float opacity) {
        if (textureLocation == null) {
            return null;
        }

        MapImage bannerIcon = new MapImage(textureLocation, 0, 0, 20, 40, ColorConstants.TEXTURE_TINT_NONE, opacity);
        bannerIcon.setBlur(false);
        bannerIcon.setAnchorX(anchorX);
        bannerIcon.setAnchorY(anchorY);
        bannerIcon.setDisplayWidth(displayWidth);
        bannerIcon.setDisplayHeight(displayHeight);
        bannerIcon.setRotation(-rotation);
        return bannerIcon;
    }

    public void releaseTexture() {
        if (textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
            textureLocation = null;
        }
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }
}
