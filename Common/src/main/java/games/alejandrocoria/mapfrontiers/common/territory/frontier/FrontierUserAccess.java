package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtCodec;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class FrontierUserAccess {
    public enum Action {
        UpdateFrontier, UpdateSettings;

        public static final Action[] VALUES = values();
    }

    public record NbtReadResult(FrontierUserAccess access, boolean repaired) {
        public NbtReadResult {
            Objects.requireNonNull(access, "access");
        }
    }

    private final PlayerId playerId;
    private Set<Action> actions;
    private boolean pending;

    public FrontierUserAccess(PlayerId playerId, boolean pending) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        actions = EnumSet.noneOf(Action.class);
        this.pending = pending;
    }

    public FrontierUserAccess(FrontierUserAccess other) {
        playerId = other.playerId;
        actions = copyActions(other.actions);
        pending = other.pending;
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public void setActions(Set<Action> actions) {
        this.actions = copyActions(actions);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void removeAction(Action action) {
        actions.remove(action);
    }

    public boolean hasAction(Action action) {
        return actions.contains(action);
    }

    public Set<Action> getActions() {
        return actions;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean hasSameState(FrontierUserAccess other) {
        return playerId.equals(other.playerId) && pending == other.pending && actions.equals(other.actions);
    }

    public static NbtReadResult readFromNBT(CompoundTag nbt, PlayerReferenceNbtReadContext context) {
        PlayerReferenceNbtCodec.ReadResult playerResult = PlayerReferenceNbtCodec.read(nbt, context);
        FrontierUserAccess access = new FrontierUserAccess(playerResult.playerId(), nbt.getBooleanOr("pending", false));

        ListTag actionsTagList = nbt.getListOrEmpty("actions");
        for (int i = 0; i < actionsTagList.size(); ++i) {
            String actionTag;
            try {
                actionTag = NbtReadHelper.requireString(actionsTagList, i, "actions");
            } catch (InvalidNbtFormatException e) {
                MapFrontiers.LOGGER.warn("Skipping invalid shared-user action at actions[{}]: {}", i, e.getMessage());
                continue;
            }

            try {
                access.actions.add(Action.valueOf(actionTag));
            } catch (IllegalArgumentException e) {
                String availableActions = StringHelper.enumValuesToString(Arrays.asList(Action.VALUES));
                MapFrontiers.LOGGER.warn("Unknown action for shared user {}. Found: \"{}\". Expected: {}",
                        access.playerId.uuid(), actionTag, availableActions);
            }
        }

        return new NbtReadResult(access, playerResult.repaired());
    }

    public void writeToNBT(CompoundTag nbt, PlayerNameResolver resolver) {
        PlayerReferenceNbtCodec.write(nbt, playerId, resolver);
        if (pending) {
            nbt.putBoolean("pending", true);
        }

        ListTag actionsTagList = new ListTag();
        for (Action action : actions) {
            actionsTagList.add(StringTag.valueOf(action.name()));
        }
        nbt.put("actions", actionsTagList);
    }

    public static FrontierUserAccess fromBytes(FriendlyByteBuf buf) {
        FrontierUserAccess access = new FrontierUserAccess(PlayerIdNetworkCodec.read(buf), buf.readBoolean());
        for (Action action : Action.VALUES) {
            if (buf.readBoolean()) {
                access.actions.add(action);
            }
        }
        return access;
    }

    public void toBytes(FriendlyByteBuf buf) {
        PlayerIdNetworkCodec.write(buf, playerId);
        buf.writeBoolean(pending);
        for (Action action : Action.VALUES) {
            buf.writeBoolean(actions.contains(action));
        }
    }

    private static Set<Action> copyActions(Set<Action> actions) {
        return actions.isEmpty() ? EnumSet.noneOf(Action.class) : EnumSet.copyOf(actions);
    }
}
