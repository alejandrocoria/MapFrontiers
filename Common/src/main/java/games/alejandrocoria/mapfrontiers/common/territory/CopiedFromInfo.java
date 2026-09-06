package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.UUID;

public class CopiedFromInfo {
    private UUID id;
    private SettingsUser user = new SettingsUser();

    public CopiedFromInfo() {
    }

    public CopiedFromInfo(CopiedFromInfo other) {
        id = other.id;
        user = other.user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SettingsUser getUser() {
        return user;
    }

    public void setUser(SettingsUser user) {
        this.user = user;
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));

        user = new SettingsUser();
        user.readFromNBT(nbt.getCompoundOrEmpty("user"));
    }

    public boolean readFromNBT(CompoundTag nbt, int version, PlayerReferenceNbtReadContext context) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));

        user = new SettingsUser();
        if (!nbt.contains("user")) {
            return false;
        }
        return user.readFromNBT(NbtReadHelper.requireCompound(nbt, "user"), context);
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

    public void writeToNBT(CompoundTag nbt, PlayerNameResolver resolver) {
        nbt.putString("id", id.toString());
        nbt.remove("user");

        if (user.uuid != null) {
            CompoundTag nbtOwner = new CompoundTag();
            user.writeToNBT(nbtOwner, resolver);
            nbt.put("user", nbtOwner);
        }
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
