package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

@ParametersAreNonnullByDefault
public class FrontierSettings {
    public enum Action {
        CreateFrontier, DeleteFrontier, UpdateFrontier, UpdateSettings, PersonalFrontier;

        public final static Action[] valuesArray = values();
    }

    private SettingsGroup OPs;
    private SettingsGroup owners;
    private SettingsGroup everyone;
    private List<SettingsGroup> customGroups;
    private int changeCounter = 1;

    private static final int dataVersion = 2;

    public FrontierSettings() {
        OPs = new SettingsGroup("OPs", true);
        owners = new SettingsGroup("Owner", true);
        everyone = new SettingsGroup("Everyone", true);
        customGroups = new ArrayList<SettingsGroup>();
    }

    public void resetToDefault() {
        OPs.addAction(Action.CreateFrontier);
        OPs.addAction(Action.DeleteFrontier);
        OPs.addAction(Action.UpdateFrontier);
        OPs.addAction(Action.UpdateSettings);

        owners.addAction(Action.DeleteFrontier);
        owners.addAction(Action.UpdateFrontier);

        everyone.addAction(Action.PersonalFrontier);
    }

    public SettingsGroup getOPsGroup() {
        return OPs;
    }

    public SettingsGroup getOwnersGroup() {
        return owners;
    }

    public SettingsGroup getEveryoneGroup() {
        return everyone;
    }

    public List<SettingsGroup> getCustomGroups() {
        return customGroups;
    }

    public SettingsGroup createCustomGroup(String name) {
        SettingsGroup group = new SettingsGroup(name, false);
        customGroups.add(group);
        return group;
    }

    public void removeCustomGroup(SettingsGroup group) {
        customGroups.remove(group);
    }

    public boolean checkAction(Action action, @Nullable SettingsUser player, boolean isOP, @Nullable SettingsUser owner) {
        if (player == null) {
            return false;
        }

        if (isOP && OPs.hasAction(action)) {
            return true;
        }

        if (owner != null && player.equals(owner) && owners.hasAction(action)) {
            return true;
        }

        if (everyone.hasAction(action)) {
            return true;
        }

        for (SettingsGroup group : customGroups) {
            if (group.hasAction(action) && group.hasUser(player)) {
                return true;
            }
        }

        return false;
    }

    public SettingsProfile getProfile(EntityPlayer player) {
        SettingsProfile profile = new SettingsProfile();
        SettingsUser user = new SettingsUser(player);

        for (Action action : owners.getActions()) {
            profile.setAction(action, SettingsProfile.State.Owner);
        }

        if (MapFrontiers.proxy.isOPorHost(player)) {
            for (Action action : OPs.getActions()) {
                profile.setAction(action, SettingsProfile.State.Enabled);
            }
        }

        for (Action action : everyone.getActions()) {
            profile.setAction(action, SettingsProfile.State.Enabled);
        }

        if (profile.isAllEnabled()) {
            return profile;
        }

        for (SettingsGroup group : customGroups) {
            if (group.hasUser(user)) {
                for (Action action : group.getActions()) {
                    profile.setAction(action, SettingsProfile.State.Enabled);
                }
            }
        }

        return profile;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        int version = nbt.getInteger("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in settings not found, expected " + String.valueOf(dataVersion));
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER
                    .warn("Data version in settings higher than expected. The mod uses " + String.valueOf(dataVersion));
        }

        NBTTagCompound OPsTag = nbt.getCompoundTag("OPs");
        OPs.readFromNBT(OPsTag);

        NBTTagCompound ownersTag = nbt.getCompoundTag("Owners");
        owners.readFromNBT(ownersTag);

        NBTTagCompound everyoneTag = nbt.getCompoundTag("Everyone");
        everyone.readFromNBT(everyoneTag);

        customGroups.clear();
        NBTTagList customGroupsTagList = nbt.getTagList("customGroups", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < customGroupsTagList.tagCount(); ++i) {
            SettingsGroup group = new SettingsGroup();
            NBTTagCompound groupTag = customGroupsTagList.getCompoundTagAt(i);
            group.readFromNBT(groupTag);
            customGroups.add(group);
        }

        if (version < 2) {
            everyone.addAction(Action.PersonalFrontier);
        }

        ensureUpdateSettingsAction();
    }

    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound OPsTag = new NBTTagCompound();
        OPs.writeToNBT(OPsTag);
        nbt.setTag("OPs", OPsTag);

        NBTTagCompound ownersTag = new NBTTagCompound();
        owners.writeToNBT(ownersTag);
        nbt.setTag("Owners", ownersTag);

        NBTTagCompound everyoneTag = new NBTTagCompound();
        everyone.writeToNBT(everyoneTag);
        nbt.setTag("Everyone", everyoneTag);

        NBTTagList customGroupsTagList = new NBTTagList();
        for (SettingsGroup group : customGroups) {
            NBTTagCompound groupTag = new NBTTagCompound();
            group.writeToNBT(groupTag);
            customGroupsTagList.appendTag(groupTag);
        }
        nbt.setTag("customGroups", customGroupsTagList);

        nbt.setInteger("Version", dataVersion);
    }

    public void fromBytes(ByteBuf buf) {
        OPs.fromBytes(buf);
        owners.fromBytes(buf);
        everyone.fromBytes(buf);

        customGroups = new ArrayList<SettingsGroup>();
        int groupsCount = buf.readInt();
        for (int i = 0; i < groupsCount; ++i) {
            SettingsGroup group = new SettingsGroup();
            group.fromBytes(buf);
            customGroups.add(group);
        }
    }

    public void toBytes(ByteBuf buf) {
        OPs.toBytes(buf);
        owners.toBytes(buf);
        everyone.toBytes(buf);

        buf.writeInt(customGroups.size());
        for (SettingsGroup group : customGroups) {
            group.toBytes(buf);
        }
    }

    public static List<Action> getAvailableActions(String groupName) {
        List<Action> actions = new ArrayList<Action>();

        if (!groupName.contentEquals("Owner")) {
            actions.add(Action.CreateFrontier);
            actions.add(Action.UpdateSettings);
            actions.add(Action.PersonalFrontier);
        }

        actions.add(Action.DeleteFrontier);
        actions.add(Action.UpdateFrontier);

        return actions;
    }

    public void setChangeCounter(int changeCounter) {
        this.changeCounter = changeCounter;
    }

    public int getChangeCounter() {
        return changeCounter;
    }

    public void advanceChangeCounter() {
        ++changeCounter;
    }

    private void ensureUpdateSettingsAction() {
        if (OPs.hasAction(Action.UpdateSettings) || owners.hasAction(Action.UpdateSettings)
                || everyone.hasAction(Action.UpdateSettings)) {
            return;
        }

        for (SettingsGroup group : customGroups) {
            if (group.hasAction(Action.UpdateSettings)) {
                return;
            }
        }

        OPs.addAction(Action.UpdateSettings);
    }
}
