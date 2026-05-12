package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.UUID;

public class CopiedFromInfo {
    protected UUID id;
    protected SettingsUser user = new SettingsUser();

    public CopiedFromInfo() {
    }

    public CopiedFromInfo(CopiedFromInfo other) {
        id = other.id;
        user = other.user;
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));

        user = new SettingsUser();
        user.readFromNBT(nbt.getCompoundOrEmpty("user"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user);
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("id", id.toString());

        CompoundTag nbtOwner = new CompoundTag();
        user.writeToNBT(nbtOwner);
        nbt.put("user", nbtOwner);
    }

    public void fromBytes(FriendlyByteBuf buf) {
        id = UUIDHelper.fromBytes(buf);

        user = new SettingsUser();
        user.fromBytes(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, id);

        user.toBytes(buf);
    }
}
