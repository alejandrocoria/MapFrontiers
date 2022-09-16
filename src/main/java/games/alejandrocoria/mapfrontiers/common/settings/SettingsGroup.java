package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

    public void readFromNBT(CompoundNBT nbt, int version) {
        if (!special) {
            name = nbt.getString("name");
            users.clear();
            ListNBT usersTagList = nbt.getList("users", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < usersTagList.size(); ++i) {
                SettingsUser user = new SettingsUser();
                CompoundNBT userTag = usersTagList.getCompound(i);
                user.readFromNBT(userTag);
                users.add(user);
            }
        }

        actions.clear();
        ListNBT actionsTagList = nbt.getList("actions", Constants.NBT.TAG_STRING);
        for (int i = 0; i < actionsTagList.size(); ++i) {
            String actionTag = actionsTagList.getString(i);
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

                MapFrontiers.LOGGER.warn(String.format("Unknown action in group %1$s. Found: \"%2$s\". Expected: %3$s", name,
                        actionTag, availableActionsString));
            }
        }
    }

    public void writeToNBT(CompoundNBT nbt) {
        if (!special) {
            nbt.putString("name", name);
            ListNBT usersTagList = new ListNBT();
            for (SettingsUser user : users) {
                CompoundNBT userTag = new CompoundNBT();
                user.writeToNBT(userTag);
                usersTagList.add(userTag);
            }

            nbt.put("users", usersTagList);
        }

        ListNBT actionsTagList = new ListNBT();
        for (FrontierSettings.Action action : actions) {
            StringNBT actionTag = StringNBT.valueOf(action.name());
            actionsTagList.add(actionTag);
        }

        nbt.put("actions", actionsTagList);
    }

    public void fromBytes(PacketBuffer buf) {
        if (!buf.readBoolean()) {
            name = buf.readUtf(17);

            users = new ArrayList<>();
            int usersCount = buf.readInt();
            for (int i = 0; i < usersCount; ++i) {
                SettingsUser user = new SettingsUser();
                user.fromBytes(buf);
                users.add(user);
            }
        }

        actions.clear();
        for (FrontierSettings.Action action : FrontierSettings.Action.valuesArray) {
            if (buf.readBoolean()) {
                actions.add(action);
            }
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeBoolean(special);

        if (!special) {
            buf.writeUtf(name, 17);

            buf.writeInt(users.size());
            for (SettingsUser user : users) {
                user.toBytes(buf);
            }
        }

        for (FrontierSettings.Action action : FrontierSettings.Action.valuesArray) {
            buf.writeBoolean(actions.contains(action));
        }
    }
}
