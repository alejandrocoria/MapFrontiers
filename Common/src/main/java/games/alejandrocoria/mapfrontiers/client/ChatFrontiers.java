package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ChatFrontiers {
    public record ReceivedFrontierCopy(FrontierData frontier, @Nullable CollectionData collection) {
    }

    private static int currentReceivedMessageId = -1;
    private static final List<String> receivedData = new ArrayList<>();
    private static final LinkedHashMap<Integer, ReceivedFrontierCopy> receivedFrontiers = new LinkedHashMap<>(3);

    public static void clear() {
        resetReceivedMessageAssembly();
        receivedFrontiers.clear();
    }

    @Nullable
    public static ReceivedFrontierCopy getReceivedFrontier(int id) {
        return receivedFrontiers.get(id);
    }

    public static void removeReceivedId(int id) {
        receivedFrontiers.remove(id);
    }

    public static void sendFrontier(FrontierOverlay frontier, SettingsUser user) {
        try {
            if (frontier.isSessionOnly()) {
                MapFrontiers.LOGGER.debug("Rejected sendFrontier because source frontier is SESSION_ONLY. frontierId={}, targetUser={}",
                        frontier.getId(), user.username);
                return;
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            CompoundTag nbt = new CompoundTag();
            CompoundTag frontierTag = new CompoundTag();
            frontier.writeToNBT(frontierTag);
            nbt.put("frontier", frontierTag);

            if (frontier.hasCollection()) {
                CollectionData collection = MapFrontiersClient.getCollection(frontier.getCollectionId());
                if (collection != null) {
                    CompoundTag collectionTag = new CompoundTag();
                    collection.writeToNBT(collectionTag);
                    nbt.put("collection", collectionTag);
                }
            }

            String encodedData = encodeNBT(nbt);
            String command = ClientConfig.SEND_COMMAND.get() + " " + user.username + " #MapFrontiers:";
            String format = "%d:%d:%d:%d:%s";

            List<String> dataList = new ArrayList<>();
            int maxLength = SharedConstants.MAX_CHAT_LENGTH - 1 - command.length() - format.length();
            int dataLength = encodedData.length();
            for (int i = 0; i < dataLength; i += maxLength) {
                int end = Math.min(dataLength, i + maxLength);
                dataList.add(encodedData.substring(i, end));
            }

            int RandomId = new Random().nextInt(999) + 1;

            for (int i = 0; i < dataList.size(); ++i) {
                String message = command + String.format(format, MapFrontiers.FRONTIER_DATA_VERSION, RandomId, i + 1, dataList.size(), dataList.get(i));
                player.connection.sendCommand(message);
            }

        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to send frontier {} {} to user {}: {}", frontier.getName1(), frontier.getName2(), user.username, t);
        }
    }

    public static boolean receiveFrontierFromChat(Component messageComponent, @Nullable UUID sender) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || sender == null) {
            return false;
        }

        String message = messageComponent.getString();
        int startIndex = message.indexOf("#MapFrontiers:");
        if (startIndex == -1) {
            return false;
        } else if (player.getUUID().equals(sender)) {
            return true;
        }
        message = message.substring(startIndex);

        try {
            String[] parts = message.split(":", 6);
            if (parts.length != 6) {
                throw new IllegalArgumentException();
            }

            int version = Integer.parseInt(parts[1]);
            int id = Integer.parseInt(parts[2]);
            int index = Integer.parseInt(parts[3]);
            int total = Integer.parseInt(parts[4]);
            String data = parts[5];

            if (id > 999) {
                throw new IllegalArgumentException();
            }

            if (index < 1 || index > total) {
                throw new IllegalArgumentException();
            }

            if (shouldStartNewMessageAssembly(id, index, total)) {
                startReceivedMessageAssembly(id, total);
            }

            receivedData.set(index - 1, data);

            if (receivedData.stream().noneMatch(String::isBlank)) {
                String encodedData = String.join("", receivedData);
                CompoundTag payload = decodeNBT(encodedData);
                FrontierData frontier = readFrontier(payload, version);
                CollectionData collection = readCollection(payload, version);
                frontier.setCopiedFromId(frontier.getId());
                frontier.setCopiedFromUser(frontier.getOwner());
                frontier.setId(UUID.randomUUID());
                frontier.setOwner(new SettingsUser(player));
                frontier.setPersonal(true);
                if (collection != null) {
                    collection.setCopiedFromId(collection.getId());
                    collection.setCopiedFromUser(collection.getOwner());
                    collection.setId(UUID.randomUUID());
                    collection.setOwner(new SettingsUser(player));
                    collection.setPersonal(true);
                    frontier.setCollectionId(collection.getId());
                } else {
                    frontier.setCollectionId(null);
                }
                receivedFrontiers.remove(currentReceivedMessageId);
                if (receivedFrontiers.size() == 3) {
                    var iterator = receivedFrontiers.entrySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                receivedFrontiers.put(currentReceivedMessageId, new ReceivedFrontierCopy(frontier, collection));

                String frontierName;
                if (frontier.getName1().isEmpty() && frontier.getName2().isEmpty()) {
                    frontierName = "Unnamed Frontier";
                } else if (frontier.getName1().isEmpty()) {
                    frontierName = frontier.getName2();
                } else if (frontier.getName2().isEmpty()) {
                    frontierName = frontier.getName1();
                } else {
                    frontierName = frontier.getName1() + " " + frontier.getName2();
                }

                MutableComponent button = Component.literal(frontierName);
                button.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to accept or use command /mfacceptcopy " + currentReceivedMessageId))));
                button.withStyle(style -> style.withBold(true));
                button.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/mapfrontiersacceptcopy " + currentReceivedMessageId)));

                SettingsUser userSender = new SettingsUser();
                userSender.uuid = sender;
                userSender.fillMissingInfo(true, null);
                MutableComponent text = Component.literal(SettingsUserFormatter.getDisplayName(userSender, "User not found") + " ");
                if (userSender.equals(frontier.getCopiedFromUser())) {
                    text.append("want to send a frontier to you: ");
                } else {
                    text.append("want to send a frontier of " + SettingsUserFormatter.getDisplayName(frontier.getCopiedFromUser(), "User not found") + " to you: ");
                }

                text.append(button);
                player.displayClientMessage(text, false);

                resetReceivedMessageAssembly();
            }

        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to parse frontier from chat: \"{}\"\n{}", message, t);
            return false;
        }

        return true;
    }

    private static String encodeNBT(CompoundTag nbt) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            NbtIo.writeCompressed(nbt, dos);
        }
        byte[] compressedBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(compressedBytes);
    }

    private static CompoundTag decodeNBT(String base64) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64);
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            return NbtIo.readCompressed(dis); // 16KB ought to be enough for anybody
        }
    }

    private static FrontierData readFrontier(CompoundTag payload, int version) {
        FrontierData frontier = new FrontierData();
        if (payload.contains("frontier")) {
            frontier.readFromNBT(NbtCompat.getCompoundOrEmpty(payload, "frontier"), version);
        } else {
            frontier.readFromNBT(payload, version);
        }
        return frontier;
    }

    private static @Nullable CollectionData readCollection(CompoundTag payload, int version) {
        if (!payload.contains("collection")) {
            return null;
        }

        CollectionData collection = new CollectionData();
        collection.readFromNBT(NbtCompat.getCompoundOrEmpty(payload, "collection"), version);
        return collection;
    }

    private static boolean shouldStartNewMessageAssembly(int messageId, int partIndex, int totalParts) {
        return messageId != currentReceivedMessageId
                || totalParts != receivedData.size()
                || !receivedData.get(partIndex - 1).isBlank();
    }

    private static void startReceivedMessageAssembly(int messageId, int totalParts) {
        resetReceivedMessageAssembly();
        receivedData.addAll(Collections.nCopies(totalParts, ""));
        currentReceivedMessageId = messageId;
    }

    private static void resetReceivedMessageAssembly() {
        currentReceivedMessageId = -1;
        receivedData.clear();
    }
}
