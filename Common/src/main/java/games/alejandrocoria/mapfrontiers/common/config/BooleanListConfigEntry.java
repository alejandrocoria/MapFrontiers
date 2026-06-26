package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BooleanListConfigEntry extends ConfigEntry<List<Boolean>, BooleanListConfigEntry> {
    public BooleanListConfigEntry(List<Boolean> defaultValue, String... path) {
        super(defaultValue, path);
    }

    @Override
    protected BooleanListConfigEntry self() {
        return this;
    }

    @Override
    protected List<Boolean> copyValue(List<Boolean> source) {
        return new ArrayList<>(source);
    }

    @Override
    protected ReadResult<List<Boolean>> readValue(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return ReadResult.invalid();
        }

        List<Boolean> parsedValues = new ArrayList<>();
        boolean dirty = false;
        for (Object element : rawList) {
            if (element instanceof Boolean booleanValue) {
                parsedValues.add(booleanValue);
            } else if (element instanceof String stringValue && ("true".equals(stringValue) || "false".equals(stringValue))) {
                parsedValues.add(Boolean.parseBoolean(stringValue));
                dirty = true;
            } else {
                dirty = true;
            }
        }

        return ReadResult.valid(parsedValues, dirty);
    }

    @Override
    protected boolean isValidValue(List<Boolean> newValue) {
        return newValue != null;
    }

    @Override
    protected Object writeValue(List<Boolean> currentValue) {
        return new ArrayList<>(currentValue);
    }

    @Override
    protected Component defaultValueComponent(List<Boolean> currentValue) {
        return literalComponent(currentValue);
    }
}
