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
    private static class MessageHandler<MSG> {
        public final ResourceLocation channelName;
        public final BiConsumer<MSG, FriendlyByteBuf> encoder;

        public MessageHandler(ResourceLocation channelName, BiConsumer<MSG, FriendlyByteBuf> encoder) {
            this.channelName = channelName;
            this.encoder = encoder;
        }
    }

    private static final Map<Class<?>, MessageHandler<?>> messageHandlers = new HashMap<>();

    @Environment(EnvType.CLIENT)
    public static void registerClientReceivers() {
        registerClientReceiver("PacketFrontier", PacketFrontier.class, PacketFrontier::toBytes,
                PacketFrontier::fromBytes, PacketFrontier::handle);
        registerClientReceiver("PacketFrontierDeleted", PacketFrontierDeleted.class, PacketFrontierDeleted::toBytes,
                PacketFrontierDeleted::fromBytes, PacketFrontierDeleted::handle);
        registerClientReceiver("PacketFrontierUpdated", PacketFrontierUpdated.class, PacketFrontierUpdated::toBytes,
                PacketFrontierUpdated::fromBytes, PacketFrontierUpdated::handle);
        registerClientReceiver("PacketFrontierSettings", PacketFrontierSettings.class, PacketFrontierSettings::toBytes,
                PacketFrontierSettings::fromBytes, PacketFrontierSettings::handle);
        registerClientReceiver("PacketSettingsProfile", PacketSettingsProfile.class, PacketSettingsProfile::toBytes,
                PacketSettingsProfile::fromBytes, PacketSettingsProfile::handle);
        registerClientReceiver("PacketPersonalFrontierShared", PacketPersonalFrontierShared.class, PacketPersonalFrontierShared::toBytes,
                PacketPersonalFrontierShared::fromBytes, PacketPersonalFrontierShared::handle);
    }

    public static void registerServerReceivers() {
        registerServerReceiver("PacketNewFrontier", PacketNewFrontier.class, PacketNewFrontier::toBytes,
                PacketNewFrontier::fromBytes, PacketNewFrontier::handle);
        registerServerReceiver("PacketDeleteFrontier", PacketDeleteFrontier.class, PacketDeleteFrontier::toBytes,
                PacketDeleteFrontier::fromBytes, PacketDeleteFrontier::handle);
        registerServerReceiver("PacketUpdateFrontier", PacketUpdateFrontier.class, PacketUpdateFrontier::toBytes,
                PacketUpdateFrontier::fromBytes, PacketUpdateFrontier::handle);
        registerServerReceiver("PacketRequestFrontierSettings", PacketRequestFrontierSettings.class, PacketRequestFrontierSettings::toBytes,
                PacketRequestFrontierSettings::fromBytes, PacketRequestFrontierSettings::handle);
        registerServerReceiver("PacketFrontierSettings", PacketFrontierSettings.class, PacketFrontierSettings::toBytes,
                PacketFrontierSettings::fromBytes, PacketFrontierSettings::handle);
        registerServerReceiver("PacketSharePersonalFrontier", PacketSharePersonalFrontier.class, PacketSharePersonalFrontier::toBytes,
                PacketSharePersonalFrontier::fromBytes, PacketSharePersonalFrontier::handle);
        registerServerReceiver("PacketRemoveSharedUserPersonalFrontier", PacketRemoveSharedUserPersonalFrontier.class,
                PacketRemoveSharedUserPersonalFrontier::toBytes, PacketRemoveSharedUserPersonalFrontier::fromBytes,
                PacketRemoveSharedUserPersonalFrontier::handle);
        registerServerReceiver("PacketUpdateSharedUserPersonalFrontier", PacketUpdateSharedUserPersonalFrontier.class,
                PacketUpdateSharedUserPersonalFrontier::toBytes, PacketUpdateSharedUserPersonalFrontier::fromBytes,
                PacketUpdateSharedUserPersonalFrontier::handle);
    }

    @Environment(EnvType.CLIENT)
    private static <MSG> void registerClientReceiver(String id, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Minecraft> messageConsumer) {
        ResourceLocation channelName = new ResourceLocation(MapFrontiers.MODID, id.toLowerCase());
        messageHandlers.put(messageType, new MessageHandler<>(channelName, encoder));
        ClientPlayNetworking.registerGlobalReceiver(channelName, (client, handler, buf, responseSender) -> {
            MSG message = decoder.apply(buf);
            messageConsumer.accept(message, client);
        });
    }

    private static <MSG> void registerServerReceiver(String id, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, ServerMessageConsumer<MSG, MinecraftServer, ServerPlayer> messageConsumer) {
        ResourceLocation channelName = new ResourceLocation(MapFrontiers.MODID, id.toLowerCase());
        messageHandlers.put(messageType, new MessageHandler<>(channelName, encoder));
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
        MessageHandler<MSG> handler = (MessageHandler<MSG>) messageHandlers.get(messageType);
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
        MessageHandler<MSG> handler = (MessageHandler<MSG>) messageHandlers.get(messageType);
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
