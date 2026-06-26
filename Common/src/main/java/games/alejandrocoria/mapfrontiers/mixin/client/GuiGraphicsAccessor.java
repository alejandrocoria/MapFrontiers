package games.alejandrocoria.mapfrontiers.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker("innerBlit")
    void innerBlitInvoker(RenderPipeline pipeline, ResourceLocation ResourceLocation, int x1, int x2, int y1, int y2, float minU, float maxU, float minV, float maxV, int color);

    @Accessor("guiRenderState")
    GuiRenderState mapfrontiers$getGuiRenderState();
}
