package games.alejandrocoria.mapfrontiers.mixin.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
    @Accessor
    boolean getCanChangeValue();

    @Accessor
    void setCanChangeValue(boolean canChangeValue);
}
