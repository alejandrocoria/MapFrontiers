package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
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
    private List<SettingsUser> users;
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
        for (SettingsUser user : other.users) {
            users.add(new SettingsUser(user));
        }
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

    public List<SettingsUser> getUsers() {
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

    public void addUser(SettingsUser user) {
        users.add(user);
    }

    public void removeUser(SettingsUser user) {
        users.remove(user);
    }

    public boolean hasUser(SettingsUser user) {
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

        for (int i = 0; i < users.size(); ++i) {
            SettingsUser user = users.get(i);
            SettingsUser otherUser = other.users.get(i);
            if (!Objects.equals(user.username, otherUser.username) || !Objects.equals(user.uuid, otherUser.uuid)) {
                return false;
            }
        }
        return true;
    }

    public boolean readFromNBT(CompoundTag nbt, int version, PlayerReferenceNbtReadContext context) {
        boolean changedDuringLoad = false;
        if (!special) {
            name = nbt.getStringOr("name", "");
            users.clear();
            ListTag usersTagList = nbt.getListOrEmpty("users");
            for (int i = 0; i < usersTagList.size(); ++i) {
                try {
                    SettingsUser user = new SettingsUser();
                    CompoundTag userTag = NbtReadHelper.requireCompound(usersTagList, i, "users");
                    changedDuringLoad |= user.readFromNBT(userTag, context);
                    users.add(user);
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
            for (SettingsUser user : users) {
                if (user.uuid == null) {
                    MapFrontiers.LOGGER.warn("Skipping user without UUID while saving settings group {}", name);
                    continue;
                }
                CompoundTag userTag = new CompoundTag();
                user.writeToNBT(userTag, resolver);
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
                SettingsUser user = new SettingsUser();
                user.fromBytes(buf);
                users.add(user);
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
            for (SettingsUser user : users) {
                user.toBytes(buf);
            }
        }

        for (FrontierSettings.Action action : FrontierSettings.Action.VALUES) {
            buf.writeBoolean(actions.contains(action));
        }
    }
}
