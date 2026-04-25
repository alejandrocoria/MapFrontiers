package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class CollectionData {
    public static final int MAX_NAME_CHARACTERS = FrontierData.MAX_NAME_CHARACTERS;

    protected UUID id;
    protected boolean personal;
    protected SettingsUser owner = new SettingsUser();
    protected String name = "";
    protected int color = ColorConstants.WHITE;
    protected @Nullable FrontierData.CopiedFrom copiedFrom;
    protected @Nullable Date created;
    protected @Nullable Date modified;

    public CollectionData() {
        id = new UUID(0, 0);
    }

    public CollectionData(CollectionData other) {
        id = other.id;
        personal = other.personal;
        owner = other.owner;
        name = other.name;
        color = other.color;
        copiedFrom = other.copiedFrom == null ? null : new FrontierData.CopiedFrom(other.copiedFrom);
        created = other.created;
        modified = other.modified;
    }

    public void updateFromData(CollectionData other) {
        if (other == this) {
            return;
        }

        id = other.id;
        personal = other.personal;
        owner = other.owner;
        name = other.name;
        color = other.color;
        copiedFrom = other.copiedFrom == null ? null : new FrontierData.CopiedFrom(other.copiedFrom);
        created = other.created;
        modified = other.modified;
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        personal = nbt.getBooleanOr("personal", true);
        owner = new SettingsUser();
        owner.readFromNBT(nbt.getCompoundOrEmpty("owner"));
        name = nbt.getStringOr("name", "");
        color = NbtReadHelper.requireInt(nbt, "color");

        if (nbt.contains("copiedFrom")) {
            copiedFrom = new FrontierData.CopiedFrom();
            copiedFrom.readFromNBT(NbtReadHelper.requireCompound(nbt, "copiedFrom"), version);
        } else {
            copiedFrom = null;
        }

        if (nbt.contains("created")) {
            created = new Date(NbtReadHelper.requireLong(nbt, "created"));
        } else {
            created = null;
        }

        if (nbt.contains("modified")) {
            modified = new Date(NbtReadHelper.requireLong(nbt, "modified"));
        } else {
            modified = null;
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("id", id.toString());
        nbt.putBoolean("personal", personal);

        CompoundTag ownerTag = new CompoundTag();
        owner.writeToNBT(ownerTag);
        nbt.put("owner", ownerTag);

        nbt.putString("name", name);
        nbt.putInt("color", color);

        if (copiedFrom != null) {
            CompoundTag copiedFromTag = new CompoundTag();
            copiedFrom.writeToNBT(copiedFromTag);
            nbt.put("copiedFrom", copiedFromTag);
        }

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        id = UUIDHelper.fromBytes(buf);
        personal = buf.readBoolean();
        owner = new SettingsUser();
        owner.fromBytes(buf);
        name = buf.readUtf(MAX_NAME_CHARACTERS);
        color = buf.readInt();

        if (buf.readBoolean()) {
            copiedFrom = new FrontierData.CopiedFrom();
            copiedFrom.fromBytes(buf);
        } else {
            copiedFrom = null;
        }

        if (buf.readBoolean()) {
            created = new Date(buf.readLong());
        } else {
            created = null;
        }

        if (buf.readBoolean()) {
            modified = new Date(buf.readLong());
        } else {
            modified = null;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, id);
        buf.writeBoolean(personal);
        owner.toBytes(buf);
        buf.writeUtf(name, MAX_NAME_CHARACTERS);
        buf.writeInt(color);

        if (copiedFrom == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            copiedFrom.toBytes(buf);
        }

        if (created == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(created.getTime());
        }

        if (modified == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(modified.getTime());
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean getPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public SettingsUser getOwner() {
        return owner;
    }

    public void setOwner(SettingsUser owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setCreated(Date created) {
        this.created = created;
        modified = created;
    }

    public @Nullable Date getCreated() {
        return created;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public @Nullable Date getModified() {
        return modified;
    }

    public boolean wasCopied() {
        return copiedFrom != null;
    }

    public void removeCopiedFromInfo() {
        copiedFrom = null;
    }

    public void setCopiedFromId(UUID id) {
        if (copiedFrom == null) {
            copiedFrom = new FrontierData.CopiedFrom();
        }
        copiedFrom.id = id;
    }

    public UUID getCopiedFromId() {
        if (copiedFrom == null) {
            return id;
        }
        return copiedFrom.id;
    }

    public void setCopiedFromUser(SettingsUser user) {
        if (copiedFrom == null) {
            copiedFrom = new FrontierData.CopiedFrom();
        }
        copiedFrom.user = user;
    }

    public SettingsUser getCopiedFromUser() {
        if (copiedFrom == null) {
            return owner;
        }
        return copiedFrom.user;
    }
}
