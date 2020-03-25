package games.alejandrocoria.mapfrontiers.client.plugin;

import java.util.HashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
@SideOnly(Side.CLIENT)
public class MapFrontiersPlugin implements IClientPlugin {
    public static MapFrontiersPlugin instance;
    private IClientAPI jmAPI;

    private final ResourceLocation openBookSoundRes = new ResourceLocation(MapFrontiers.MODID, "open_book");
    private final SoundEvent openBookSoundEvent = new SoundEvent(openBookSoundRes);

    private final ResourceLocation turnPageSoundRes = new ResourceLocation(MapFrontiers.MODID, "turn_page");
    private final SoundEvent turnPageSoundEvent = new SoundEvent(turnPageSoundRes);

    private HashMap<Integer, Integer> frontiersSelected;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        instance = this;

        this.jmAPI = jmAPI;
        ((ClientProxy) MapFrontiers.proxy).jmAPI = jmAPI;
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(openBookSoundEvent);
        event.getRegistry().register(turnPageSoundEvent);
    }

    @SideOnly(Side.CLIENT)
    public void playSoundOpenBook() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(openBookSoundEvent, 1.0F));
    }

    @SideOnly(Side.CLIENT)
    public void playSoundTurnPage() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(turnPageSoundEvent, 1.0F));
    }
}