package games.alejandrocoria.mapfrontiers.client.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import games.alejandrocoria.mapfrontiers.common.config.ConfigMigrationException;
import games.alejandrocoria.mapfrontiers.common.config.ConfigMigrationStep;
import games.alejandrocoria.mapfrontiers.common.config.ConfigMigrations;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public final class ClientConfigMigrations implements ConfigMigrations {
    private static final String[][] LEGACY_PATH_MOVES_V0_TO_V1 = {
            // announcement
            {"titleAnnouncementDuration", "announcement.title.duration"},
            {"titleAnnouncementTimeout", "announcement.title.timeout"},
            {"titleAnnouncementAboveHotbar", "announcement.title.aboveHotbar"},
            {"announceUnnamedFrontiers", "announcement.announceUnnamed"},

            // editing
            {"snapDistance", "editing.snapDistance"},

            // appearance
            {"hideNamesThatDontFit", "appearance.hideNamesThatDontFit"},
            {"polygonsOpacity", "appearance.polygons.opacity"},
            {"borderWidth", "appearance.border.width"},
            {"borderOpacity", "appearance.border.opacity"},
            {"textSize", "appearance.text.size"},
            {"textOpacity", "appearance.text.opacity"},
            {"textColor", "appearance.text.color"},
            {"bannerSize", "appearance.banner.size"},
            {"bannerOpacity", "appearance.banner.opacity"},

            // visibility
            {"frontierVisibility", "visibility.frontier"},
            {"announceInChat", "visibility.announceInChat"},
            {"announceInTitle", "visibility.announceInTitle"},
            {"fullscreenVisibility", "visibility.fullscreen.frontier"},
            {"fullscreenNameVisibility", "visibility.fullscreen.name"},
            {"fullscreenOwnerVisibility", "visibility.fullscreen.owner"},
            {"fullscreenBannerVisibility", "visibility.fullscreen.banner"},
            {"fullscreenDayVisibility", "visibility.fullscreen.day"},
            {"fullscreenNightVisibility", "visibility.fullscreen.night"},
            {"fullscreenUndergroundVisibility", "visibility.fullscreen.underground"},
            {"fullscreenTopoVisibility", "visibility.fullscreen.topo"},
            {"fullscreenBiomeVisibility", "visibility.fullscreen.biome"},
            {"minimapVisibility", "visibility.minimap.frontier"},
            {"minimapNameVisibility", "visibility.minimap.name"},
            {"minimapOwnerVisibility", "visibility.minimap.owner"},
            {"minimapBannerVisibility", "visibility.minimap.banner"},
            {"minimapDayVisibility", "visibility.minimap.day"},
            {"minimapNightVisibility", "visibility.minimap.night"},
            {"minimapUndergroundVisibility", "visibility.minimap.underground"},
            {"minimapTopoVisibility", "visibility.minimap.topo"},
            {"minimapBiomeVisibility", "visibility.minimap.biome"},
            {"webmapVisibility", "visibility.webmap.frontier"},
            {"webmapNameVisibility", "visibility.webmap.name"},
            {"webmapOwnerVisibility", "visibility.webmap.owner"},
            {"webmapBannerVisibility", "visibility.webmap.banner"},
            {"webmapDayVisibility", "visibility.webmap.day"},
            {"webmapNightVisibility", "visibility.webmap.night"},
            {"webmapUndergroundVisibility", "visibility.webmap.underground"},
            {"webmapTopoVisibility", "visibility.webmap.topo"},
            {"webmapBiomeVisibility", "visibility.webmap.biome"},

            // gui
            {"fullscreenButtons", "gui.fullscreenButtons"},
            {"askConfirmationFrontierDelete", "gui.confirmation.frontierDelete"},
            {"askConfirmationGroupDelete", "gui.confirmation.groupDelete"},
            {"askConfirmationUserDelete", "gui.confirmation.userDelete"},

            // newFrontier
            {"newFrontierMode", "newFrontier.mode"},
            {"afterCreatingFrontier", "newFrontier.afterCreation"},
            {"newFrontierShape", "newFrontier.shape"},
            {"newFrontierVertexCount", "newFrontier.vertexCount"},
            {"newFrontierShapeWidth", "newFrontier.shapeWidth"},
            {"newFrontierShapeRadius", "newFrontier.shapeRadius"},
            {"newFrontierChunkShape", "newFrontier.chunkShape"},
            {"newFrontierChunkShapeWidth", "newFrontier.chunkShapeWidth"},
            {"newFrontierChunkShapeLength", "newFrontier.chunkShapeLength"},

            // paste
            {"pasteName", "paste.name"},
            {"pasteVisibility", "paste.visibility"},
            {"pasteColor", "paste.color"},
            {"pasteBanner", "paste.banner"},
            {"pasteOptionsVisible", "paste.optionsVisible"},

            // list
            {"frontierSorting", "list.sorting.priority"},
            {"frontierSortingDirection", "list.sorting.directions"},
            {"filterFrontierType", "list.filters.type"},
            {"filterFrontierOwner", "list.filters.owner"},
            {"filterFrontierDimension", "list.filters.dimension"},

            // chatSharing
            {"sendCommand", "chatSharing.sendCommand"}
    };

    public static final ClientConfigMigrations INSTANCE = new ClientConfigMigrations();

    private ClientConfigMigrations() {
    }

    @Nullable
    @Override
    public ConfigMigrationStep step(int fromVersion) {
        return switch (fromVersion) {
            case 0 -> this::migrateFrom0To1;
            case 1 -> this::migrateFrom1To2;
            default -> null;
        };
    }

    private void migrateFrom0To1(CommentedConfig config) throws ConfigMigrationException {
        moveAll(config, LEGACY_PATH_MOVES_V0_TO_V1);

        rewriteStringIfPresent(config, "newFrontier.afterCreation", value -> switch (value) {
            case "Info" -> "InfoScreen";
            case "Edit" -> "EditShape";
            case "Nothing" -> "DoNothing";
            default -> value;
        });
        rewriteStringIfPresent(config, "appearance.text.color", value -> switch (value) {
            case "Frontier" -> "FrontierColor";
            case "Bright" -> "FrontierColorBright";
            default -> value;
        });
        rewriteStringIfPresent(config, "list.filters.owner", value -> switch (value) {
            case "You" -> "Self";
            default -> value;
        });
        rewriteStringIfPresent(config, "list.filters.dimension", value -> switch (value) {
            case "all" -> ClientConfig.DIMENSION_FILTER_ALL;
            case "current" -> ClientConfig.DIMENSION_FILTER_CURRENT;
            default -> value;
        });
    }

    private void migrateFrom1To2(CommentedConfig config) {
        List<HUDSlot> legacyHudSlots = List.of(
                parseLegacyHudSlot(config.get("hud.slot1")),
                parseLegacyHudSlot(config.get("hud.slot2")),
                parseLegacyHudSlot(config.get("hud.slot3"))
        );

        List<HUDSlot> migratedHudSlots = new ArrayList<>(4);
        int nameSlotIndex = legacyHudSlots.indexOf(HUDSlot.Name);
        if (nameSlotIndex >= 0) {
            for (int i = 0; i < legacyHudSlots.size(); ++i) {
                migratedHudSlots.add(legacyHudSlots.get(i));
                if (i == nameSlotIndex) {
                    migratedHudSlots.add(HUDSlot.Collection);
                }
            }
        } else {
            migratedHudSlots.addAll(legacyHudSlots);
            migratedHudSlots.add(HUDSlot.None);
        }

        while (migratedHudSlots.size() < 4) {
            migratedHudSlots.add(HUDSlot.None);
        }
        if (migratedHudSlots.size() > 4) {
            migratedHudSlots = new ArrayList<>(migratedHudSlots.subList(0, 4));
        }

        List<String> migratedSlotNames = migratedHudSlots.stream().map(Enum::name).toList();
        config.set("hud.slots", migratedSlotNames);
        config.remove("hud.slot1");
        config.remove("hud.slot2");
        config.remove("hud.slot3");
    }

    private static void moveAll(CommentedConfig config, String[][] moves) {
        for (String[] move : moves) {
            moveIfPresent(config, move[0], move[1]);
        }
    }

    private static void moveIfPresent(CommentedConfig config, String legacyPath, String newPath) {
        Object value = config.get(legacyPath);
        if (value == null) {
            return;
        }

        config.set(newPath, value);
        config.remove(legacyPath);
    }

    private static void rewriteStringIfPresent(CommentedConfig config, String path, UnaryOperator<String> rewrite) throws ConfigMigrationException {
        Object value = config.get(path);
        if (value == null) {
            return;
        }

        if (!(value instanceof String stringValue)) {
            throw new ConfigMigrationException("Expected string value at config path " + path);
        }

        config.set(path, rewrite.apply(stringValue));
    }

    private static HUDSlot parseLegacyHudSlot(@Nullable Object rawValue) {
        if (!(rawValue instanceof String value)) {
            return HUDSlot.None;
        }

        try {
            return HUDSlot.valueOf(value);
        } catch (Exception ignored) {
            return HUDSlot.None;
        }
    }
}
