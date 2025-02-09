package games.alejandrocoria.mapfrontiers.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextureAtlas.class)
public interface TextureAtlasInvoker {
    @Invoker("getWidth")
    public int mapfrontiers$getWidth();
    @Invoker("getHeight")
    public int mapfrontiers$getHeight();
}
