package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

public final class StringConfigEntry extends ConfigEntry<String, StringConfigEntry> {
    public StringConfigEntry(String defaultValue, String... path) {
        super(defaultValue, path);
    }

    @Override
    protected StringConfigEntry self() {
        return this;
    }

    @Override
    protected ReadResult<String> readValue(Object rawValue) {
        if (rawValue instanceof String stringValue) {
            return ReadResult.valid(stringValue);
        }

        return ReadResult.invalid();
    }

    @Override
    protected boolean isValidValue(String newValue) {
        return newValue != null;
    }

    @Override
    protected Object writeValue(String currentValue) {
        return currentValue;
    }

    @Override
    protected Component defaultValueComponent(String currentValue) {
        return literalComponent(currentValue);
    }
}
