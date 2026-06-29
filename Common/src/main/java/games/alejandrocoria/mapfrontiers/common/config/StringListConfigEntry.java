package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class StringListConfigEntry extends ConfigEntry<List<String>, StringListConfigEntry> {
    private final Predicate<String> validator;

    public StringListConfigEntry(List<String> defaultValue, Predicate<String> validator, String... path) {
        super(defaultValue, path);
        this.validator = validator;
    }

    @Override
    protected StringListConfigEntry self() {
        return this;
    }

    @Override
    protected List<String> copyValue(List<String> source) {
        return new ArrayList<>(source);
    }

    @Override
    protected ReadResult<List<String>> readValue(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return ReadResult.invalid();
        }

        List<String> parsedValues = new ArrayList<>();
        boolean dirty = false;
        for (Object element : rawList) {
            if (element instanceof String stringValue && validator.test(stringValue)) {
                parsedValues.add(stringValue);
            } else {
                dirty = true;
            }
        }

        return ReadResult.valid(parsedValues, dirty);
    }

    @Override
    protected boolean isValidValue(List<String> newValue) {
        return newValue != null && newValue.stream().allMatch(validator);
    }

    @Override
    protected Object writeValue(List<String> currentValue) {
        return new ArrayList<>(currentValue);
    }

    @Override
    protected Component defaultValueComponent(List<String> currentValue) {
        return literalComponent(currentValue);
    }
}
