package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

public final class ColorConfigEntry extends ConfigEntry<Integer, ColorConfigEntry> {
    public ColorConfigEntry(int defaultValue, String... path) {
        super(normalizeOpaqueColor(defaultValue), path);
    }

    @Override
    protected ColorConfigEntry self() {
        return this;
    }

    @Override
    protected ReadResult<Integer> readValue(Object rawValue) {
        if (!(rawValue instanceof String stringValue)) {
            return ReadResult.invalid();
        }

        Integer parsedValue = parseColor(stringValue);
        if (parsedValue == null) {
            return ReadResult.invalid();
        }

        return ReadResult.valid(parsedValue, !formatColor(parsedValue).equals(stringValue.trim()));
    }

    @Override
    protected boolean isValidValue(Integer newValue) {
        return newValue != null;
    }

    @Override
    protected Object writeValue(Integer currentValue) {
        return formatColor(currentValue);
    }

    @Override
    protected Component defaultValueComponent(Integer currentValue) {
        return literalComponent(formatColor(currentValue));
    }

    @Override
    protected Integer copyValue(Integer source) {
        return source == null ? null : normalizeOpaqueColor(source);
    }

    @Override
    protected String describeConstraints() {
        return "Format: \"#RRGGBB\". Also accepts \"RRGGBB\", \"0xRRGGBB\", and \"0xAARRGGBB\".";
    }

    private static Integer parseColor(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String hexDigits;
        if (trimmed.startsWith("#")) {
            hexDigits = trimmed.substring(1);
        } else if (trimmed.regionMatches(true, 0, "0x", 0, 2)) {
            hexDigits = trimmed.substring(2);
        } else {
            hexDigits = trimmed;
        }

        if (hexDigits.length() != 6 && hexDigits.length() != 8) {
            return null;
        }

        if (!isHex(hexDigits)) {
            return null;
        }

        try {
            long parsed = Long.parseLong(hexDigits, 16);
            if (hexDigits.length() == 8) {
                parsed &= 0x00FFFFFFL;
            }
            return normalizeOpaqueColor((int) parsed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }

        return true;
    }

    private static String formatColor(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    private static int normalizeOpaqueColor(int color) {
        return color | 0xFF000000;
    }
}
