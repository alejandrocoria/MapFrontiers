package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
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
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(openBookSoundEvent, 1.0F));
    }

    public static void playSoundTurnPage() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(turnPageSoundEvent, 1.0F));
    }

    private Sounds() {
    }
}
