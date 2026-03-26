package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

public final class DoubleConfigEntry extends ConfigEntry<Double, DoubleConfigEntry> {
    private final double minValue;
    private final double maxValue;

    public DoubleConfigEntry(double defaultValue, double minValue, double maxValue, String... path) {
        super(defaultValue, path);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    protected DoubleConfigEntry self() {
        return this;
    }

    @Override
    protected ReadResult<Double> readValue(Object rawValue) {
        Double parsedValue = null;
        boolean dirty = false;

        if (rawValue instanceof Number numberValue) {
            parsedValue = numberValue.doubleValue();
            dirty = !(rawValue instanceof Double);
        } else if (rawValue instanceof String stringValue) {
            try {
                parsedValue = Double.parseDouble(stringValue);
                dirty = true;
            } catch (NumberFormatException ignored) {
                return ReadResult.invalid();
            }
        }

        if (parsedValue == null || !isValidValue(parsedValue)) {
            return ReadResult.invalid();
        }

        return ReadResult.valid(parsedValue, dirty);
    }

    @Override
    protected boolean isValidValue(Double newValue) {
        return newValue != null && newValue >= minValue && newValue <= maxValue;
    }

    @Override
    protected Object writeValue(Double currentValue) {
        return currentValue;
    }

    @Override
    protected Component defaultValueComponent(Double currentValue) {
        return literalComponent(currentValue);
    }

    @Override
    protected String describeConstraints() {
        return "Range: " + minValue + " ~ " + maxValue;
    }
}
