package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

public final class BooleanConfigEntry extends ConfigEntry<Boolean, BooleanConfigEntry> {
    public BooleanConfigEntry(boolean defaultValue, String... path) {
        super(defaultValue, path);
    }

    @Override
    protected BooleanConfigEntry self() {
        return this;
    }

    @Override
    protected ReadResult<Boolean> readValue(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return ReadResult.valid(booleanValue);
        }
        if (rawValue instanceof String stringValue && ("true".equals(stringValue) || "false".equals(stringValue))) {
            return ReadResult.valid(Boolean.parseBoolean(stringValue), true);
        }

        return ReadResult.invalid();
    }

    @Override
    protected boolean isValidValue(Boolean newValue) {
        return newValue != null;
    }

    @Override
    protected Object writeValue(Boolean currentValue) {
        return currentValue;
    }

    @Override
    protected Component defaultValueComponent(Boolean currentValue) {
        return booleanComponent(currentValue);
    }
}
