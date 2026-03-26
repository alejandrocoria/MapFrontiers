package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

public final class IntConfigEntry extends ConfigEntry<Integer, IntConfigEntry> {
    private final int minValue;
    private final int maxValue;

    public IntConfigEntry(int defaultValue, int minValue, int maxValue, String... path) {
        super(defaultValue, path);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    protected IntConfigEntry self() {
        return this;
    }

    @Override
    protected ReadResult<Integer> readValue(Object rawValue) {
        Integer parsedValue = null;
        boolean dirty = false;

        if (rawValue instanceof Number numberValue) {
            parsedValue = numberValue.intValue();
            dirty = !(rawValue instanceof Integer);
        } else if (rawValue instanceof String stringValue) {
            try {
                parsedValue = Integer.parseInt(stringValue);
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
    protected boolean isValidValue(Integer newValue) {
        return newValue != null && newValue >= minValue && newValue <= maxValue;
    }

    @Override
    protected Object writeValue(Integer currentValue) {
        return currentValue;
    }

    @Override
    protected Component defaultValueComponent(Integer currentValue) {
        return literalComponent(currentValue);
    }

    @Override
    protected String describeConstraints() {
        return "Range: " + minValue + " ~ " + maxValue;
    }
}
