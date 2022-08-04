package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public class PacketHandler {
    private static class MessageEncoder<MSG> {
        public final ResourceLocation channelName;
        public final BiConsumer<MSG, FriendlyByteBuf> encoder;

        public MessageEncoder(ResourceLocation channelName, BiConsumer<MSG, FriendlyByteBuf> encoder) {
            this.channelName = channelName;
            this.encoder = encoder;
        }
    }

    private static final Map<Class<?>, MessageEncoder<?>> messageEncoders = new HashMap<>();

    @Environment(EnvType.CLIENT)
    public static void registerClientReceivers() {
        registerClientReceiver("PacketFrontier", PacketFrontier::fromBytes, PacketFrontier::handle);
        registerClientReceiver("PacketFrontierDeleted", PacketFrontierDeleted::fromBytes, PacketFrontierDeleted::handle);
        registerClientReceiver("PacketFrontierUpdated", PacketFrontierUpdated::fromBytes, PacketFrontierUpdated::handle);
        registerClientReceiver("PacketFrontierSettings", PacketFrontierSettings::fromBytes, PacketFrontierSettings::handle);
        registerClientReceiver("PacketSettingsProfile", PacketSettingsProfile::fromBytes, PacketSettingsProfile::handle);
        registerClientReceiver("PacketPersonalFrontierShared", PacketPersonalFrontierShared::fromBytes, PacketPersonalFrontierShared::handle);
    }

    public static void registerServerReceivers() {
        registerMessageEncoders();

        registerServerReceiver("PacketNewFrontier", PacketNewFrontier::fromBytes, PacketNewFrontier::handle);
        registerServerReceiver("PacketDeleteFrontier", PacketDeleteFrontier::fromBytes, PacketDeleteFrontier::handle);
        registerServerReceiver("PacketUpdateFrontier", PacketUpdateFrontier::fromBytes, PacketUpdateFrontier::handle);
        registerServerReceiver("PacketRequestFrontierSettings", PacketRequestFrontierSettings::fromBytes, PacketRequestFrontierSettings::handle);
        registerServerReceiver("PacketFrontierSettings", PacketFrontierSettings::fromBytes, PacketFrontierSettings::handle);
        registerServerReceiver("PacketSharePersonalFrontier", PacketSharePersonalFrontier::fromBytes, PacketSharePersonalFrontier::handle);
        registerServerReceiver("PacketRemoveSharedUserPersonalFrontier", PacketRemoveSharedUserPersonalFrontier::fromBytes, PacketRemoveSharedUserPersonalFrontier::handle);
        registerServerReceiver("PacketUpdateSharedUserPersonalFrontier", PacketUpdateSharedUserPersonalFrontier::fromBytes, PacketUpdateSharedUserPersonalFrontier::handle);
    }

    private static void registerMessageEncoders() {
        registerMessageEncoder("PacketFrontier", PacketFrontier.class, PacketFrontier::toBytes);
        registerMessageEncoder("PacketFrontierDeleted", PacketFrontierDeleted.class, PacketFrontierDeleted::toBytes);
        registerMessageEncoder("PacketFrontierUpdated", PacketFrontierUpdated.class, PacketFrontierUpdated::toBytes);
        registerMessageEncoder("PacketFrontierSettings", PacketFrontierSettings.class, PacketFrontierSettings::toBytes);
        registerMessageEncoder("PacketSettingsProfile", PacketSettingsProfile.class, PacketSettingsProfile::toBytes);
        registerMessageEncoder("PacketPersonalFrontierShared", PacketPersonalFrontierShared.class, PacketPersonalFrontierShared::toBytes);
        registerMessageEncoder("PacketNewFrontier", PacketNewFrontier.class, PacketNewFrontier::toBytes);
        registerMessageEncoder("PacketDeleteFrontier", PacketDeleteFrontier.class, PacketDeleteFrontier::toBytes);
        registerMessageEncoder("PacketUpdateFrontier", PacketUpdateFrontier.class, PacketUpdateFrontier::toBytes);
        registerMessageEncoder("PacketRequestFrontierSettings", PacketRequestFrontierSettings.class, PacketRequestFrontierSettings::toBytes);
        registerMessageEncoder("PacketFrontierSettings", PacketFrontierSettings.class, PacketFrontierSettings::toBytes);
        registerMessageEncoder("PacketSharePersonalFrontier", PacketSharePersonalFrontier.class, PacketSharePersonalFrontier::toBytes);
        registerMessageEncoder("PacketRemoveSharedUserPersonalFrontier", PacketRemoveSharedUserPersonalFrontier.class, PacketRemoveSharedUserPersonalFrontier::toBytes);
        registerMessageEncoder("PacketUpdateSharedUserPersonalFrontier", PacketUpdateSharedUserPersonalFrontier.class, PacketUpdateSharedUserPersonalFrontier::toBytes);
    }

    private static <MSG> void registerMessageEncoder(String id, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder) {
        ResourceLocation channelName = new ResourceLocation(MapFrontiers.MODID, id.toLowerCase());
        messageEncoders.put(messageType, new MessageEncoder<>(channelName, encoder));
    }

    @Environment(EnvType.CLIENT)
    private static <MSG> void registerClientReceiver(String id, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Minecraft> messageConsumer) {
        ResourceLocation channelName = new ResourceLocation(MapFrontiers.MODID, id.toLowerCase());
        ClientPlayNetworking.registerGlobalReceiver(channelName, (client, handler, buf, responseSender) -> {
            MSG message = decoder.apply(buf);
            messageConsumer.accept(message, client);
        });
    }

    private static <MSG> void registerServerReceiver(String id, Function<FriendlyByteBuf, MSG> decoder, ServerMessageConsumer<MSG, MinecraftServer, ServerPlayer> messageConsumer) {
        ResourceLocation channelName = new ResourceLocation(MapFrontiers.MODID, id.toLowerCase());
        ServerPlayNetworking.registerGlobalReceiver(channelName, (server, player, handler, buf, responseSender) -> {
            MSG message = decoder.apply(buf);
            messageConsumer.accept(message, server, player);
        });
    }

    public static <MSG> void sendToUsersWithAccess(Class<MSG> messageType, MSG message, FrontierData frontier, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(frontier.getOwner().uuid);
        if (player != null) {
            sendTo(messageType, message, player);
        }

        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                if (!userShared.isPending()) {
                    player = server.getPlayerList().getPlayer(userShared.getUser().uuid);
                    if (player != null) {
                        sendTo(messageType, message, player);
                    }
                }
            }
        }
    }

    public static <MSG> void sendTo(Class<MSG> messageType, MSG message, ServerPlayer player) {
        MessageEncoder<MSG> handler = (MessageEncoder<MSG>) messageEncoders.get(messageType);
        if (handler == null || !ServerPlayNetworking.canSend(player, handler.channelName)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        handler.encoder.accept(message, buf);
        ServerPlayNetworking.send(player, handler.channelName, buf);
    }

    public static <MSG> void sendToAll(Class<MSG> messageType, MSG message, MinecraftServer server) {
        for (ServerPlayer player : PlayerLookup.all(server)) {
            sendTo(messageType, message, player);
        }
    }

    @Environment(EnvType.CLIENT)
    public static <MSG> void sendToServer(Class<MSG> messageType, MSG message) {
        MessageEncoder<MSG> handler = (MessageEncoder<MSG>) messageEncoders.get(messageType);
        if (handler == null || !ClientPlayNetworking.canSend(handler.channelName)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        handler.encoder.accept(message, buf);
        ClientPlayNetworking.send(handler.channelName, buf);
    }

    @FunctionalInterface
    private interface ServerMessageConsumer<MSG, S, P> {
        void accept(MSG message, S server, P player);
    }
}
