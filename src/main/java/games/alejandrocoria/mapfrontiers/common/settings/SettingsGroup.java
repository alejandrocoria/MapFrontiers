package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;

@ParametersAreNonnullByDefault
public class SettingsGroup {
    private String name;
    private List<SettingsUser> users;
    private Set<FrontierSettings.Action> actions;
    private boolean special;

    public SettingsGroup() {
        name = "";
        users = new ArrayList<SettingsUser>();
        actions = EnumSet.noneOf(FrontierSettings.Action.class);
        special = false;
    }

    public SettingsGroup(String name, boolean special) {
        this.name = name;
        users = new ArrayList<SettingsUser>();
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

    public void readFromNBT(NBTTagCompound nbt) {
        if (!special) {
            name = nbt.getString("name");
            users.clear();
            NBTTagList usersTagList = nbt.getTagList("users", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < usersTagList.tagCount(); ++i) {
                SettingsUser user = new SettingsUser();
                NBTTagCompound userTag = usersTagList.getCompoundTagAt(i);
                user.readFromNBT(userTag);
                users.add(user);
            }
        }

        actions.clear();
        NBTTagList actionsTagList = nbt.getTagList("actions", Constants.NBT.TAG_STRING);
        for (int i = 0; i < actionsTagList.tagCount(); ++i) {
            String actionTag = actionsTagList.getStringTagAt(i);
            List<FrontierSettings.Action> availableActions = FrontierSettings.getAvailableActions(name);

            try {
                FrontierSettings.Action action = FrontierSettings.Action.valueOf(actionTag);
                if (!availableActions.contains(action)) {
                    throw new IllegalArgumentException();
                }
                actions.add(action);
            } catch (IllegalArgumentException e) {
                String availableActionsString = availableActions.get(0).name();
                for (int i2 = 1; i2 < availableActions.size() - 1; ++i2) {
                    availableActionsString += ", ";
                    availableActionsString += availableActions.get(i2).name();
                }

                availableActionsString += " or ";
                availableActionsString += availableActions.get(availableActions.size() - 1).name();

                MapFrontiers.LOGGER.warn(String.format("Unknown action in group %1$s. Found: \"%2$s\". Expected: %3$s", name,
                        actionTag, availableActionsString));
            }
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        if (!special) {
            nbt.setString("name", name);
            NBTTagList usersTagList = new NBTTagList();
            for (SettingsUser user : users) {
                NBTTagCompound userTag = new NBTTagCompound();
                user.writeToNBT(userTag);
                usersTagList.appendTag(userTag);
            }

            nbt.setTag("users", usersTagList);
        }

        NBTTagList actionsTagList = new NBTTagList();
        for (FrontierSettings.Action action : actions) {
            NBTTagString actionTag = new NBTTagString(action.name());
            actionsTagList.appendTag(actionTag);
        }

        nbt.setTag("actions", actionsTagList);
    }

    public void fromBytes(ByteBuf buf) {
        if (!buf.readBoolean()) {
            name = ByteBufUtils.readUTF8String(buf);

            users = new ArrayList<SettingsUser>();
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

    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(special);

        if (!special) {
            ByteBufUtils.writeUTF8String(buf, name);

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
