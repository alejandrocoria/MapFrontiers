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
    private static final String[][] PATH_MOVES_V2_TO_V3 = {
            {"announcement.title.duration", "frontier.announcement.title.duration"},
            {"announcement.title.timeout", "frontier.announcement.title.timeout"},
            {"announcement.title.aboveHotbar", "frontier.announcement.title.aboveHotbar"},
            {"announcement.announceUnnamed", "frontier.announcement.announceUnnamed"},

            {"editing.snapDistance", "frontier.editing.snapDistance"},

            {"appearance.hideNamesThatDontFit", "frontier.appearance.hideNamesThatDontFit"},
            {"appearance.polygons.opacity", "frontier.appearance.fill.opacity"},
            {"appearance.border.width", "frontier.appearance.border.width"},
            {"appearance.border.opacity", "frontier.appearance.border.opacity"},
            {"appearance.pathMarkers.size", "frontier.appearance.pathMarkers.size"},
            {"appearance.pathMarkers.opacity", "frontier.appearance.pathMarkers.opacity"},
            {"appearance.text.size", "frontier.appearance.text.size"},
            {"appearance.text.opacity", "frontier.appearance.text.opacity"},
            {"appearance.text.color", "frontier.appearance.text.color"},
            {"appearance.banner.size", "frontier.appearance.banner.size"},
            {"appearance.banner.opacity", "frontier.appearance.banner.opacity"},

            {"path.defaultStyle.start", "frontier.path.defaultStyle.start"},
            {"path.defaultStyle.inner", "frontier.path.defaultStyle.inner"},
            {"path.defaultStyle.end", "frontier.path.defaultStyle.end"},
            {"path.defaultStyle.segment", "frontier.path.defaultStyle.segment"},
            {"path.defaultStyle.labelAtStart", "frontier.path.defaultStyle.labelAtStart"},
            {"path.defaultStyle.labelAtMiddle", "frontier.path.defaultStyle.labelAtMiddle"},
            {"path.defaultStyle.labelAtEnd", "frontier.path.defaultStyle.labelAtEnd"},
            {"path.proximity.enterDistance", "frontier.path.proximity.enterDistance"},
            {"path.proximity.exitDistance", "frontier.path.proximity.exitDistance"},

            {"visibility.frontier", "frontier.visibility.visible"},
            {"visibility.announceInChat", "frontier.visibility.announce.chat"},
            {"visibility.announceInTitle", "frontier.visibility.announce.title"},
            {"visibility.mentionCollection", "frontier.visibility.announce.mentionCollection"},
            {"visibility.fullscreen.frontier", "frontier.visibility.fullscreen.visible"},
            {"visibility.fullscreen.name", "frontier.visibility.fullscreen.name"},
            {"visibility.fullscreen.collection", "frontier.visibility.fullscreen.collection"},
            {"visibility.fullscreen.owner", "frontier.visibility.fullscreen.owner"},
            {"visibility.fullscreen.banner", "frontier.visibility.fullscreen.banner"},
            {"visibility.fullscreen.day", "frontier.visibility.fullscreen.day"},
            {"visibility.fullscreen.night", "frontier.visibility.fullscreen.night"},
            {"visibility.fullscreen.underground", "frontier.visibility.fullscreen.underground"},
            {"visibility.fullscreen.topo", "frontier.visibility.fullscreen.topo"},
            {"visibility.fullscreen.biome", "frontier.visibility.fullscreen.biome"},
            {"visibility.minimap.frontier", "frontier.visibility.minimap.visible"},
            {"visibility.minimap.name", "frontier.visibility.minimap.name"},
            {"visibility.minimap.collection", "frontier.visibility.minimap.collection"},
            {"visibility.minimap.owner", "frontier.visibility.minimap.owner"},
            {"visibility.minimap.banner", "frontier.visibility.minimap.banner"},
            {"visibility.minimap.day", "frontier.visibility.minimap.day"},
            {"visibility.minimap.night", "frontier.visibility.minimap.night"},
            {"visibility.minimap.underground", "frontier.visibility.minimap.underground"},
            {"visibility.minimap.topo", "frontier.visibility.minimap.topo"},
            {"visibility.minimap.biome", "frontier.visibility.minimap.biome"},
            {"visibility.webmap.frontier", "frontier.visibility.webmap.visible"},
            {"visibility.webmap.name", "frontier.visibility.webmap.name"},
            {"visibility.webmap.collection", "frontier.visibility.webmap.collection"},
            {"visibility.webmap.owner", "frontier.visibility.webmap.owner"},
            {"visibility.webmap.banner", "frontier.visibility.webmap.banner"},
            {"visibility.webmap.day", "frontier.visibility.webmap.day"},
            {"visibility.webmap.night", "frontier.visibility.webmap.night"},
            {"visibility.webmap.underground", "frontier.visibility.webmap.underground"},
            {"visibility.webmap.topo", "frontier.visibility.webmap.topo"},
            {"visibility.webmap.biome", "frontier.visibility.webmap.biome"},

            {"newFrontier.shape", "frontier.new.shape"},
            {"newFrontier.afterCreation", "frontier.new.afterCreation"},
            {"newFrontier.vertexShape", "frontier.new.vertexShape"},
            {"newFrontier.vertexCount", "frontier.new.vertexCount"},
            {"newFrontier.vertexShapeWidth", "frontier.new.vertexShapeWidth"},
            {"newFrontier.vertexShapeRadius", "frontier.new.vertexShapeRadius"},
            {"newFrontier.chunkShape", "frontier.new.chunkShape"},
            {"newFrontier.chunkShapeWidth", "frontier.new.chunkShapeWidth"},
            {"newFrontier.chunkShapeLength", "frontier.new.chunkShapeLength"},
            {"newFrontier.pathShape", "frontier.new.pathShape"},
            {"newFrontier.pathSegmentLength", "frontier.new.pathSegmentLength"}
    };
    private static final String[][] PATH_MOVES_V3_TO_V4 = {
            {"frontier.path.defaultStyle.start", "frontier.default.pathStyle.start"},
            {"frontier.path.defaultStyle.inner", "frontier.default.pathStyle.inner"},
            {"frontier.path.defaultStyle.end", "frontier.default.pathStyle.end"},
            {"frontier.path.defaultStyle.segment", "frontier.default.pathStyle.segment"},
            {"frontier.path.defaultStyle.labelAtStart", "frontier.default.pathStyle.labelAtStart"},
            {"frontier.path.defaultStyle.labelAtMiddle", "frontier.default.pathStyle.labelAtMiddle"},
            {"frontier.path.defaultStyle.labelAtEnd", "frontier.default.pathStyle.labelAtEnd"}
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
            case 2 -> this::migrateFrom2To3;
            case 3 -> this::migrateFrom3To4;
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
        migrateHudSlotsToList(config);
        migrateNewFrontierShapeConfig(config);
    }

    private void migrateFrom2To3(CommentedConfig config) {
        moveAll(config, PATH_MOVES_V2_TO_V3);
    }

    private void migrateFrom3To4(CommentedConfig config) {
        moveAll(config, PATH_MOVES_V3_TO_V4);
    }

    private static void migrateHudSlotsToList(CommentedConfig config) {
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

    private static void migrateNewFrontierShapeConfig(CommentedConfig config) {
        // Preserve the old vertex preset before reusing newFrontier.shape for the frontier shape enum.
        moveIfPresent(config, "newFrontier.shape", "newFrontier.vertexShape");
        moveIfPresent(config, "newFrontier.mode", "newFrontier.shape");
        moveIfPresent(config, "newFrontier.shapeWidth", "newFrontier.vertexShapeWidth");
        moveIfPresent(config, "newFrontier.shapeRadius", "newFrontier.vertexShapeRadius");
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
