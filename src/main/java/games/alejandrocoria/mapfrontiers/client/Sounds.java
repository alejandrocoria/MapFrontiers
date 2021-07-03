package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class Sounds {
    private static final ResourceLocation openBookSoundRes = new ResourceLocation(MapFrontiers.MODID, "open_book");
    private static final SoundEvent openBookSoundEvent = new SoundEvent(openBookSoundRes);

    private static final ResourceLocation turnPageSoundRes = new ResourceLocation(MapFrontiers.MODID, "turn_page");
    private static final SoundEvent turnPageSoundEvent = new SoundEvent(turnPageSoundRes);

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(openBookSoundEvent);
        event.getRegistry().register(turnPageSoundEvent);
    }

    public static void playSoundOpenBook() {
        Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(openBookSoundEvent, 1.0F));
    }

    public static void playSoundTurnPage() {
        Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(turnPageSoundEvent, 1.0F));
    }

    private Sounds() {
    }
}
