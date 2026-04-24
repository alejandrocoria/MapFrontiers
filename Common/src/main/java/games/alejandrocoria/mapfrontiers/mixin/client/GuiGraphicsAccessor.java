package games.alejandrocoria.mapfrontiers.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccessor {
    @Invoker("innerBlit")
    void innerBlitInvoker(RenderPipeline pipeline, Identifier Identifier, int x1, int x2, int y1, int y2, float minU, float maxU, float minV, float maxV, int color);

    @Accessor("guiRenderState")
    GuiRenderState mapfrontiers$getGuiRenderState();
}
