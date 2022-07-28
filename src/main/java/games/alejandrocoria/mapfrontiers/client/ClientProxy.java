package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

    private static KeyMapping openSettingsKey;
    private static GuiHUD guiHUD;

    private static BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private static Set<FrontierOverlay> insideFrontiers = new HashSet<>();

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
        MinecraftForge.EVENT_BUS.register(FrontiersOverlayManager.class);

        openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");

        MapFrontiers.LOGGER.info("clientSetup done");
    }

    @SubscribeEvent
    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(openSettingsKey);
    }

    @SubscribeEvent
    public static void onKeyEvent(InputEvent.Key event) {
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
    public static void livingUpdateEvent(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            Player player = (Player) event.getEntity();

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

    private static MutableComponent createChatTextWithName(String key, FrontierOverlay frontier) {
        if (StringUtils.isBlank(frontier.getName1()) && StringUtils.isBlank(frontier.getName2())) {
            MutableComponent text = Component.translatable("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            text.withStyle(style -> style.withItalic(true).withColor(GuiColors.SETTINGS_TEXT_MEDIUM));
            return Component.translatable(key, text);
        }

        String name = frontier.getName1().trim();
        String name2 = frontier.getName2().trim();
        if (!StringUtils.isBlank(name2)) {
            if (!name.isEmpty()) {
                name += " ";
            }
            name += name2;
        }

        MutableComponent text = Component.literal(name);
        text.withStyle(style -> style.withColor(frontier.getColor()));

        return Component.translatable(key, text);
    }

    public static BlockPos snapVertex(BlockPos vertex, float snapDistance, ResourceKey<Level> dimension, @Nullable FrontierOverlay owner) {
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
    public static void clientConnectedToServer(LoggingIn event) {
        initializeManagers();

        guiHUD = new GuiHUD(frontiersOverlayManager, personalFrontiersOverlayManager);
        MinecraftForge.EVENT_BUS.register(guiHUD);
    }

    @SubscribeEvent
    public static void clientDisconnectionFromServer(LoggingOut event) {
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

    private static void initializeManagers() {
        if (jmAPI == null) {
            return;
        }

        if (frontiersOverlayManager == null) {
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI, false);
        }

        if (personalFrontiersOverlayManager == null) {
            personalFrontiersOverlayManager = new FrontiersOverlayManager(jmAPI, true);
        }
    }

    public static ItemStack getHeldBanner() {
        ItemStack mainhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof BannerItem) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof BannerItem) {
            heldBanner = offhand;
        }

        return heldBanner;
    }

    public static FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        initializeManagers();

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
