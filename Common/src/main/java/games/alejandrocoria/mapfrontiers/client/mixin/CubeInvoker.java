package games.alejandrocoria.mapfrontiers.client.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Cube.class)
public interface CubeInvoker {
    @Accessor("polygons")
    public ModelPart.Polygon[] mapfrontiers$getPolygon();
}
