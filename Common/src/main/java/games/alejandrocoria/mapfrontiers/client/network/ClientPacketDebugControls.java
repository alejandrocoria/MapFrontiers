package games.alejandrocoria.mapfrontiers.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ClientPacketDebugControls {
    private ClientPacketDebugControls() {
    }

    public static int setDelay(int ticks) {
        ClientPacketDelivery.setDelayTicks(ticks);
        report("Delivery delay updated");
        return 1;
    }

    public static int hold() {
        ClientPacketDelivery.hold();
        report("Automatic delivery held");
        return 1;
    }

    public static int resume() {
        ClientPacketDelivery.resume();
        report("Automatic delivery resumed");
        return 1;
    }

    public static int runNext() {
        boolean executed = ClientPacketDelivery.runNext();
        report(executed ? "Released one packet" : "No packet was queued");
        return 1;
    }

    public static int flush() {
        int flushed = ClientPacketDelivery.flush();
        report("Released " + flushed + " packet(s)");
        return 1;
    }

    public static int clear() {
        int cleared = ClientPacketDelivery.clear();
        report("Discarded " + cleared + " packet(s)");
        return 1;
    }

    private static void report(String action) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String state = "delay=" + ClientPacketDelivery.getDelayTicks()
                + ", hold=" + ClientPacketDelivery.isHolding()
                + ", queued=" + ClientPacketDelivery.getPendingCount();
        player.sendSystemMessage(Component.literal("[MapFrontiers] " + action + " (" + state + ")"));
    }
}
