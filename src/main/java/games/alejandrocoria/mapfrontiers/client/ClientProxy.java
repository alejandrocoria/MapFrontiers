package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    private static IClientAPI jmAPI;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static SettingsProfile settingsProfile;
    private static GuiFrontierSettings.Tab lastSettingsTab = GuiFrontierSettings.Tab.Credits;

    private static KeyBinding openSettingsKey;
    private static GuiHUD guiHUD;

    private static BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private static Set<FrontierOverlay> insideFrontiers = new HashSet<>();

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
        MinecraftForge.EVENT_BUS.register(FrontiersOverlayManager.class);

        openSettingsKey = new KeyBinding("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");
        ClientRegistry.registerKeyBinding(openSettingsKey);

        MapFrontiers.LOGGER.info("clientSetup done");
    }

    @SubscribeEvent
    public static void onEvent(KeyInputEvent event) {
        if (openSettingsKey.matches(event.getKey(), event.getScanCode()) && openSettingsKey.isDown()) {
            if (frontiersOverlayManager == null) {
                return;
            }

            if (settingsProfile == null) {
                return;
            }

            Minecraft.getInstance().setScreen(new GuiFrontierSettings());
        }
    }

    @SubscribeEvent
    public static void livingUpdateEvent(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() == Minecraft.getInstance().player) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.getEntityLiving();

            BlockPos currentPlayerPosition = player.blockPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX() || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                Set<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersForAnnounce(player.level.dimension(), lastPlayerPosition);
                frontiers.addAll(frontiersOverlayManager.getFrontiersForAnnounce(player.level.dimension(), lastPlayerPosition));

                for (Iterator<FrontierOverlay> i = insideFrontiers.iterator(); i.hasNext();) {
                    FrontierOverlay inside = i.next();
                    if (frontiers.stream().noneMatch(f -> f.getId().equals(inside.getId()))) {
                        player.displayClientMessage(createChatTextWithName("mapfrontiers.chat.leaving", inside), false);
                        i.remove();
                    }
                }

                for (FrontierOverlay frontier : frontiers) {
                    if (insideFrontiers.add(frontier)) {
                        player.displayClientMessage(createChatTextWithName("mapfrontiers.chat.entering", frontier), false);
                    }
                }
            }
        }
    }

    private static ITextComponent createChatTextWithName(String key, FrontierOverlay frontier) {
        if (StringUtils.isBlank(frontier.getName1()) && StringUtils.isBlank(frontier.getName2())) {
            TextComponent text = new TranslationTextComponent("mapfrontiers.unnamed", TextFormatting.ITALIC);
            text.withStyle(style -> style.withItalic(true).withColor(Color.fromRgb(GuiColors.SETTINGS_TEXT_MEDIUM)));
            return new TranslationTextComponent(key, text);
        }

        String name = frontier.getName1().trim();
        String name2 = frontier.getName2().trim();
        if (!StringUtils.isBlank(name2)) {
            if (!name.isEmpty()) {
                name += " ";
            }
            name += name2;
        }

        TextComponent text = new StringTextComponent(name);
        text.withStyle(style -> style.withColor(Color.fromRgb(frontier.getColor())));

        return new TranslationTextComponent(key, text);
    }

    public static BlockPos snapVertex(BlockPos vertex, float snapDistance, RegistryKey<World> dimension, @Nullable FrontierOverlay owner) {
        BlockPos closest = BlockPosHelper.atY(vertex,70);
        double closestDistance = snapDistance * snapDistance;

        for (FrontierOverlay frontier : personalFrontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        for (FrontierOverlay frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        return closest;
    }

    public static void setjmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.updateAllOverlays(false);
                personalFrontiersOverlayManager.updateAllOverlays(false);
            }
        }
    }

    @SubscribeEvent
    public static void clientConnectedToServer(LoggedInEvent event) {
        if (jmAPI != null) {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.removeAllOverlays();
                personalFrontiersOverlayManager.removeAllOverlays();
            }
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI, false);
            personalFrontiersOverlayManager = new FrontiersOverlayManager(jmAPI, true);

            guiHUD = new GuiHUD(frontiersOverlayManager, personalFrontiersOverlayManager);
            MinecraftForge.EVENT_BUS.register(guiHUD);
        }
    }

    @SubscribeEvent
    public static void clientDisconnectionFromServer(LoggedOutEvent event) {
        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.removeAllOverlays();
            frontiersOverlayManager = null;
            personalFrontiersOverlayManager.removeAllOverlays();
            personalFrontiersOverlayManager = null;
        }

        if (guiHUD != null) {
            MinecraftForge.EVENT_BUS.unregister(guiHUD);
            guiHUD = null;
        }
    }

    public static ItemStack getHeldBanner() {
        ItemStack mainhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlotType.MAINHAND);
        ItemStack offhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlotType.OFFHAND);
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof BannerItem) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof BannerItem) {
            heldBanner = offhand;
        }

        return heldBanner;
    }

    public static FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        if (personal) {
            return personalFrontiersOverlayManager;
        } else {
            return frontiersOverlayManager;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        settingsProfile = event.profile;
    }

    public static SettingsProfile getSettingsProfile() {
        return settingsProfile;
    }

    public static void setLastSettingsTab(GuiFrontierSettings.Tab tab) {
        lastSettingsTab = tab;
    }

    public static GuiFrontierSettings.Tab getLastSettingsTab() {
        return lastSettingsTab;
    }

    public static void configUpdated() {
        ConfigData.save();

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.updateAllOverlays(true);
            personalFrontiersOverlayManager.updateAllOverlays(true);
        }

        if (guiHUD != null) {
            guiHUD.configUpdated(Minecraft.getInstance().getWindow());
        }
    }
}
