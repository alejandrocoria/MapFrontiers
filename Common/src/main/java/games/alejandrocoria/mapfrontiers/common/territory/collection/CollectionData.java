package games.alejandrocoria.mapfrontiers.common.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.CopiedFromInfo;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.SourcePluginIdHelper;
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
    public static final int MAX_NAME_CHARACTERS = 48;
    protected UUID id;
    protected boolean personal;
    protected TerritoryLifetime lifetime = TerritoryLifetime.PERSISTENT;
    protected SettingsUser owner = new SettingsUser();
    protected String name = "";
    protected int color = ColorConstants.WHITE;
    protected CollectionVisibilityData visibilityData = new CollectionVisibilityData();
    protected @Nullable BannerData banner;
    protected @Nullable String sourcePluginId;
    protected @Nullable CopiedFromInfo copiedFrom;
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
        visibilityData = new CollectionVisibilityData(other.visibilityData);
        banner = other.banner == null ? null : new BannerData(other.banner);
        sourcePluginId = other.sourcePluginId;
        copiedFrom = other.copiedFrom == null ? null : new CopiedFromInfo(other.copiedFrom);
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
        visibilityData = new CollectionVisibilityData(other.visibilityData);
        banner = other.banner == null ? null : new BannerData(other.banner);
        sourcePluginId = other.sourcePluginId;
        copiedFrom = other.copiedFrom == null ? null : new CopiedFromInfo(other.copiedFrom);
        created = other.created;
        modified = other.modified;

        validateTypeAndLifetime(personal, lifetime);
    }

    public boolean readFromNBT(CompoundTag nbt, int version) {
        boolean changedDuringLoad = false;
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        personal = NbtCompat.getBooleanOr(nbt, "personal", true);
        lifetime = readLifetimeFromNbt(nbt);
        try {
            validateTypeAndLifetime(personal, lifetime);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Invalid lifetime for collection " + id + ": " + e.getMessage(), e);
        }
        owner = new SettingsUser();
        owner.readFromNBT(NbtCompat.getCompoundOrEmpty(nbt, "owner"));
        name = NbtCompat.getStringOr(nbt, "name", "");
        color = NbtReadHelper.requireInt(nbt, "color");
        visibilityData = new CollectionVisibilityData();
        visibilityData.readFromNBT(NbtCompat.getCompoundOrEmpty(nbt, "visibility"));
        if (nbt.contains("banner")) {
            banner = new BannerData();
            changedDuringLoad |= banner.readFromNBT(NbtReadHelper.requireCompound(nbt, "banner"));
        } else {
            banner = null;
        }
        setSourcePluginId(NbtCompat.getStringOr(nbt, "sourcePluginId", null));

        if (nbt.contains("copiedFrom")) {
            copiedFrom = new CopiedFromInfo();
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

        return changedDuringLoad;
    }

    public void writeToNBT(CompoundTag nbt) {
        assertSerializableLifetime();

        nbt.putString("id", id.toString());
        nbt.putBoolean("personal", personal);
        nbt.putString("lifetime", lifetime.name());

        CompoundTag ownerTag = new CompoundTag();
        owner.writeToNBT(ownerTag);
        nbt.put("owner", ownerTag);

        nbt.putString("name", name);
        nbt.putInt("color", color);
        CompoundTag visibilityTag = new CompoundTag();
        visibilityData.writeToNBT(visibilityTag);
        nbt.put("visibility", visibilityTag);
        if (banner != null) {
            CompoundTag bannerTag = new CompoundTag();
            banner.writeToNBT(bannerTag);
            nbt.put("banner", bannerTag);
        }
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
        visibilityData = new CollectionVisibilityData();
        visibilityData.fromBytes(buf);
        if (buf.readBoolean()) {
            banner = new BannerData();
            banner.fromBytes(buf);
        } else {
            banner = null;
        }
        setSourcePluginId(buf.readBoolean() ? buf.readUtf() : null);

        if (buf.readBoolean()) {
            copiedFrom = new CopiedFromInfo();
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
        assertSerializableLifetime();

        UUIDHelper.toBytes(buf, id);
        buf.writeBoolean(personal);
        buf.writeInt(lifetime.ordinal());
        owner.toBytes(buf);
        buf.writeUtf(name, MAX_NAME_CHARACTERS);
        buf.writeInt(color);
        visibilityData.toBytes(buf);
        if (banner == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            banner.toBytes(buf);
        }
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

    public TerritoryLifetime getLifetime() {
        return lifetime;
    }

    public void setLifetime(TerritoryLifetime lifetime) {
        TerritoryLifetime checkedLifetime = Objects.requireNonNull(lifetime, "lifetime");
        validateTypeAndLifetime(personal, checkedLifetime);
        this.lifetime = checkedLifetime;
    }

    public boolean isPersistent() {
        return lifetime == TerritoryLifetime.PERSISTENT;
    }

    public boolean isSessionOnly() {
        return lifetime == TerritoryLifetime.SESSION_ONLY;
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

    public boolean hasBanner() {
        return banner != null;
    }

    public void setBannerData(@Nullable BannerData bannerData) {
        banner = bannerData == null ? null : new BannerData(bannerData);
    }

    public @Nullable BannerData getBannerData() {
        return banner;
    }

    public void setBannerRotation(int rotation) {
        if (banner != null) {
            banner.rotation = rotation;
        }
    }

    public int getBannerRotation() {
        return banner == null ? 0 : banner.rotation;
    }

    public void setVisibilityData(CollectionVisibilityData visibilityData) {
        this.visibilityData = new CollectionVisibilityData(visibilityData);
    }

    public CollectionVisibilityData getVisibilityData() {
        return visibilityData;
    }

    public void setSourcePluginId(@Nullable String sourcePluginId) {
        this.sourcePluginId = SourcePluginIdHelper.normalize(sourcePluginId);
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
            copiedFrom = new CopiedFromInfo();
        }
        copiedFrom.setId(id);
    }

    public UUID getCopiedFromId() {
        if (copiedFrom == null) {
            return id;
        }
        return copiedFrom.getId();
    }

    public void setCopiedFromUser(SettingsUser user) {
        if (copiedFrom == null) {
            copiedFrom = new CopiedFromInfo();
        }
        copiedFrom.setUser(user);
    }

    public SettingsUser getCopiedFromUser() {
        if (copiedFrom == null) {
            return owner;
        }
        return copiedFrom.getUser();
    }

    private static void validateTypeAndLifetime(boolean personal, TerritoryLifetime lifetime) {
        if (!personal && lifetime == TerritoryLifetime.SESSION_ONLY) {
            throw new IllegalArgumentException("SESSION_ONLY collections must be personal");
        }
    }

    private static TerritoryLifetime readLifetimeFromNbt(CompoundTag nbt) {
        String lifetimeTag = NbtCompat.getStringOr(nbt, "lifetime", "");
        if (lifetimeTag.isEmpty()) {
            return TerritoryLifetime.PERSISTENT;
        }

        try {
            return TerritoryLifetime.valueOf(lifetimeTag);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Unknown collection lifetime '" + lifetimeTag + "'", e);
        }
    }

    private static TerritoryLifetime readLifetimeFromBytes(FriendlyByteBuf buf) {
        int lifetimeOrdinal = buf.readInt();
        if (lifetimeOrdinal < 0 || lifetimeOrdinal >= TerritoryLifetime.VALUES.length) {
            MapFrontiers.LOGGER.warn("Unknown lifetime ordinal in collection packet. Found: {}. Defaulting to {}",
                    lifetimeOrdinal,
                    TerritoryLifetime.PERSISTENT);
            return TerritoryLifetime.PERSISTENT;
        }

        return TerritoryLifetime.VALUES[lifetimeOrdinal];
    }

    private void assertSerializableLifetime() {
        if (isSessionOnly()) {
            throw new IllegalStateException("Cannot serialize SESSION_ONLY collection. id=" + id + ", personal=" + personal + ", lifetime=" + lifetime);
        }
    }
}
