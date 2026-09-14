package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtCodec;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class CopiedFromInfo {
    public record NbtReadResult(CopiedFromInfo copiedFrom, boolean changedDuringLoad) {
        public NbtReadResult {
            Objects.requireNonNull(copiedFrom, "copiedFrom");
        }
    }

    private final UUID id;
    private final @Nullable PlayerId user;

    public CopiedFromInfo(UUID id) {
        this(id, null);
    }

    public CopiedFromInfo(UUID id, @Nullable PlayerId user) {
        this.id = Objects.requireNonNull(id, "id");
        this.user = user;
    }

    public CopiedFromInfo(CopiedFromInfo other) {
        id = other.id;
        user = other.user;
    }

    public UUID getId() {
        return id;
    }

    public @Nullable PlayerId getUser() {
        return user;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof CopiedFromInfo info
                && id.equals(info.id)
                && Objects.equals(user, info.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user);
    }

    public static NbtReadResult readFromNBT(CompoundTag nbt, PlayerReferenceNbtReadContext context) {
        UUID id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        if (!nbt.contains("user")) {
            return new NbtReadResult(new CopiedFromInfo(id), false);
        }

        try {
            PlayerReferenceNbtCodec.ReadResult result = PlayerReferenceNbtCodec.read(
                    NbtReadHelper.requireCompound(nbt, "user"), context);
            return new NbtReadResult(new CopiedFromInfo(id, result.playerId()), result.repaired());
        } catch (InvalidNbtFormatException e) {
            MapFrontiers.LOGGER.warn("Ignoring invalid copied-from user for territory {}: {}", id, e.getMessage());
            return new NbtReadResult(new CopiedFromInfo(id), true);
        }
    }

    public void writeToNBT(CompoundTag nbt, PlayerNameResolver resolver) {
        nbt.putString("id", id.toString());
        nbt.remove("user");
        if (user != null) {
            CompoundTag userTag = new CompoundTag();
            PlayerReferenceNbtCodec.write(userTag, user, resolver);
            nbt.put("user", userTag);
        }
    }

    public static CopiedFromInfo fromBytes(FriendlyByteBuf buf) {
        UUID id = UUIDHelper.fromBytes(buf);
        PlayerId user = buf.readBoolean() ? PlayerIdNetworkCodec.read(buf) : null;
        return new CopiedFromInfo(id, user);
    }

    public void toBytes(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, id);
        buf.writeBoolean(user != null);
        if (user != null) {
            PlayerIdNetworkCodec.write(buf, user);
        }
    }
}
