package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraftforge.common.util.Constants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;

@ParametersAreNonnullByDefault
public class FrontierSettings {
    public enum Action {
        CreateFrontier, DeleteFrontier, UpdateFrontier, UpdateSettings, PersonalFrontier;

        public final static Action[] valuesArray = values();
    }

    private final SettingsGroup OPs;
    private final SettingsGroup owners;
    private final SettingsGroup everyone;
    private List<SettingsGroup> customGroups;
    private int changeCounter = 1;

    private static final int dataVersion = 3;

    public FrontierSettings() {
        OPs = new SettingsGroup("OPs", true);
        owners = new SettingsGroup("Owner", true);
        everyone = new SettingsGroup("Everyone", true);
        customGroups = new ArrayList<>();
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

        if (player.equals(owner) && owners.hasAction(action)) {
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

    public SettingsProfile getProfile(PlayerEntity player) {
        SettingsProfile profile = new SettingsProfile();
        SettingsUser user = new SettingsUser(player);

        for (Action action : owners.getActions()) {
            profile.setAction(action, SettingsProfile.State.Owner);
        }

        if (MapFrontiers.isOPorHost(player)) {
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

    public void readFromNBT(CompoundNBT nbt) {
        int version = nbt.getInt("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in settings not found, expected " + dataVersion);
        } else if (version < 3) {
            MapFrontiers.LOGGER.warn("Data version in settings lower than expected. The mod uses " + dataVersion);
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER
                    .warn("Data version in settings higher than expected. The mod uses " + dataVersion);
        }

        CompoundNBT OPsTag = nbt.getCompound("OPs");
        OPs.readFromNBT(OPsTag);

        CompoundNBT ownersTag = nbt.getCompound("Owners");
        owners.readFromNBT(ownersTag);

        CompoundNBT everyoneTag = nbt.getCompound("Everyone");
        everyone.readFromNBT(everyoneTag);

        customGroups.clear();
        ListNBT customGroupsTagList = nbt.getList("customGroups", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < customGroupsTagList.size(); ++i) {
            SettingsGroup group = new SettingsGroup();
            CompoundNBT groupTag = customGroupsTagList.getCompound(i);
            group.readFromNBT(groupTag);
            customGroups.add(group);
        }

        ensureUpdateSettingsAction();
    }

    public void writeToNBT(CompoundNBT nbt) {
        CompoundNBT OPsTag = new CompoundNBT();
        OPs.writeToNBT(OPsTag);
        nbt.put("OPs", OPsTag);

        CompoundNBT ownersTag = new CompoundNBT();
        owners.writeToNBT(ownersTag);
        nbt.put("Owners", ownersTag);

        CompoundNBT everyoneTag = new CompoundNBT();
        everyone.writeToNBT(everyoneTag);
        nbt.put("Everyone", everyoneTag);

        ListNBT customGroupsTagList = new ListNBT();
        for (SettingsGroup group : customGroups) {
            CompoundNBT groupTag = new CompoundNBT();
            group.writeToNBT(groupTag);
            customGroupsTagList.add(groupTag);
        }
        nbt.put("customGroups", customGroupsTagList);

        nbt.putInt("Version", dataVersion);
    }

    public void fromBytes(PacketBuffer buf) {
        OPs.fromBytes(buf);
        owners.fromBytes(buf);
        everyone.fromBytes(buf);

        customGroups = new ArrayList<>();
        int groupsCount = buf.readInt();
        for (int i = 0; i < groupsCount; ++i) {
            SettingsGroup group = new SettingsGroup();
            group.fromBytes(buf);
            customGroups.add(group);
        }
    }

    public void toBytes(PacketBuffer buf) {
        OPs.toBytes(buf);
        owners.toBytes(buf);
        everyone.toBytes(buf);

        buf.writeInt(customGroups.size());
        for (SettingsGroup group : customGroups) {
            group.toBytes(buf);
        }
    }

    public static List<Action> getAvailableActions(String groupName) {
        List<Action> actions = new ArrayList<>();

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
