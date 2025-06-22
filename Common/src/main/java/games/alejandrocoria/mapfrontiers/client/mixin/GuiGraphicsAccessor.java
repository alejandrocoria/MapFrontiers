package games.alejandrocoria.mapfrontiers.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor
{
    @Invoker("innerBlit")
    void innerBlitInvoker(RenderPipeline pipeline, ResourceLocation resourceLocation, int x1, int x2, int y1, int y2, float minU, float maxU, float minV, float maxV, int color);
}
