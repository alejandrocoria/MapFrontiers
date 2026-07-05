package games.alejandrocoria.mapfrontiers.client.event;

import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.fullscreen.ThemeButtonDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ClientGlobalEvents {
    // Minecraft/Loader events
    private static final Map<Object, Consumer<Minecraft>> clientTickEventMap = new HashMap<>();
    private static final Map<Object, BiConsumer<Minecraft, Player>> playerTickEventMap = new HashMap<>();
    private static final Map<Object, BiConsumer<GuiGraphics, Float>> hudRenderEventMap = new HashMap<>();
    private static final Map<Object, Runnable> clientConnectedEventMap = new HashMap<>();
    private static final Map<Object, Runnable> clientDisconnectedEventMap = new HashMap<>();
    private static final Map<Object, Consumer<Integer>> mouseReleaseEventMap = new HashMap<>();

    // JourneyMap events
    private static final Map<Object, Consumer<ThemeButtonDisplay>> addonButtonDisplayEventMap = new HashMap<>();
    private static final Map<Object, Consumer<ModPopupMenu>> fullscreenPopupMenuEventMap = new HashMap<>();

    // Our events
    private static final Map<Object, Runnable> updatedConfigEventMap = new HashMap<>();

    // Minecraft/Loader events
    public static void subscribeClientTickEvent(Object object, Consumer<Minecraft> callback) {
        clientTickEventMap.put(object, callback);
    }

    public static void subscribePlayerTickEvent(Object object, BiConsumer<Minecraft, Player> callback) {
        playerTickEventMap.put(object, callback);
    }

    public static void subscribeHudRenderEvent(Object object, BiConsumer<GuiGraphics, Float> callback) {
        hudRenderEventMap.put(object, callback);
    }

    public static void subscribeClientConnectedEvent(Object object, Runnable callback) {
        clientConnectedEventMap.put(object, callback);
    }

    public static void subscribeClientDisconnectedEvent(Object object, Runnable callback) {
        clientDisconnectedEventMap.put(object, callback);
    }

    public static void subscribeMouseReleaseEvent(Object object, Consumer<Integer> callback) {
        mouseReleaseEventMap.put(object, callback);
    }


    // JourneyMap events
    public static void subscribeAddonButtonDisplayEvent(Object object, Consumer<ThemeButtonDisplay> callback) {
        addonButtonDisplayEventMap.put(object, callback);
    }

    public static void subscribeFullscreenPopupMenuEvent(Object object, Consumer<ModPopupMenu> callback) {
        fullscreenPopupMenuEventMap.put(object, callback);
    }

    public static void subscribeUpdatedConfigEvent(Object object, Runnable callback) {
        updatedConfigEventMap.put(object, callback);
    }


    public static void unsubscribeAllEvents(Object object) {
        // Minecraft/Loader events
        clientTickEventMap.remove(object);
        playerTickEventMap.remove(object);
        hudRenderEventMap.remove(object);
        clientConnectedEventMap.remove(object);
        clientDisconnectedEventMap.remove(object);
        mouseReleaseEventMap.remove(object);

        // JourneyMap events
        addonButtonDisplayEventMap.remove(object);
        fullscreenPopupMenuEventMap.remove(object);

        // Our events
        updatedConfigEventMap.remove(object);
    }

    // Minecraft/Loader events
    public static void postClientTickEvent(Minecraft client) {
        for (Consumer<Minecraft> callback : clientTickEventMap.values()) {
            callback.accept(client);
        }
    }

    public static void postPlayerTickEvent(Minecraft client, @Nullable Player player) {
        for (BiConsumer<Minecraft, Player> callback : playerTickEventMap.values()) {
            callback.accept(client, player);
        }
    }

    public static void postHudRenderEvent(GuiGraphics graphics, float timer) {
        for (BiConsumer<GuiGraphics, Float> callback : hudRenderEventMap.values()) {
            callback.accept(graphics, timer);
        }
    }

    public static void postClientConnectedEvent() {
        for (Runnable callback : clientConnectedEventMap.values()) {
            callback.run();
        }
    }

    public static void postClientDisconnectedEvent() {
        for (Runnable callback : clientDisconnectedEventMap.values()) {
            callback.run();
        }
    }

    public static void postMouseReleaseEvent(int button) {
        for (Consumer<Integer> callback : mouseReleaseEventMap.values()) {
            callback.accept(button);
        }
    }


    // JourneyMap events
    public static void postAddonButtonDisplayEvent(ThemeButtonDisplay buttonDisplay) {
        for (Consumer<ThemeButtonDisplay> callback : addonButtonDisplayEventMap.values()) {
            callback.accept(buttonDisplay);
        }
    }

    public static void postFullscreenPopupMenuEvent(ModPopupMenu popupMenu) {
        for (Consumer<ModPopupMenu> callback : fullscreenPopupMenuEventMap.values()) {
            callback.accept(popupMenu);
        }
    }

    public static void postUpdatedConfigEvent() {
        for (Runnable callback : updatedConfigEventMap.values()) {
            callback.run();
        }
    }
}
