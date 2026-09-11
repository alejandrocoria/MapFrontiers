package games.alejandrocoria.mapfrontiers.common.util;

import io.netty.buffer.ByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class UUIDHelper {
    public static UUID fromBytes(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void toBytes(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    private UUIDHelper() {
    }
}
