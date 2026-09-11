package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtCodec;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public class SettingsGroup {
    private String name;
    private List<PlayerId> users;
    private final Set<FrontierSettings.Action> actions;
    private final boolean special;

    public SettingsGroup() {
        name = "";
        users = new ArrayList<>();
        actions = EnumSet.noneOf(FrontierSettings.Action.class);
        special = false;
    }

    public SettingsGroup(String name, boolean special) {
        this.name = name;
        users = new ArrayList<>();
        actions = EnumSet.noneOf(FrontierSettings.Action.class);
        this.special = special;
    }

    public SettingsGroup(SettingsGroup other) {
        name = other.name;
        users = new ArrayList<>(other.users.size());
        users.addAll(other.users);
        actions = other.actions.isEmpty()
                ? EnumSet.noneOf(FrontierSettings.Action.class)
                : EnumSet.copyOf(other.actions);
        special = other.special;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addAction(FrontierSettings.Action action) {
        actions.add(action);
    }

    public List<PlayerId> getUsers() {
        return users;
    }

    public void removeAction(FrontierSettings.Action action) {
        actions.remove(action);
    }

    public boolean hasAction(FrontierSettings.Action action) {
        return actions.contains(action);
    }

    public Set<FrontierSettings.Action> getActions() {
        return actions;
    }

    public void addUser(PlayerId user) {
        users.add(Objects.requireNonNull(user, "user"));
    }

    public void removeUser(PlayerId user) {
        users.remove(user);
    }

    public boolean hasUser(PlayerId user) {
        return users.contains(user);
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean hasSameFunctionalState(SettingsGroup other) {
        if (!name.equals(other.name) || special != other.special || !actions.equals(other.actions)
                || users.size() != other.users.size()) {
            return false;
        }

        return users.equals(other.users);
    }

    public boolean readFromNBT(CompoundTag nbt, int version, PlayerReferenceNbtReadContext context) {
        boolean changedDuringLoad = false;
        if (!special) {
            name = nbt.getStringOr("name", "");
            users.clear();
            ListTag usersTagList = nbt.getListOrEmpty("users");
            for (int i = 0; i < usersTagList.size(); ++i) {
                try {
                    CompoundTag userTag = NbtReadHelper.requireCompound(usersTagList, i, "users");
                    PlayerReferenceNbtCodec.ReadResult result = PlayerReferenceNbtCodec.read(userTag, context);
                    changedDuringLoad |= result.repaired();
                    users.add(result.playerId());
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid user in group {} at users[{}]: {}", name, i, e.getMessage());
                    changedDuringLoad = true;
                }
            }
        }

        actions.clear();
        ListTag actionsTagList = nbt.getListOrEmpty("actions");
        for (int i = 0; i < actionsTagList.size(); ++i) {
            String actionTag;
            try {
                actionTag = NbtReadHelper.requireString(actionsTagList, i, "actions");
            } catch (InvalidNbtFormatException e) {
                MapFrontiers.LOGGER.warn("Skipping invalid action in group {} at actions[{}]: {}", name, i, e.getMessage());
                continue;
            }

            List<FrontierSettings.Action> availableActions = FrontierSettings.getAvailableActions(name);

            try {
                FrontierSettings.Action action;
                if (version > 3) {
                    action = FrontierSettings.Action.valueOf(actionTag);
                } else {
                    action = FrontierSettings.ActionV3.valueOf(actionTag).toAction();
                }

                if (!availableActions.contains(action)) {
                    throw new IllegalArgumentException();
                }
                actions.add(action);
            } catch (IllegalArgumentException e) {
                String availableActionsString;

                if (version > 3) {
                    availableActionsString = StringHelper.enumValuesToString(availableActions);
                } else {
                    availableActionsString = StringHelper.enumValuesToString(FrontierSettings.getAvailableActionsV3(name));
                }

                MapFrontiers.LOGGER.warn("Unknown action in group {}. Found: \"{}\". Expected: {}", name, actionTag, availableActionsString);
            }
        }

        return changedDuringLoad;
    }

    public void writeToNBT(CompoundTag nbt, PlayerNameResolver resolver) {
        if (!special) {
            nbt.putString("name", name);
            ListTag usersTagList = new ListTag();
            for (PlayerId user : users) {
                CompoundTag userTag = new CompoundTag();
                PlayerReferenceNbtCodec.write(userTag, user, resolver);
                usersTagList.add(userTag);
            }

            nbt.put("users", usersTagList);
        }

        ListTag actionsTagList = new ListTag();
        for (FrontierSettings.Action action : actions) {
            StringTag actionTag = StringTag.valueOf(action.name());
            actionsTagList.add(actionTag);
        }

        nbt.put("actions", actionsTagList);
    }

    public void fromBytes(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            name = buf.readUtf(SharedConstants.MAX_PLAYER_NAME_LENGTH);

            users = new ArrayList<>();
            int usersCount = buf.readInt();
            for (int i = 0; i < usersCount; ++i) {
                users.add(PlayerIdNetworkCodec.read(buf));
            }
        }

        actions.clear();
        for (FrontierSettings.Action action : FrontierSettings.Action.VALUES) {
            if (buf.readBoolean()) {
                actions.add(action);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(special);

        if (!special) {
            buf.writeUtf(name, SharedConstants.MAX_PLAYER_NAME_LENGTH);

            buf.writeInt(users.size());
            for (PlayerId user : users) {
                PlayerIdNetworkCodec.write(buf, user);
            }
        }

        for (FrontierSettings.Action action : FrontierSettings.Action.VALUES) {
            buf.writeBoolean(actions.contains(action));
        }
    }
}
