package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.Nullable;

public final class SourcePluginIdHelper {
    private SourcePluginIdHelper() {
    }

    public static @Nullable String normalize(@Nullable String sourcePluginId) {
        if (sourcePluginId == null) {
            return null;
        }

        StringBuilder normalized = new StringBuilder(sourcePluginId.length());
        for (int i = 0; i < sourcePluginId.length(); i++) {
            char c = sourcePluginId.charAt(i);
            if (isValidNamespaceChar(c)) {
                normalized.append(c);
            }
        }

        if (normalized.length() == 2 && normalized.charAt(0) == '.' && normalized.charAt(1) == '.') {
            normalized.setLength(1);
        }

        return normalized.toString();
    }

    private static boolean isValidNamespaceChar(char c) {
        return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.';
    }
}
