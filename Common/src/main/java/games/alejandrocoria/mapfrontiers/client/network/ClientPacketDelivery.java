package games.alejandrocoria.mapfrontiers.client.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.Services;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ClientPacketDelivery {
    public static final String DEBUG_PROPERTY = "mapfrontiers.debugPackets";

    private static final boolean DEBUG_ENABLED = Services.PLATFORM.isDevelopmentEnvironment()
            && Boolean.getBoolean(DEBUG_PROPERTY);
    private static final DelayedClientPacketQueue QUEUE = new DelayedClientPacketQueue();

    private ClientPacketDelivery() {
    }

    public static void submit(Runnable action) {
        if (!DEBUG_ENABLED) {
            action.run();
            return;
        }

        QUEUE.submit(action);
    }

    public static void tick() {
        if (DEBUG_ENABLED) {
            QUEUE.tick();
        }
    }

    public static void beginConnection() {
        if (DEBUG_ENABLED) {
            QUEUE.discardPendingPackets();
        }
    }

    public static void changeWorld() {
        if (DEBUG_ENABLED) {
            QUEUE.discardPendingPackets();
        }
    }

    public static void endConnection() {
        if (DEBUG_ENABLED) {
            QUEUE.discardPendingPackets();
        }
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }

    public static void setDelayTicks(int delayTicks) {
        requireDebugEnabled();
        QUEUE.setDelayTicks(delayTicks);
    }

    public static void hold() {
        requireDebugEnabled();
        QUEUE.hold();
    }

    public static void resume() {
        requireDebugEnabled();
        QUEUE.resume();
    }

    public static boolean runNext() {
        requireDebugEnabled();
        return QUEUE.runNext();
    }

    public static int flush() {
        requireDebugEnabled();
        return QUEUE.flush();
    }

    public static int clear() {
        requireDebugEnabled();
        return QUEUE.clear();
    }

    public static int getDelayTicks() {
        return QUEUE.getDelayTicks();
    }

    public static boolean isHolding() {
        return QUEUE.isHolding();
    }

    public static int getPendingCount() {
        return QUEUE.getPendingCount();
    }

    public static void logIfEnabled() {
        if (DEBUG_ENABLED) {
            MapFrontiers.LOGGER.warn("Client packet delivery debugging is enabled via -D{}=true", DEBUG_PROPERTY);
        }
    }

    private static void requireDebugEnabled() {
        if (!DEBUG_ENABLED) {
            throw new IllegalStateException("Client packet delivery debugging is not enabled");
        }
    }
}
