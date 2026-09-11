package games.alejandrocoria.mapfrontiers.common.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtCodec;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.CopiedFromInfo;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
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
    protected PlayerId owner;
    protected String name = "";
    protected int color = ColorConstants.WHITE;
    protected CollectionVisibilityData visibilityData = new CollectionVisibilityData();
    protected @Nullable BannerData banner;
    protected @Nullable String sourcePluginId;
    protected @Nullable CopiedFromInfo copiedFrom;
    protected @Nullable Date created;
    protected @Nullable Date modified;
    private long collectionRevision;

    public record NbtReadResult(CollectionData collection, boolean changedDuringLoad) {
        public NbtReadResult {
            Objects.requireNonNull(collection, "collection");
        }
    }

    public CollectionData(PlayerId owner) {
        id = new UUID(0, 0);
        this.owner = Objects.requireNonNull(owner, "owner");
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
        collectionRevision = other.collectionRevision;

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
        collectionRevision = other.collectionRevision;

        validateTypeAndLifetime(personal, lifetime);
    }

    public static NbtReadResult readFromNBT(CompoundTag nbt, int version,
                                            PlayerReferenceNbtReadContext context) {
        PlayerReferenceNbtCodec.ReadResult ownerResult = PlayerReferenceNbtCodec.read(
                nbt.getCompoundOrEmpty("owner"), context);
        CollectionData collection = new CollectionData(ownerResult.playerId());
        boolean changedDuringLoad = ownerResult.repaired();
        collection.collectionRevision = 0L;
        collection.id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        collection.personal = nbt.getBooleanOr("personal", true);
        collection.lifetime = readLifetimeFromNbt(nbt);
        try {
            validateTypeAndLifetime(collection.personal, collection.lifetime);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Invalid lifetime for collection " + collection.id + ": " + e.getMessage(), e);
        }
        collection.name = nbt.getStringOr("name", "");
        collection.color = NbtReadHelper.requireInt(nbt, "color");
        collection.visibilityData = new CollectionVisibilityData();
        collection.visibilityData.readFromNBT(nbt.getCompoundOrEmpty("visibility"));
        if (nbt.contains("banner")) {
            collection.banner = new BannerData();
            changedDuringLoad |= collection.banner.readFromNBT(NbtReadHelper.requireCompound(nbt, "banner"));
        } else {
            collection.banner = null;
        }
        collection.setSourcePluginId(nbt.getStringOr("sourcePluginId", null));

        if (nbt.contains("copiedFrom")) {
            CompoundTag copiedFromTag = NbtReadHelper.requireCompound(nbt, "copiedFrom");
            CopiedFromInfo.NbtReadResult copiedResult = CopiedFromInfo.readFromNBT(copiedFromTag, context);
            collection.copiedFrom = copiedResult.copiedFrom();
            changedDuringLoad |= copiedResult.changedDuringLoad();
        } else {
            collection.copiedFrom = null;
        }

        if (nbt.contains("created")) {
            collection.created = new Date(NbtReadHelper.requireLong(nbt, "created"));
        } else {
            collection.created = null;
        }

        if (nbt.contains("modified")) {
            collection.modified = new Date(NbtReadHelper.requireLong(nbt, "modified"));
        } else {
            collection.modified = null;
        }

        return new NbtReadResult(collection, changedDuringLoad);
    }

    public void writeToNBT(CompoundTag nbt, PlayerNameResolver resolver) {
        assertSerializableLifetime();

        nbt.putString("id", id.toString());
        nbt.putBoolean("personal", personal);
        nbt.putString("lifetime", lifetime.name());

        CompoundTag ownerTag = new CompoundTag();
        PlayerReferenceNbtCodec.write(ownerTag, owner, resolver);
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
            copiedFrom.writeToNBT(copiedFromTag, resolver);
            nbt.put("copiedFrom", copiedFromTag);
        }

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public static CollectionData fromBytes(FriendlyByteBuf buf) {
        UUID id = UUIDHelper.fromBytes(buf);
        boolean personal = buf.readBoolean();
        TerritoryLifetime lifetime = readLifetimeFromBytes(buf);
        validateTypeAndLifetime(personal, lifetime);
        CollectionData collection = new CollectionData(PlayerIdNetworkCodec.read(buf));
        collection.id = id;
        collection.personal = personal;
        collection.lifetime = lifetime;
        collection.name = buf.readUtf(MAX_NAME_CHARACTERS);
        collection.color = buf.readInt();
        collection.visibilityData = new CollectionVisibilityData();
        collection.visibilityData.fromBytes(buf);
        if (buf.readBoolean()) {
            collection.banner = new BannerData();
            collection.banner.fromBytes(buf);
        } else {
            collection.banner = null;
        }
        collection.setSourcePluginId(buf.readBoolean() ? buf.readUtf() : null);

        if (buf.readBoolean()) {
            collection.copiedFrom = CopiedFromInfo.fromBytes(buf);
        } else {
            collection.copiedFrom = null;
        }

        if (buf.readBoolean()) {
            collection.created = new Date(buf.readLong());
        } else {
            collection.created = null;
        }

        if (buf.readBoolean()) {
            collection.modified = new Date(buf.readLong());
        } else {
            collection.modified = null;
        }
        collection.collectionRevision = buf.readLong();
        return collection;
    }

    public void toBytes(FriendlyByteBuf buf) {
        assertSerializableLifetime();

        UUIDHelper.toBytes(buf, id);
        buf.writeBoolean(personal);
        buf.writeInt(lifetime.ordinal());
        PlayerIdNetworkCodec.write(buf, owner);
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
        buf.writeLong(collectionRevision);
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

    public PlayerId getOwner() {
        return owner;
    }

    public void setOwner(PlayerId owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
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

    public long getCollectionRevision() {
        return collectionRevision;
    }

    public void setCollectionRevision(long collectionRevision) {
        this.collectionRevision = collectionRevision;
    }

    public void advanceCollectionRevision() {
        ++collectionRevision;
    }

    public boolean hasSameEditableState(CollectionData other) {
        return name.equals(other.name)
                && color == other.color
                && visibilityData.equals(other.visibilityData)
                && Objects.equals(banner, other.banner);
    }

    public boolean hasSameSynchronizedState(CollectionData other) {
        return id.equals(other.id)
                && personal == other.personal
                && lifetime == other.lifetime
                && owner.equals(other.owner)
                && hasSameEditableState(other)
                && Objects.equals(sourcePluginId, other.sourcePluginId)
                && copiedFromHasSameState(other)
                && Objects.equals(created, other.created)
                && Objects.equals(modified, other.modified)
                && collectionRevision == other.collectionRevision;
    }

    public boolean wasCopied() {
        return copiedFrom != null;
    }

    public void removeCopiedFromInfo() {
        copiedFrom = null;
    }

    public UUID getCopiedFromId() {
        if (copiedFrom == null) {
            return id;
        }
        return copiedFrom.getId();
    }

    public void setCopiedFrom(UUID id, @Nullable PlayerId user) {
        copiedFrom = new CopiedFromInfo(id, user);
    }

    public @Nullable PlayerId getCopiedFromUser() {
        if (copiedFrom == null) {
            return owner;
        }
        return copiedFrom.getUser();
    }

    private boolean copiedFromHasSameState(CollectionData other) {
        if (wasCopied() != other.wasCopied()) {
            return false;
        }
        return !wasCopied() || getCopiedFromId().equals(other.getCopiedFromId())
                && Objects.equals(getCopiedFromUser(), other.getCopiedFromUser());
    }

    private static void validateTypeAndLifetime(boolean personal, TerritoryLifetime lifetime) {
        if (!personal && lifetime == TerritoryLifetime.SESSION_ONLY) {
            throw new IllegalArgumentException("SESSION_ONLY collections must be personal");
        }
    }

    private static TerritoryLifetime readLifetimeFromNbt(CompoundTag nbt) {
        String lifetimeTag = nbt.getStringOr("lifetime", "");
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
