package games.alejandrocoria.mapfrontiers.api.model;

public record DimensionId(String value) {
    public DimensionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DimensionId cannot be blank");
        }
    }
}
