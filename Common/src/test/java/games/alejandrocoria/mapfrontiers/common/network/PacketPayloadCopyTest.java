package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
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
        PlayerId user = playerId(1L);
        group.addUser(user);
        PacketFrontierSettings packet = new PacketFrontierSettings(settings, 7L, 9L,
                OperationResolution.Accepted);

        settings.getOPsGroup().addAction(FrontierSettings.Action.CreateGlobalFrontier);
        group.setName("changed");

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
        assertEquals(user, decoded.getCustomGroups().getFirst().getUsers().getFirst());
        assertEquals(7L, revision);
        assertEquals(9L, requestId);
        assertEquals(OperationResolution.Accepted, resolution);
        encoded.release();
    }

    @Test
    void updateFrontierSettingsPacketCapturesDeepCopy() {
        FrontierSettings settings = new FrontierSettings();
        SettingsGroup group = settings.createCustomGroup("group");
        group.addUser(playerId(8L));
        PacketUpdateFrontierSettings packet = new PacketUpdateFrontierSettings(settings, 15L, 16L);

        group.setName("changed");

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        FrontierSettings decoded = new FrontierSettings();
        decoded.fromBytes(encoded);
        assertEquals("group", decoded.getCustomGroups().getFirst().getName());
        assertEquals(playerId(8L), decoded.getCustomGroups().getFirst().getUsers().getFirst());
        assertEquals(15L, encoded.readLong());
        assertEquals(16L, encoded.readLong());
        encoded.release();
    }

    @Test
    void sharingPacketsCaptureTheirUsers() {
        UUID frontierId = new UUID(4L, 5L);
        FrontierUserAccess sharedUser = new FrontierUserAccess(playerId(2L), true);
        sharedUser.addAction(FrontierUserAccess.Action.UpdateFrontier);
        PacketSharePersonalFrontier sharePacket = new PacketSharePersonalFrontier(frontierId, sharedUser, 11L, 12L);
        PacketUpdateSharedUserPersonalFrontier updatePacket =
                new PacketUpdateSharedUserPersonalFrontier(frontierId, sharedUser, 11L, 12L);

        sharedUser.addAction(FrontierUserAccess.Action.UpdateSettings);
        sharedUser.setPending(false);

        assertCapturedSharedUser(sharePacket::encode, frontierId, 11L, 12L);
        assertCapturedSharedUser(updatePacket::encode, frontierId, 11L, 12L);
    }

    @Test
    void removeSharingPacketCapturesTargetUser() {
        UUID frontierId = new UUID(6L, 7L);
        PlayerId targetUser = playerId(3L);
        PacketRemoveSharedUserPersonalFrontier packet =
                new PacketRemoveSharedUserPersonalFrontier(frontierId, targetUser, 13L, 14L);

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        assertEquals(frontierId, UUIDHelper.fromBytes(encoded));
        assertEquals(targetUser, PlayerIdNetworkCodec.read(encoded));
        assertEquals(13L, encoded.readLong());
        assertEquals(14L, encoded.readLong());
        encoded.release();
    }

    @Test
    void invitationPacketUsesPlayerIdsWithoutNames() {
        PlayerId playerSharing = playerId(8L);
        PlayerId owner = playerId(9L);
        PacketPersonalFrontierShared packet = new PacketPersonalFrontierShared(10, playerSharing, owner,
                "First", "Second");

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);

        assertEquals(10, encoded.readInt());
        assertEquals(playerSharing, PlayerIdNetworkCodec.read(encoded));
        assertEquals(owner, PlayerIdNetworkCodec.read(encoded));
        assertEquals("First", encoded.readUtf());
        assertEquals("Second", encoded.readUtf());
        encoded.release();
    }

    private static void assertCapturedSharedUser(PacketEncoder encoder, UUID frontierId,
                                                 long baseRevision, long requestId) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        encoder.encode(encoded);
        assertEquals(frontierId, UUIDHelper.fromBytes(encoded));
        FrontierUserAccess decoded = FrontierUserAccess.fromBytes(encoded);
        assertEquals(playerId(2L), decoded.getPlayerId());
        assertTrue(decoded.isPending());
        assertTrue(decoded.hasAction(FrontierUserAccess.Action.UpdateFrontier));
        assertFalse(decoded.hasAction(FrontierUserAccess.Action.UpdateSettings));
        assertEquals(baseRevision, encoded.readLong());
        assertEquals(requestId, encoded.readLong());
        encoded.release();
    }

    private static PlayerId playerId(long uuidValue) {
        return new PlayerId(new UUID(0L, uuidValue));
    }

    @FunctionalInterface
    private interface PacketEncoder {
        void encode(FriendlyByteBuf buf);
    }
}
