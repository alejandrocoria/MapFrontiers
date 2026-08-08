package games.alejandrocoria.mapfrontiers.client.territory;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.mixin.client.CubeInvoker;
import games.alejandrocoria.mapfrontiers.mixin.client.SpriteContentsInvoker;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
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
    private ResourceLocation textureLocation;
    private int rotation;
    private long textureRevision;

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

        ModelPart bannerModelPart = mc.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
        float[] flagUV = {0, 0, 0, 0};
        bannerModelPart.visit(new PoseStack(), (pose, path, i, cube) -> {
            for (ModelPart.Polygon polygon : ((CubeInvoker) cube).mapfrontiers$getPolygon()) {
                if (polygon.normal.z() < 0) {
                    flagUV[0] = polygon.vertices[0].u;
                    flagUV[1] = polygon.vertices[0].v;
                    flagUV[2] = polygon.vertices[2].u;
                    flagUV[3] = polygon.vertices[2].v;
                }
            }
        });

        if (flagUV[0] == flagUV[2] || flagUV[1] == flagUV[3]) {
            MapFrontiers.LOGGER.error("Failed to resolve banner flag UVs while creating a banner texture.");
            return;
        }

        TextureAtlasSprite base = Sheets.BANNER_BASE.sprite();
        SpriteContents baseSprite = base.contents();
        int width = (int) (Math.abs(flagUV[0] - flagUV[2]) * baseSprite.width());
        int height = (int) (Math.abs(flagUV[1] - flagUV[3]) * baseSprite.height());
        NativeImage bannerImage = new NativeImage(width, height, false);
        DynamicTexture texture = null;
        try {
            generateBannerLayer(bannerImage, flagUV, baseSprite, bannerData.baseColor);

            for (int i = 0; i < patternLayers.layers().size(); ++i) {
                BannerPatternLayers.Layer layer = patternLayers.layers().get(i);
                ResourceLocation patternTextureLocation = layer.pattern().value().assetId().withPrefix("entity/banner/");
                TextureAtlasSprite sprite = mc.getTextureAtlas(Sheets.BANNER_SHEET).apply(patternTextureLocation);

                generateBannerLayer(bannerImage, flagUV, sprite.contents(), layer.color());
            }

            bannerImage.applyToAllPixels(color -> color | 0xFF000000);

            ResourceLocation newTextureLocation = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "banner/" + id + "/" + textureInstanceId);
            texture = new DynamicTexture(bannerImage);
            bannerImage = null;
            mc.getTextureManager().register(newTextureLocation, texture);
            textureLocation = newTextureLocation;
            texture = null;
            textureRevision++;
        } finally {
            if (texture != null) {
                texture.close();
            }
            if (bannerImage != null) {
                bannerImage.close();
            }
        }
    }

    private static void generateBannerLayer(NativeImage bannerImage, float[] flagUV, SpriteContents sprite, DyeColor dye) {
        NativeImage spriteImage = ((SpriteContentsInvoker) sprite).mapfrontiers$getOriginalImage();
        for (int y = 0; y < bannerImage.getHeight(); ++y) {
            for (int x = 0; x < bannerImage.getWidth(); ++x) {
                int u = (int) (Mth.lerp((x + 0.5f) / bannerImage.getWidth(), flagUV[2], flagUV[0]) * sprite.width());
                int v = (int) (Mth.lerp((y + 0.5f) / bannerImage.getHeight(), flagUV[1], flagUV[3]) * sprite.height());
                int color = FastColor.ARGB32.multiply(spriteImage.getPixelRGBA(u, v), dye.getTextureDiffuseColor());
                bannerImage.blendPixel(x, y, FastColor.ABGR32.fromArgb32(color));
            }
        }
    }

    public void renderBanner(GuiGraphics graphics, int centerX, int y, int scale) {
        if (textureLocation == null) {
            return;
        }

        int width = 20 * scale;
        int height = 40 * scale;
        int x = centerX - width / 2;
        float centerY = y + height / 2f;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        graphics.pose().translate(-centerX, -centerY, 0.0f);
        graphics.blit(textureLocation, x, y, 0, 0, width, height, width, height);
        graphics.pose().popPose();
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
            textureRevision++;
        }
    }

    public void setRotation(int rotation) {
        if (this.rotation == rotation) {
            return;
        }
        this.rotation = rotation;
        textureRevision++;
    }

    public int getRotation() {
        return rotation;
    }

    public long getTextureRevision() {
        return textureRevision;
    }
}
