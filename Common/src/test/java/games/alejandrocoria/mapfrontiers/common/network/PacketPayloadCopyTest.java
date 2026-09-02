package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketPayloadCopyTest {
    @Test
    void frontierSettingsPacketCapturesDeepCopy() {
        FrontierSettings settings = new FrontierSettings();
        settings.getOPsGroup().addAction(FrontierSettings.Action.UpdateSettings);
        SettingsGroup group = settings.createCustomGroup("group");
        SettingsUser user = user("original", 1L);
        group.addUser(user);
        PacketFrontierSettings packet = new PacketFrontierSettings(settings, 7L, 9L,
                OperationResolution.Accepted);

        settings.getOPsGroup().addAction(FrontierSettings.Action.CreateGlobalFrontier);
        group.setName("changed");
        user.username = "changed";

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        FrontierSettings decoded = new FrontierSettings();
        decoded.fromBytes(encoded);
        long revision = encoded.readLong();
        long requestId = encoded.readLong();
        OperationResolution resolution = OperationResolution.VALUES[encoded.readInt()];

        assertTrue(decoded.getOPsGroup().hasAction(FrontierSettings.Action.UpdateSettings));
        assertFalse(decoded.getOPsGroup().hasAction(FrontierSettings.Action.CreateGlobalFrontier));
        assertEquals("group", decoded.getCustomGroups().getFirst().getName());
        assertEquals("original", decoded.getCustomGroups().getFirst().getUsers().getFirst().username);
        assertEquals(7L, revision);
        assertEquals(9L, requestId);
        assertEquals(OperationResolution.Accepted, resolution);
        encoded.release();
    }

    @Test
    void updateFrontierSettingsPacketCapturesDeepCopy() {
        FrontierSettings settings = new FrontierSettings();
        SettingsGroup group = settings.createCustomGroup("group");
        group.addUser(user("original", 8L));
        PacketUpdateFrontierSettings packet = new PacketUpdateFrontierSettings(settings, 15L, 16L);

        group.setName("changed");
        group.getUsers().getFirst().username = "changed";

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        FrontierSettings decoded = new FrontierSettings();
        decoded.fromBytes(encoded);
        assertEquals("group", decoded.getCustomGroups().getFirst().getName());
        assertEquals("original", decoded.getCustomGroups().getFirst().getUsers().getFirst().username);
        assertEquals(15L, encoded.readLong());
        assertEquals(16L, encoded.readLong());
        encoded.release();
    }

    @Test
    void sharingPacketsCaptureTheirUsers() {
        UUID frontierId = new UUID(4L, 5L);
        SettingsUserShared sharedUser = new SettingsUserShared(user("original", 2L), true);
        sharedUser.addAction(SettingsUserShared.Action.UpdateFrontier);
        PacketSharePersonalFrontier sharePacket = new PacketSharePersonalFrontier(frontierId, sharedUser, 11L, 12L);
        PacketUpdateSharedUserPersonalFrontier updatePacket =
                new PacketUpdateSharedUserPersonalFrontier(frontierId, sharedUser, 11L, 12L);

        sharedUser.getUser().username = "changed";
        sharedUser.addAction(SettingsUserShared.Action.UpdateSettings);
        sharedUser.setPending(false);

        assertCapturedSharedUser(sharePacket::encode, frontierId, 11L, 12L);
        assertCapturedSharedUser(updatePacket::encode, frontierId, 11L, 12L);
    }

    @Test
    void removeSharingPacketCapturesTargetUser() {
        UUID frontierId = new UUID(6L, 7L);
        SettingsUser targetUser = user("original", 3L);
        PacketRemoveSharedUserPersonalFrontier packet =
                new PacketRemoveSharedUserPersonalFrontier(frontierId, targetUser, 13L, 14L);
        targetUser.username = "changed";
        targetUser.uuid = new UUID(0L, 4L);

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        assertEquals(frontierId, UUIDHelper.fromBytes(encoded));
        SettingsUser decoded = new SettingsUser();
        decoded.fromBytes(encoded);
        assertEquals("original", decoded.username);
        assertEquals(new UUID(0L, 3L), decoded.uuid);
        assertEquals(13L, encoded.readLong());
        assertEquals(14L, encoded.readLong());
        encoded.release();
    }

    private static void assertCapturedSharedUser(PacketEncoder encoder, UUID frontierId,
                                                 long baseRevision, long requestId) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        encoder.encode(encoded);
        assertEquals(frontierId, UUIDHelper.fromBytes(encoded));
        SettingsUserShared decoded = new SettingsUserShared();
        decoded.fromBytes(encoded);
        assertEquals("original", decoded.getUser().username);
        assertTrue(decoded.isPending());
        assertTrue(decoded.hasAction(SettingsUserShared.Action.UpdateFrontier));
        assertFalse(decoded.hasAction(SettingsUserShared.Action.UpdateSettings));
        assertEquals(baseRevision, encoded.readLong());
        assertEquals(requestId, encoded.readLong());
        encoded.release();
    }

    private static SettingsUser user(String username, long uuidValue) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = new UUID(0L, uuidValue);
        return user;
    }

    @FunctionalInterface
    private interface PacketEncoder {
        void encode(FriendlyByteBuf buf);
    }
}
