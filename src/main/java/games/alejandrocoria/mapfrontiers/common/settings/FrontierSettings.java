package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

@ParametersAreNonnullByDefault
public class FrontierSettings {
    public enum Action {
        CreateFrontier, DeleteFrontier, UpdateFrontier, UpdateSettings
    }

    private SettingsGroup OPs;
    private SettingsGroup owners;
    private SettingsGroup everyone;
    private List<SettingsGroup> customGroups;
    private int changeCounter = 1;

    private static final int dataVersion = 1;

    public FrontierSettings() {
        OPs = new SettingsGroup("OPs");
        owners = new SettingsGroup("Owner");
        everyone = new SettingsGroup("Everyone");
        customGroups = new ArrayList<SettingsGroup>();
    }

    public void resetToDefault() {
        OPs.addAction(Action.CreateFrontier);
        OPs.addAction(Action.DeleteFrontier);
        OPs.addAction(Action.UpdateFrontier);
        OPs.addAction(Action.UpdateSettings);

        owners.addAction(Action.DeleteFrontier);
        owners.addAction(Action.UpdateFrontier);
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

    public boolean checkAction(Action action, SettingsUser player, boolean isOP, SettingsUser owner) {
        if (player == null) {
            return false;
        }

        if (isOP && OPs.hasAction(action)) {
            return true;
        }

        if (owner != null && player == owner && owners.hasAction(action)) {
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
        OPs.readFromNBT(OPsTag, false);

        NBTTagCompound ownersTag = nbt.getCompoundTag("Owners");
        owners.readFromNBT(ownersTag, false);

        NBTTagCompound everyoneTag = nbt.getCompoundTag("Everyone");
        everyone.readFromNBT(everyoneTag, false);

        customGroups.clear();
        NBTTagList customGroupsTagList = nbt.getTagList("customGroups", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < customGroupsTagList.tagCount(); ++i) {
            SettingsGroup group = new SettingsGroup();
            NBTTagCompound groupTag = customGroupsTagList.getCompoundTagAt(i);
            group.readFromNBT(groupTag);
            customGroups.add(group);
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound OPsTag = new NBTTagCompound();
        OPs.writeToNBT(OPsTag, false);
        nbt.setTag("OPs", OPsTag);

        NBTTagCompound ownersTag = new NBTTagCompound();
        owners.writeToNBT(ownersTag, false);
        nbt.setTag("Owners", ownersTag);

        NBTTagCompound everyoneTag = new NBTTagCompound();
        everyone.writeToNBT(everyoneTag, false);
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

    public static List<Action> getAvailableActions(String groupName) {
        List<Action> actions = new ArrayList<Action>();

        if (!groupName.contentEquals("Owner")) {
            actions.add(Action.CreateFrontier);
            actions.add(Action.UpdateSettings);
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
}
