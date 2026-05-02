package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class CollectionData {
    public static final int MAX_NAME_CHARACTERS = FrontierData.MAX_NAME_CHARACTERS;

    protected UUID id;
    protected boolean personal;
    protected FrontierData.FrontierLifetime lifetime = FrontierData.FrontierLifetime.PERSISTENT;
    protected SettingsUser owner = new SettingsUser();
    protected String name = "";
    protected int color = ColorConstants.WHITE;
    protected @Nullable String sourcePluginId;
    protected @Nullable FrontierData.CopiedFrom copiedFrom;
    protected @Nullable Date created;
    protected @Nullable Date modified;

    public CollectionData() {
        id = new UUID(0, 0);
    }

    public CollectionData(CollectionData other) {
        id = other.id;
        personal = other.personal;
        lifetime = other.lifetime;
        owner = other.owner;
        name = other.name;
        color = other.color;
        sourcePluginId = other.sourcePluginId;
        copiedFrom = other.copiedFrom == null ? null : new FrontierData.CopiedFrom(other.copiedFrom);
        created = other.created;
        modified = other.modified;

        validateTypeAndLifetime(personal, lifetime);
    }

    public void updateFromData(CollectionData other) {
        if (other == this) {
            return;
        }

        id = other.id;
        personal = other.personal;
        lifetime = other.lifetime;
        owner = other.owner;
        name = other.name;
        color = other.color;
        sourcePluginId = other.sourcePluginId;
        copiedFrom = other.copiedFrom == null ? null : new FrontierData.CopiedFrom(other.copiedFrom);
        created = other.created;
        modified = other.modified;

        validateTypeAndLifetime(personal, lifetime);
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        personal = nbt.getBooleanOr("personal", true);
        lifetime = readLifetimeFromNbt(nbt);
        try {
            validateTypeAndLifetime(personal, lifetime);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Invalid lifetime for collection " + id + ": " + e.getMessage(), e);
        }
        owner = new SettingsUser();
        owner.readFromNBT(nbt.getCompoundOrEmpty("owner"));
        name = nbt.getStringOr("name", "");
        color = NbtReadHelper.requireInt(nbt, "color");
        sourcePluginId = nbt.getStringOr("sourcePluginId", null);

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
        nbt.putString("lifetime", lifetime.name());

        CompoundTag ownerTag = new CompoundTag();
        owner.writeToNBT(ownerTag);
        nbt.put("owner", ownerTag);

        nbt.putString("name", name);
        nbt.putInt("color", color);
        if (sourcePluginId != null) {
            nbt.putString("sourcePluginId", sourcePluginId);
        }

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
        lifetime = readLifetimeFromBytes(buf);
        validateTypeAndLifetime(personal, lifetime);
        owner = new SettingsUser();
        owner.fromBytes(buf);
        name = buf.readUtf(MAX_NAME_CHARACTERS);
        color = buf.readInt();
        if (buf.readBoolean()) {
            sourcePluginId = buf.readUtf();
        } else {
            sourcePluginId = null;
        }

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
        buf.writeInt(lifetime.ordinal());
        owner.toBytes(buf);
        buf.writeUtf(name, MAX_NAME_CHARACTERS);
        buf.writeInt(color);
        if (sourcePluginId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(sourcePluginId);
        }

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
        validateTypeAndLifetime(personal, lifetime);
        this.personal = personal;
    }

    public FrontierData.FrontierLifetime getLifetime() {
        return lifetime;
    }

    public void setLifetime(FrontierData.FrontierLifetime lifetime) {
        FrontierData.FrontierLifetime checkedLifetime = Objects.requireNonNull(lifetime, "lifetime");
        validateTypeAndLifetime(personal, checkedLifetime);
        this.lifetime = checkedLifetime;
    }

    public boolean isPersistent() {
        return lifetime == FrontierData.FrontierLifetime.PERSISTENT;
    }

    public boolean isSessionOnly() {
        return lifetime == FrontierData.FrontierLifetime.SESSION_ONLY;
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

    public void setSourcePluginId(@Nullable String sourcePluginId) {
        this.sourcePluginId = sourcePluginId;
    }

    public @Nullable String getSourcePluginId() {
        return sourcePluginId;
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

    private static void validateTypeAndLifetime(boolean personal, FrontierData.FrontierLifetime lifetime) {
        if (!personal && lifetime == FrontierData.FrontierLifetime.SESSION_ONLY) {
            throw new IllegalArgumentException("SESSION_ONLY collections must be personal");
        }
    }

    private static FrontierData.FrontierLifetime readLifetimeFromNbt(CompoundTag nbt) {
        String lifetimeTag = nbt.getStringOr("lifetime", "");
        if (lifetimeTag.isEmpty()) {
            return FrontierData.FrontierLifetime.PERSISTENT;
        }

        try {
            return FrontierData.FrontierLifetime.valueOf(lifetimeTag);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Unknown collection lifetime '" + lifetimeTag + "'", e);
        }
    }

    private static FrontierData.FrontierLifetime readLifetimeFromBytes(FriendlyByteBuf buf) {
        int lifetimeOrdinal = buf.readInt();
        if (lifetimeOrdinal < 0 || lifetimeOrdinal >= FrontierData.FrontierLifetime.VALUES.length) {
            MapFrontiers.LOGGER.warn("Unknown lifetime ordinal in collection packet. Found: {}. Defaulting to {}",
                    lifetimeOrdinal,
                    FrontierData.FrontierLifetime.PERSISTENT);
            return FrontierData.FrontierLifetime.PERSISTENT;
        }

        return FrontierData.FrontierLifetime.VALUES[lifetimeOrdinal];
    }
}
