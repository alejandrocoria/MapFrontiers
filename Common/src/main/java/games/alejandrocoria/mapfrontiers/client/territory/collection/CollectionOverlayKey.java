package games.alejandrocoria.mapfrontiers.client.territory.collection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public record CollectionOverlayKey(UUID collectionId, ResourceKey<Level> dimension) {
}
