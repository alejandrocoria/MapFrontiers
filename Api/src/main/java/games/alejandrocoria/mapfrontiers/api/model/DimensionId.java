package games.alejandrocoria.mapfrontiers.api.model;

/**
 * Dimension identifier string, usually a namespaced id like {@code minecraft:overworld}.
 */
public record DimensionId(String value) {
    public DimensionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DimensionId cannot be blank");
        }
    }
}
