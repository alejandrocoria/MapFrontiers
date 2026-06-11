package games.alejandrocoria.mapfrontiers.client.config;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.BooleanListConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ColorConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigFile;
import games.alejandrocoria.mapfrontiers.common.config.DoubleConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.EnumConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.StringConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.StringListConfigEntry;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityField;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class ClientConfig {
    public static final int CURRENT_VERSION = 4;

    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory().resolve(MapFrontiers.MODID + "-client.toml");
    private static final ConfigFile FILE = new ConfigFile(CONFIG_PATH, CURRENT_VERSION, ClientConfigMigrations.INSTANCE);
    private static boolean initialized = false;

    private static final List<String> DEFAULT_TERRITORY_LIST_SORTING = List.of(
            TerritoryListSorting.Created.name(),
            TerritoryListSorting.Name.name(),
            TerritoryListSorting.Owner.name(),
            TerritoryListSorting.Shape.name(),
            TerritoryListSorting.Area.name(),
            TerritoryListSorting.Modified.name()
    );
    private static final List<Boolean> DEFAULT_TERRITORY_LIST_SORTING_DIRECTION = List.of(false, true, true, true, true, false);

    public static final String DIMENSION_FILTER_ALL = "mapfrontiers:all";
    public static final String DIMENSION_FILTER_CURRENT = "mapfrontiers:current";

    private static final int HUD_SLOT_COUNT = 4;
    private static final List<HUDSlot> DEFAULT_HUD_SLOTS = List.of(HUDSlot.Name, HUDSlot.Collection, HUDSlot.Owner, HUDSlot.Banner);
    private static final List<String> DEFAULT_HUD_SLOT_NAMES = DEFAULT_HUD_SLOTS.stream().map(Enum::name).toList();
    private static final String DEFAULT_FRONTIER_NAME_1_VALUE = "New";
    private static final String DEFAULT_FRONTIER_NAME_2_VALUE = "Frontier";
    private static final String DEFAULT_COLLECTION_NAME_VALUE = "New Collection";
    private static final int DEFAULT_NEW_TERRITORY_COLOR = 0xFFE0E0E0;


    public static final BooleanConfigEntry ANNOUNCE_UNNAMED_FRONTIERS = register(boolEntry(false, "frontier", "announcement", "announceUnnamed")
            .comment("Announce unnamed frontiers in chat/title.")
            .translation(translation("frontier", "announcement", "announceUnnamed")));
    public static final IntConfigEntry TITLE_ANNOUNCEMENT_DURATION = register(intEntry(70, 0, 1200, "frontier", "announcement", "title", "duration")
            .comment("Duration of title announcement, in game ticks.")
            .translation(translation("frontier", "announcement", "title", "duration")));
    public static final IntConfigEntry TITLE_ANNOUNCEMENT_TIMEOUT = register(intEntry(0, 0, 1200, "frontier", "announcement", "title", "timeout")
            .comment("Minimum time between consecutive title announcement, in game ticks.")
            .translation(translation("frontier", "announcement", "title", "timeout")));
    public static final BooleanConfigEntry TITLE_ANNOUNCEMENT_ABOVE_HOTBAR = register(boolEntry(false, "frontier", "announcement", "title", "aboveHotbar")
            .comment("Show the frontier announcement above the hotbar instead of showing it as a title.")
            .translation(translation("frontier", "announcement", "title", "aboveHotbar")));

    public static final IntConfigEntry SNAP_DISTANCE = register(intEntry(8, 0, 16, "frontier", "editing", "snapDistance")
            .comment("Distance at which vertices snap to nearby vertices.")
            .translation(translation("frontier", "editing", "snapDistance")));

    public static final BooleanConfigEntry HIDE_NAMES_THAT_DONT_FIT = register(boolEntry(false, "frontier", "appearance", "hideNamesThatDontFit")
            .comment("Hide frontier names when they do not fit at the current zoom level.")
            .translation(translation("frontier", "appearance", "hideNamesThatDontFit")));
    public static final DoubleConfigEntry FILL_OPACITY = register(doubleEntry(0.4, 0.0, 1.0, "frontier", "appearance", "fill", "opacity")
            .comment("Transparency of the frontier fill. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("frontier", "appearance", "fill", "opacity")));
    public static final IntConfigEntry BORDER_WIDTH = register(intEntry(0, 0, 64, "frontier", "appearance", "border", "width")
            .comment("Width of the frontier border.")
            .translation(translation("frontier", "appearance", "border", "width")));
    public static final DoubleConfigEntry BORDER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "frontier", "appearance", "border", "opacity")
            .comment("Transparency of the frontier border. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("frontier", "appearance", "border", "opacity")));
    public static final IntConfigEntry PATH_MARKER_SIZE = register(intEntry(2, 1, 5, "frontier", "appearance", "pathMarkers", "size")
            .comment("Size of path markers.")
            .translation(translation("frontier", "appearance", "pathMarkers", "size")));
    public static final DoubleConfigEntry PATH_MARKER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "frontier", "appearance", "pathMarkers", "opacity")
            .comment("Transparency of path markers. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("frontier", "appearance", "pathMarkers", "opacity")));
    public static final IntConfigEntry TEXT_SIZE = register(intEntry(2, 1, 5, "frontier", "appearance", "text", "size")
            .comment("Size of the frontier text.")
            .translation(translation("frontier", "appearance", "text", "size")));
    public static final DoubleConfigEntry TEXT_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "frontier", "appearance", "text", "opacity")
            .comment("Transparency of the frontier text. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("frontier", "appearance", "text", "opacity")));
    public static final EnumConfigEntry<TextColor> TEXT_COLOR = register(enumEntry(TextColor.class, TextColor.FrontierColor, "frontier", "appearance", "text", "color")
            .comment("Color of the frontier text. FrontierColor uses the frontier color. FrontierColorBright uses the same color at maximum brightness.")
            .translation(translation("frontier", "appearance", "text", "color")));
    public static final IntConfigEntry BANNER_SIZE = register(intEntry(1, 1, 5, "frontier", "appearance", "banner", "size")
            .comment("Size of the frontier banner.")
            .translation(translation("frontier", "appearance", "banner", "size")));
    public static final DoubleConfigEntry BANNER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "frontier", "appearance", "banner", "opacity")
            .comment("Transparency of the frontier banner. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("frontier", "appearance", "banner", "opacity")));

    public static final StringConfigEntry FRONTIER_DEFAULT_NAME_1 = register(stringEntry(DEFAULT_FRONTIER_NAME_1_VALUE, "frontier", "default", "name1")
            .comment("Default first name line for new frontiers."));
    public static final StringConfigEntry FRONTIER_DEFAULT_NAME_2 = register(stringEntry(DEFAULT_FRONTIER_NAME_2_VALUE, "frontier", "default", "name2")
            .comment("Default second name line for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_RANDOM_COLOR = register(boolEntry(true, "frontier", "default", "randomColor")
            .comment("Use a random color for new frontiers by default."));
    public static final ColorConfigEntry FRONTIER_DEFAULT_COLOR = register(colorEntry(DEFAULT_NEW_TERRITORY_COLOR,
                    "frontier", "default", "color")
            .comment("Fallback fixed color for new frontiers when random color is disabled."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_VISIBLE = register(boolEntry(FrontierVisibility.Frontier.getDefaultValue(),
                    "frontier", "default", "visibility", "visible")
            .comment("Default visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_ANNOUNCE_IN_CHAT = register(boolEntry(FrontierVisibility.AnnounceInChat.getDefaultValue(),
                    "frontier", "default", "visibility", "announce", "chat")
            .comment("Default chat announcement visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_ANNOUNCE_IN_TITLE = register(boolEntry(FrontierVisibility.AnnounceInTitle.getDefaultValue(),
                    "frontier", "default", "visibility", "announce", "title")
            .comment("Default title announcement visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MENTION_COLLECTION = register(boolEntry(FrontierVisibility.MentionCollection.getDefaultValue(),
                    "frontier", "default", "visibility", "announce", "mentionCollection")
            .comment("Default collection mention visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_VISIBLE = register(boolEntry(FrontierVisibility.Fullscreen.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "visible")
            .comment("Default fullscreen visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_NAME = register(boolEntry(FrontierVisibility.FullscreenName.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "name")
            .comment("Default fullscreen name visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_COLLECTION = register(boolEntry(FrontierVisibility.FullscreenCollection.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "collection")
            .comment("Default fullscreen collection visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_OWNER = register(boolEntry(FrontierVisibility.FullscreenOwner.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "owner")
            .comment("Default fullscreen owner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_BANNER = register(boolEntry(FrontierVisibility.FullscreenBanner.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "banner")
            .comment("Default fullscreen banner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_DAY = register(boolEntry(FrontierVisibility.FullscreenDay.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "day")
            .comment("Default fullscreen day visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_NIGHT = register(boolEntry(FrontierVisibility.FullscreenNight.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "night")
            .comment("Default fullscreen night visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_UNDERGROUND = register(boolEntry(FrontierVisibility.FullscreenUnderground.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "underground")
            .comment("Default fullscreen underground visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_TOPO = register(boolEntry(FrontierVisibility.FullscreenTopo.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "topo")
            .comment("Default fullscreen topo visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_FULLSCREEN_BIOME = register(boolEntry(FrontierVisibility.FullscreenBiome.getDefaultValue(),
                    "frontier", "default", "visibility", "fullscreen", "biome")
            .comment("Default fullscreen biome visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_VISIBLE = register(boolEntry(FrontierVisibility.Minimap.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "visible")
            .comment("Default minimap visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_NAME = register(boolEntry(FrontierVisibility.MinimapName.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "name")
            .comment("Default minimap name visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_COLLECTION = register(boolEntry(FrontierVisibility.MinimapCollection.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "collection")
            .comment("Default minimap collection visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_OWNER = register(boolEntry(FrontierVisibility.MinimapOwner.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "owner")
            .comment("Default minimap owner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_BANNER = register(boolEntry(FrontierVisibility.MinimapBanner.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "banner")
            .comment("Default minimap banner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_DAY = register(boolEntry(FrontierVisibility.MinimapDay.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "day")
            .comment("Default minimap day visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_NIGHT = register(boolEntry(FrontierVisibility.MinimapNight.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "night")
            .comment("Default minimap night visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_UNDERGROUND = register(boolEntry(FrontierVisibility.MinimapUnderground.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "underground")
            .comment("Default minimap underground visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_TOPO = register(boolEntry(FrontierVisibility.MinimapTopo.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "topo")
            .comment("Default minimap topo visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_MINIMAP_BIOME = register(boolEntry(FrontierVisibility.MinimapBiome.getDefaultValue(),
                    "frontier", "default", "visibility", "minimap", "biome")
            .comment("Default minimap biome visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_VISIBLE = register(boolEntry(FrontierVisibility.Webmap.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "visible")
            .comment("Default webmap visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_NAME = register(boolEntry(FrontierVisibility.WebmapName.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "name")
            .comment("Default webmap name visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_COLLECTION = register(boolEntry(FrontierVisibility.WebmapCollection.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "collection")
            .comment("Default webmap collection visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_OWNER = register(boolEntry(FrontierVisibility.WebmapOwner.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "owner")
            .comment("Default webmap owner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_BANNER = register(boolEntry(FrontierVisibility.WebmapBanner.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "banner")
            .comment("Default webmap banner visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_DAY = register(boolEntry(FrontierVisibility.WebmapDay.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "day")
            .comment("Default webmap day visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_NIGHT = register(boolEntry(FrontierVisibility.WebmapNight.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "night")
            .comment("Default webmap night visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_UNDERGROUND = register(boolEntry(FrontierVisibility.WebmapUnderground.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "underground")
            .comment("Default webmap underground visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_TOPO = register(boolEntry(FrontierVisibility.WebmapTopo.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "topo")
            .comment("Default webmap topo visibility for new frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_WEBMAP_BIOME = register(boolEntry(FrontierVisibility.WebmapBiome.getDefaultValue(),
                    "frontier", "default", "visibility", "webmap", "biome")
            .comment("Default webmap biome visibility for new frontiers."));
    public static final StringConfigEntry FRONTIER_DEFAULT_PATH_STYLE_START = register(stringEntry(FrontierData.PathStyle.BIG_DOT.toString(),
                    "frontier", "default", "pathStyle", "start")
            .comment("Marker identifier used by default for the start of new path frontiers."));
    public static final StringConfigEntry FRONTIER_DEFAULT_PATH_STYLE_INNER = register(stringEntry(FrontierData.PathStyle.NONE.toString(),
                    "frontier", "default", "pathStyle", "inner")
            .comment("Marker identifier used by default for inner points of new path frontiers."));
    public static final StringConfigEntry FRONTIER_DEFAULT_PATH_STYLE_END = register(stringEntry(FrontierData.PathStyle.BIG_DOT.toString(),
                    "frontier", "default", "pathStyle", "end")
            .comment("Marker identifier used by default for the end of new path frontiers."));
    public static final StringConfigEntry FRONTIER_DEFAULT_PATH_STYLE_SEGMENT = register(stringEntry(FrontierData.PathStyle.SMALL_DOT.toString(),
                    "frontier", "default", "pathStyle", "segment")
            .comment("Marker identifier used by default for segments of new path frontiers."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_START = register(boolEntry(true,
                    "frontier", "default", "pathStyle", "labelAtStart")
            .comment("Show path labels and banner at the start by default."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_MIDDLE = register(boolEntry(false,
                    "frontier", "default", "pathStyle", "labelAtMiddle")
            .comment("Show path labels and banner at the midpoint by default."));
    public static final BooleanConfigEntry FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_END = register(boolEntry(false,
                    "frontier", "default", "pathStyle", "labelAtEnd")
            .comment("Show path labels and banner at the end by default."));
    public static final IntConfigEntry PATH_PROXIMITY_ENTER_DISTANCE = register(intEntry(8, 0, 128, "frontier", "path", "proximity", "enterDistance")
            .comment("Distance in blocks used to activate Path frontiers for HUD and announcements.")
            .translation(translation("frontier", "path", "proximity", "enterDistance")));
    public static final IntConfigEntry PATH_PROXIMITY_EXIT_DISTANCE = register(intEntry(10, 0, 128, "frontier", "path", "proximity", "exitDistance")
            .comment("Distance in blocks used to keep Path frontiers active for HUD and announcements.")
            .translation(translation("frontier", "path", "proximity", "exitDistance")));

    public static final EnumConfigEntry<FrontierDisplayVisibility> FRONTIER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "visible");
    public static final EnumConfigEntry<FrontierDisplayVisibility> ANNOUNCE_IN_CHAT = frontierVisibilityEntry(
            "Force all frontiers to be announced in chat. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "announce", "chat");
    public static final EnumConfigEntry<FrontierDisplayVisibility> ANNOUNCE_IN_TITLE = frontierVisibilityEntry(
            "Force all frontiers to be announced as titles. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "announce", "title");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MENTION_COLLECTION = frontierVisibilityEntry(
            "Force collection names to be mentioned in local frontier announcements. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "announce", "mentionCollection");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "visible");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_NAME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier names to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_COLLECTION_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier collection names to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "collection");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_OWNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier owners to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_BANNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier banners to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "banner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_DAY_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the day fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "day");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_NIGHT_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the night fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "night");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_UNDERGROUND_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the underground fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "underground");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_TOPO_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the topo fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "topo");
    public static final EnumConfigEntry<FrontierDisplayVisibility> FULLSCREEN_BIOME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the biome fullscreen map. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "fullscreen", "biome");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "visible");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_NAME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier names to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_COLLECTION_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier collection names to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "collection");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_OWNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier owners to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_BANNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier banners to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "banner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_DAY_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the day minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "day");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_NIGHT_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the night minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "night");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_UNDERGROUND_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the underground minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "underground");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_TOPO_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the topo minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "topo");
    public static final EnumConfigEntry<FrontierDisplayVisibility> MINIMAP_BIOME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the biome minimap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "minimap", "biome");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "visible");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_NAME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier names to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_COLLECTION_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier collection names to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "collection");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_OWNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier owners to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_BANNER_VISIBILITY = frontierVisibilityEntry(
            "Force all frontier banners to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "banner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_DAY_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the day webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "day");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_NIGHT_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the night webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "night");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_UNDERGROUND_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the underground webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "underground");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_TOPO_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the topo webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "topo");
    public static final EnumConfigEntry<FrontierDisplayVisibility> WEBMAP_BIOME_VISIBILITY = frontierVisibilityEntry(
            "Force all frontiers to be shown or hidden on the biome webmap. In Custom, you can decide for each frontier.",
            "frontier", "visibility", "webmap", "biome");

    public static final EnumConfigEntry<FrontierShape> NEW_FRONTIER_SHAPE = register(enumEntry(FrontierShape.class, FrontierShape.Vertex, "frontier", "new", "shape")
            .comment("Shape used when creating a new frontier."));
    public static final EnumConfigEntry<AfterCreatingFrontier> AFTER_CREATING_FRONTIER = register(enumEntry(AfterCreatingFrontier.class, AfterCreatingFrontier.InfoScreen, "frontier", "new", "afterCreation")
            .comment("Action to perform after creating a new frontier."));
    public static final IntConfigEntry NEW_FRONTIER_VERTEX_SHAPE = register(intEntry(6, 0, 11, "frontier", "new", "vertexShape")
            .comment("Shape preset used when creating a new vertex frontier."));
    public static final IntConfigEntry NEW_FRONTIER_VERTEX_COUNT = register(intEntry(16, 3, 999, "frontier", "new", "vertexCount")
            .comment("Number of vertices used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_VERTEX_SHAPE_WIDTH = register(intEntry(10, 0, 999, "frontier", "new", "vertexShapeWidth")
            .comment("Width used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_VERTEX_SHAPE_RADIUS = register(intEntry(20, 0, 999, "frontier", "new", "vertexShapeRadius")
            .comment("Radius used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE = register(intEntry(2, 0, 7, "frontier", "new", "chunkShape")
            .comment("Shape preset used when creating a new chunk frontier."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_WIDTH = register(intEntry(5, 0, 32, "frontier", "new", "chunkShapeWidth")
            .comment("Width used by the selected chunk shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_LENGTH = register(intEntry(5, 0, 32, "frontier", "new", "chunkShapeLength")
            .comment("Length used by the selected chunk shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_PATH_SHAPE = register(intEntry(1, 0, 7, "frontier", "new", "pathShape")
            .comment("Shape preset used when creating a new path frontier."));
    public static final IntConfigEntry NEW_FRONTIER_PATH_SEGMENT_LENGTH = register(intEntry(10, 1, 999, "frontier", "new", "pathSegmentLength")
            .comment("Segment length used by the selected path shape preset."));

    public static final DoubleConfigEntry COLLECTION_FILL_OPACITY = register(doubleEntry(0.4, 0.0, 1.0, "collection", "appearance", "fill", "opacity")
            .comment("Transparency of the collection fill in collection view. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("collection", "appearance", "fill", "opacity")));
    public static final IntConfigEntry COLLECTION_BORDER_WIDTH = register(intEntry(4, 0, 64, "collection", "appearance", "border", "width")
            .comment("Width of collection borders in collection view.")
            .translation(translation("collection", "appearance", "border", "width")));
    public static final DoubleConfigEntry COLLECTION_BORDER_OPACITY = register(doubleEntry(0.8, 0.0, 1.0, "collection", "appearance", "border", "opacity")
            .comment("Transparency of collection borders in collection view. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("collection", "appearance", "border", "opacity")));
    public static final IntConfigEntry COLLECTION_TEXT_SIZE = register(intEntry(2, 1, 5, "collection", "appearance", "text", "size")
            .comment("Size of the collection text.")
            .translation(translation("collection", "appearance", "text", "size")));
    public static final DoubleConfigEntry COLLECTION_TEXT_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "collection", "appearance", "text", "opacity")
            .comment("Transparency of the collection text. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("collection", "appearance", "text", "opacity")));
    public static final EnumConfigEntry<TextColor> COLLECTION_TEXT_COLOR = register(enumEntry(TextColor.class, TextColor.FrontierColor,
                    "collection", "appearance", "text", "color")
            .comment("Color of the collection text. Collection uses the collection color. Bright uses the same color at maximum brightness.")
            .translation(translation("collection", "appearance", "text", "color")));
    public static final IntConfigEntry COLLECTION_BANNER_SIZE = register(intEntry(1, 1, 5, "collection", "appearance", "banner", "size")
            .comment("Size of the collection banner.")
            .translation(translation("collection", "appearance", "banner", "size")));
    public static final DoubleConfigEntry COLLECTION_BANNER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "collection", "appearance", "banner", "opacity")
            .comment("Transparency of the collection banner. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("collection", "appearance", "banner", "opacity")));

    public static final StringConfigEntry COLLECTION_DEFAULT_NAME = register(stringEntry(DEFAULT_COLLECTION_NAME_VALUE, "collection", "default", "name")
            .comment("Default name for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_RANDOM_COLOR = register(boolEntry(true, "collection", "default", "randomColor")
            .comment("Use a random color for new collections by default."));
    public static final ColorConfigEntry COLLECTION_DEFAULT_COLOR = register(colorEntry(DEFAULT_NEW_TERRITORY_COLOR,
                    "collection", "default", "color")
            .comment("Fallback fixed color for new collections when random color is disabled."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_VISIBLE = register(boolEntry(CollectionVisibilityField.Visible.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "visible")
            .comment("Default visibility for new collections."));
    public static final IntConfigEntry COLLECTION_DEFAULT_FULLSCREEN_ZOOM = register(intEntry(CollectionVisibilityData.getDefaultFullscreenZoom(), 0, 16384,
                    "collection", "default", "visibility", "fullscreen", "zoom")
            .comment("Default fullscreen collection zoom for new collections. 0 disables collection view."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_FULLSCREEN_NAME = register(boolEntry(CollectionVisibilityField.FullscreenName.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "fullscreen", "name")
            .comment("Default fullscreen name visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_FULLSCREEN_OWNER = register(boolEntry(CollectionVisibilityField.FullscreenOwner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "fullscreen", "owner")
            .comment("Default fullscreen owner visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_FULLSCREEN_BANNER = register(boolEntry(CollectionVisibilityField.FullscreenBanner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "fullscreen", "banner")
            .comment("Default fullscreen banner visibility for new collections."));
    public static final IntConfigEntry COLLECTION_DEFAULT_MINIMAP_ZOOM = register(intEntry(CollectionVisibilityData.getDefaultMinimapZoom(), 0, 16384,
                    "collection", "default", "visibility", "minimap", "zoom")
            .comment("Default minimap collection zoom for new collections. 0 disables collection view."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_MINIMAP_NAME = register(boolEntry(CollectionVisibilityField.MinimapName.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "minimap", "name")
            .comment("Default minimap name visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_MINIMAP_OWNER = register(boolEntry(CollectionVisibilityField.MinimapOwner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "minimap", "owner")
            .comment("Default minimap owner visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_MINIMAP_BANNER = register(boolEntry(CollectionVisibilityField.MinimapBanner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "minimap", "banner")
            .comment("Default minimap banner visibility for new collections."));
    public static final IntConfigEntry COLLECTION_DEFAULT_WEBMAP_ZOOM = register(intEntry(CollectionVisibilityData.getDefaultWebmapZoom(), 0, 16384,
                    "collection", "default", "visibility", "webmap", "zoom")
            .comment("Default webmap collection zoom for new collections. 0 disables collection view."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_WEBMAP_NAME = register(boolEntry(CollectionVisibilityField.WebmapName.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "webmap", "name")
            .comment("Default webmap name visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_WEBMAP_OWNER = register(boolEntry(CollectionVisibilityField.WebmapOwner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "webmap", "owner")
            .comment("Default webmap owner visibility for new collections."));
    public static final BooleanConfigEntry COLLECTION_DEFAULT_WEBMAP_BANNER = register(boolEntry(CollectionVisibilityField.WebmapBanner.getDefaultBooleanValue(),
                    "collection", "default", "visibility", "webmap", "banner")
            .comment("Default webmap banner visibility for new collections."));

    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_VISIBILITY = collectionVisibilityEntry(
            "Force all collections to be shown or hidden. In Custom, you can decide for each collection.",
            "collection", "visibility", "visible");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_FULLSCREEN_NAME_VISIBILITY = collectionVisibilityEntry(
            "Force all collection names to be shown or hidden on the fullscreen map. In Custom, you can decide for each collection.",
            "collection", "visibility", "fullscreen", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_FULLSCREEN_OWNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection owners to be shown or hidden on the fullscreen map. In Custom, you can decide for each collection.",
            "collection", "visibility", "fullscreen", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_FULLSCREEN_BANNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection banners to be shown or hidden on the fullscreen map. In Custom, you can decide for each collection.",
            "collection", "visibility", "fullscreen", "banner");
    public static final BooleanConfigEntry COLLECTION_FULLSCREEN_ZOOM_FORCED = register(boolEntry(false, "collection", "visibility", "fullscreen", "zoomForced")
            .comment("Force collection zoom on the fullscreen map instead of using each collection."));
    public static final IntConfigEntry COLLECTION_FULLSCREEN_ZOOM = register(intEntry(256, 0, 16384, "collection", "visibility", "fullscreen", "zoom")
            .comment("Forced collection zoom on the fullscreen map. 0 disables collection view."));
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_MINIMAP_NAME_VISIBILITY = collectionVisibilityEntry(
            "Force all collection names to be shown or hidden on the minimap. In Custom, you can decide for each collection.",
            "collection", "visibility", "minimap", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_MINIMAP_OWNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection owners to be shown or hidden on the minimap. In Custom, you can decide for each collection.",
            "collection", "visibility", "minimap", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_MINIMAP_BANNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection banners to be shown or hidden on the minimap. In Custom, you can decide for each collection.",
            "collection", "visibility", "minimap", "banner");
    public static final BooleanConfigEntry COLLECTION_MINIMAP_ZOOM_FORCED = register(boolEntry(false, "collection", "visibility", "minimap", "zoomForced")
            .comment("Force collection zoom on the minimap instead of using each collection."));
    public static final IntConfigEntry COLLECTION_MINIMAP_ZOOM = register(intEntry(256, 0, 16384, "collection", "visibility", "minimap", "zoom")
            .comment("Forced collection zoom on the minimap. 0 disables collection view."));
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_WEBMAP_NAME_VISIBILITY = collectionVisibilityEntry(
            "Force all collection names to be shown or hidden on the webmap. In Custom, you can decide for each collection.",
            "collection", "visibility", "webmap", "name");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_WEBMAP_OWNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection owners to be shown or hidden on the webmap. In Custom, you can decide for each collection.",
            "collection", "visibility", "webmap", "owner");
    public static final EnumConfigEntry<FrontierDisplayVisibility> COLLECTION_WEBMAP_BANNER_VISIBILITY = collectionVisibilityEntry(
            "Force all collection banners to be shown or hidden on the webmap. In Custom, you can decide for each collection.",
            "collection", "visibility", "webmap", "banner");
    public static final BooleanConfigEntry COLLECTION_WEBMAP_ZOOM_FORCED = register(boolEntry(false, "collection", "visibility", "webmap", "zoomForced")
            .comment("Force collection zoom on the webmap instead of using each collection."));
    public static final IntConfigEntry COLLECTION_WEBMAP_ZOOM = register(intEntry(256, 0, 16384, "collection", "visibility", "webmap", "zoom")
            .comment("Forced collection zoom on the webmap. 0 disables collection view."));

    public static final BooleanConfigEntry FULLSCREEN_BUTTONS = register(boolEntry(true, "gui", "fullscreenButtons")
            .comment("Show buttons on the fullscreen map.")
            .translation(translation("gui", "fullscreenButtons")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_FRONTIER_DELETE = register(boolEntry(true, "gui", "confirmation", "frontierDelete")
            .comment("Show a confirmation dialog before deleting a frontier.")
            .translation(translation("gui", "confirmation", "frontierDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_COLLECTION_DELETE = register(boolEntry(true, "gui", "confirmation", "collectionDelete")
            .comment("Show a confirmation dialog before deleting a collection.")
            .translation(translation("gui", "confirmation", "collectionDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_GROUP_DELETE = register(boolEntry(true, "gui", "confirmation", "groupDelete")
            .comment("Show a confirmation dialog before deleting a group.")
            .translation(translation("gui", "confirmation", "groupDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_USER_DELETE = register(boolEntry(true, "gui", "confirmation", "userDelete")
            .comment("Show a confirmation dialog before deleting a user.")
            .translation(translation("gui", "confirmation", "userDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE = register(boolEntry(true, "gui", "confirmation", "temporaryFrontierCreate")
            .comment("Show a confirmation dialog before creating a temporary frontier.")
            .translation(translation("gui", "confirmation", "temporaryFrontierCreate")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE = register(boolEntry(true, "gui", "confirmation", "temporaryCollectionCreate")
            .comment("Show a confirmation dialog before creating a temporary collection.")
            .translation(translation("gui", "confirmation", "temporaryCollectionCreate")));

    public static final BooleanConfigEntry HUD_ENABLED = register(boolEntry(true, "hud", "enabled")
            .comment("Show the HUD on screen.")
            .translation(translation("hud", "enabled")));
    public static final BooleanConfigEntry HUD_AUTO_ADJUST_ANCHOR = register(boolEntry(true, "hud", "autoAdjustAnchor")
            .comment("Automatically switch to nearest anchor when HUD position is edited (on settings screen).")
            .translation(translation("hud", "autoAdjustAnchor")));
    public static final BooleanConfigEntry HUD_SNAP_TO_BORDER = register(boolEntry(true, "hud", "snapToBorder")
            .comment("Automatically snap to closest border when HUD position is edited (on settings screen).")
            .translation(translation("hud", "snapToBorder")));
    public static final IntConfigEntry HUD_TEXT_SIZE = register(intEntry(1, 1, 8, "hud", "textSize")
            .comment("Size of the HUD text.")
            .translation(translation("hud", "textSize")));
    public static final IntConfigEntry HUD_BANNER_SIZE = register(intEntry(3, 1, 8, "hud", "bannerSize")
            .comment("Size of the HUD banner.")
            .translation(translation("hud", "bannerSize")));
    public static final StringListConfigEntry HUD_SLOTS = register(stringListEntry(DEFAULT_HUD_SLOT_NAMES, ClientConfig::isValidHUDSlot, "hud", "slots")
            .comment("HUD elements in order from slot 1 to slot 4. Valid values: None, Name, Collection, Owner, Banner."));
    public static final EnumConfigEntry<HUDAnchor> HUD_ANCHOR = register(enumEntry(HUDAnchor.class, HUDAnchor.MinimapHorizontal, "hud", "anchor")
            .comment("Anchor point of the HUD. When anchored to the minimap, coordinates are relative to the minimap's default position.")
            .translation(translation("hud", "anchor")));
    public static final IntConfigEntry HUD_X_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "xPosition")
            .comment("Horizontal HUD offset relative to the selected anchor."));
    public static final IntConfigEntry HUD_Y_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "yPosition")
            .comment("Vertical HUD offset relative to the selected anchor."));

    public static final BooleanConfigEntry PASTE_NAME = register(boolEntry(false, "paste", "name")
            .comment("Paste the name when pasting info."));
    public static final BooleanConfigEntry PASTE_VISIBILITY = register(boolEntry(true, "paste", "visibility")
            .comment("Paste visibility settings when pasting info."));
    public static final BooleanConfigEntry PASTE_PATH_STYLE = register(boolEntry(true, "paste", "pathStyle")
            .comment("Paste the frontier path style when pasting info."));
    public static final BooleanConfigEntry PASTE_COLOR = register(boolEntry(true, "paste", "color")
            .comment("Paste the color when pasting info."));
    public static final BooleanConfigEntry PASTE_BANNER = register(boolEntry(true, "paste", "banner")
            .comment("Paste the banner when pasting info."));
    public static final BooleanConfigEntry PASTE_OPTIONS_VISIBLE = register(boolEntry(false, "paste", "optionsVisible")
            .comment("Whether paste options are currently expanded."));
    public static final EnumConfigEntry<ColorInputMode> COLOR_INPUT_MODE = register(enumEntry(ColorInputMode.class, ColorInputMode.RGB, "color", "inputMode")
            .comment("Selected color input mode in color editors."));

    public static final StringListConfigEntry TERRITORY_LIST_SORTING = register(stringListEntry(DEFAULT_TERRITORY_LIST_SORTING, ClientConfig::isValidTerritoryListSorting, "list", "sorting", "priority")
            .comment("Order of the MapFrontiers list sorting modes."));
    public static final BooleanListConfigEntry TERRITORY_LIST_SORTING_DIRECTION = register(booleanListEntry(DEFAULT_TERRITORY_LIST_SORTING_DIRECTION, "list", "sorting", "directions")
            .comment("Direction of the MapFrontiers list sorting modes. True means ascending and false means descending."));
    public static final EnumConfigEntry<FilterFrontierShape> FILTER_FRONTIER_SHAPE = register(enumEntry(FilterFrontierShape.class, FilterFrontierShape.All, "list", "filters", "shape")
            .comment("Selected frontier shape filter in the MapFrontiers list."));
    public static final EnumConfigEntry<FilterFrontierOwner> FILTER_FRONTIER_OWNER = register(enumEntry(FilterFrontierOwner.class, FilterFrontierOwner.All, "list", "filters", "owner")
            .comment("Selected frontier owner filter in the MapFrontiers list."));
    public static final StringConfigEntry FILTER_FRONTIER_DIMENSION = register(stringEntry(DIMENSION_FILTER_ALL, "list", "filters", "dimension")
            .comment("Selected dimension filter in the MapFrontiers list. Use \"" + DIMENSION_FILTER_ALL + "\" to show every dimension or \"" + DIMENSION_FILTER_CURRENT + "\" to show only the current one."));

    public static final StringConfigEntry SEND_COMMAND = register(stringEntry("msg", "chatSharing", "sendCommand")
            .comment("Chat command used to send shared frontiers to another player."));

    static {
        FILE.registerSectionComment("frontier", "Frontier settings.");
        FILE.registerSectionComment("frontier.path", "Path settings.");
        FILE.registerSectionComment("collection", "Collection settings.");
        FILE.registerSectionComment("collection.appearance", "Collection appearance settings.");
        FILE.registerSectionComment("list", "MapFrontiers list settings.");
    }

    public static boolean load() {
        boolean dirty = FILE.load();
        dirty |= validateHUDSlots();
        dirty |= validateDefaultPathStyle();
        dirty |= validateDefaultTerritoryNamesAndColors();
        dirty |= validatePathActivationDistances();
        dirty |= validateCollectionVisibilityZooms();
        dirty |= validateDefaultCollectionVisibilityZooms();
        dirty |= validateSorting();
        return dirty;
    }

    public static void save() {
        validateHUDSlots();
        validateDefaultPathStyle();
        validateDefaultTerritoryNamesAndColors();
        validatePathActivationDistances();
        validateCollectionVisibilityZooms();
        validateDefaultCollectionVisibilityZooms();
        validateSorting();
        FILE.save();
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientGlobalEvents.subscribeUpdatedConfigEvent(ClientConfig.class, ClientConfig::save);
        if (load()) {
            save();
        }
    }

    public static boolean resolveVisibilityValue(FrontierDisplayVisibility visibility, boolean customValue) {
        return switch (visibility) {
            case Always -> true;
            case Never -> false;
            case Custom -> customValue;
        };
    }

    public static <E extends Enum<E>> Component getTranslatedEnum(E value) {
        return Component.translatable("mapfrontiers.config." + value.name());
    }

    public static List<HUDSlot> getHUDSlots() {
        List<HUDSlot> configuredSlots = HUD_SLOTS.get().stream().map(ClientConfig::parseHUDSlot).toList();
        return normalizeHUDSlots(configuredSlots);
    }

    public static HUDSlot getDefaultHUDSlot(int index) {
        if (index >= 0 && index < DEFAULT_HUD_SLOTS.size()) {
            return DEFAULT_HUD_SLOTS.get(index);
        }
        return HUDSlot.None;
    }

    public static void setHUDSlots(List<HUDSlot> slots) {
        List<HUDSlot> normalized = normalizeHUDSlots(slots);
        HUD_SLOTS.set(normalized.stream().map(Enum::name).toList());
    }

    private static List<HUDSlot> normalizeHUDSlots(List<HUDSlot> slots) {
        List<HUDSlot> paddedSlots = new ArrayList<>(HUD_SLOT_COUNT);
        for (int i = 0; i < HUD_SLOT_COUNT; ++i) {
            HUDSlot slot = i < slots.size() && slots.get(i) != null ? slots.get(i) : HUDSlot.None;
            paddedSlots.add(slot);
        }

        List<HUDSlot> resolvedSlots = new ArrayList<>(HUD_SLOT_COUNT);
        EnumSet<HUDSlot> seenSlots = EnumSet.noneOf(HUDSlot.class);

        for (HUDSlot slot : paddedSlots) {
            if (slot == HUDSlot.None || seenSlots.add(slot)) {
                resolvedSlots.add(slot);
            } else {
                resolvedSlots.add(HUDSlot.None);
            }
        }

        return resolvedSlots;
    }

    public static List<TerritoryListSorting> getTerritoryListSortingValues() {
        return TERRITORY_LIST_SORTING.get().stream().map(TerritoryListSorting::valueOf).toList();
    }

    public static void setTerritoryListSortingValues(List<TerritoryListSorting> sorting) {
        TERRITORY_LIST_SORTING.set(sorting.stream().map(Enum::name).toList());
    }

    public static List<Boolean> getTerritoryListSortingDirectionValues() {
        return TERRITORY_LIST_SORTING_DIRECTION.get();
    }

    public static void setTerritoryListSortingDirectionValues(List<Boolean> direction) {
        TERRITORY_LIST_SORTING_DIRECTION.set(direction);
    }

    public static FrontierVisibilityData getDefaultFrontierVisibility() {
        FrontierVisibilityData visibilityData = new FrontierVisibilityData();
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            visibilityData.set(visibility, getDefaultFrontierVisibilityEntry(visibility).get());
        }
        return visibilityData;
    }

    public static void setDefaultFrontierVisibility(FrontierVisibilityData visibilityData) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            getDefaultFrontierVisibilityEntry(visibility).set(visibilityData.get(visibility));
        }
    }

    public static CollectionVisibilityData getDefaultCollectionVisibility() {
        CollectionVisibilityData visibilityData = new CollectionVisibilityData();
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                visibilityData.setBoolean(field, getDefaultCollectionBooleanVisibilityEntry(field).get());
            } else {
                visibilityData.setZoom(field, getDefaultCollectionZoomVisibilityEntry(field).get());
            }
        }
        return visibilityData;
    }

    public static void setDefaultCollectionVisibility(CollectionVisibilityData visibilityData) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                getDefaultCollectionBooleanVisibilityEntry(field).set(visibilityData.getBoolean(field));
            } else {
                getDefaultCollectionZoomVisibilityEntry(field).set(visibilityData.getZoom(field));
            }
        }
    }

    public static FrontierData.PathStyle getDefaultFrontierPathStyle() {
        FrontierData.PathStyle pathStyle = new FrontierData.PathStyle();
        pathStyle.startMarker = parsePathMarker(FRONTIER_DEFAULT_PATH_STYLE_START.get(), FrontierData.PathStyle.BIG_DOT);
        pathStyle.innerMarker = parsePathMarker(FRONTIER_DEFAULT_PATH_STYLE_INNER.get(), FrontierData.PathStyle.NONE);
        pathStyle.endMarker = parsePathMarker(FRONTIER_DEFAULT_PATH_STYLE_END.get(), FrontierData.PathStyle.BIG_DOT);
        pathStyle.segmentMarker = parsePathMarker(FRONTIER_DEFAULT_PATH_STYLE_SEGMENT.get(), FrontierData.PathStyle.SMALL_DOT);
        pathStyle.labelAtStart = FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_START.get();
        pathStyle.labelAtMiddle = FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_MIDDLE.get();
        pathStyle.labelAtEnd = FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_END.get();
        pathStyle.normalizeForPersistence();
        return pathStyle;
    }

    public static void setDefaultFrontierPathStyle(FrontierData.PathStyle pathStyle) {
        FrontierData.PathStyle normalized = normalizeDefaultPathStyle(pathStyle);
        FRONTIER_DEFAULT_PATH_STYLE_START.set(normalized.startMarker.toString());
        FRONTIER_DEFAULT_PATH_STYLE_INNER.set(normalized.innerMarker.toString());
        FRONTIER_DEFAULT_PATH_STYLE_END.set(normalized.endMarker.toString());
        FRONTIER_DEFAULT_PATH_STYLE_SEGMENT.set(normalized.segmentMarker.toString());
        FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_START.set(normalized.labelAtStart);
        FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_MIDDLE.set(normalized.labelAtMiddle);
        FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_END.set(normalized.labelAtEnd);
    }

    public static int resolveNewFrontierColor() {
        if (FRONTIER_DEFAULT_RANDOM_COLOR.get()) {
            return ColorHelper.getRandomColor();
        }
        return normalizeOpaqueColor(FRONTIER_DEFAULT_COLOR.get());
    }

    public static int resolveNewCollectionColor() {
        if (COLLECTION_DEFAULT_RANDOM_COLOR.get()) {
            return ColorHelper.getRandomColor();
        }
        return normalizeOpaqueColor(COLLECTION_DEFAULT_COLOR.get());
    }

    public static void applyDefaultFrontierValues(FrontierData frontier) {
        frontier.setName1(normalizeName(FRONTIER_DEFAULT_NAME_1.get(), FrontierData.MAX_NAME_CHARACTERS));
        frontier.setName2(normalizeName(FRONTIER_DEFAULT_NAME_2.get(), FrontierData.MAX_NAME_CHARACTERS));
        frontier.setColor(resolveNewFrontierColor());
        frontier.setVisibilityData(getDefaultFrontierVisibility());
        if (frontier.getShape() == FrontierShape.Path) {
            frontier.setPathStyle(getDefaultFrontierPathStyle());
        }
    }

    public static void applyDefaultCollectionValues(CollectionData collection) {
        collection.setName(normalizeName(COLLECTION_DEFAULT_NAME.get(), CollectionData.MAX_NAME_CHARACTERS));
        collection.setColor(resolveNewCollectionColor());
        collection.setVisibilityData(getDefaultCollectionVisibility());
    }

    public static double getPathActivationDistance(boolean alreadyActive) {
        return alreadyActive ? PATH_PROXIMITY_EXIT_DISTANCE.get() : PATH_PROXIMITY_ENTER_DISTANCE.get();
    }

    public static int getNormalizedCollectionFullscreenZoom() {
        return CollectionVisibilityData.normalizeZoomToNearest(COLLECTION_FULLSCREEN_ZOOM.get());
    }

    public static int getNormalizedCollectionMinimapZoom() {
        return CollectionVisibilityData.normalizeZoomToNearest(COLLECTION_MINIMAP_ZOOM.get());
    }

    public static int getNormalizedCollectionWebmapZoom() {
        return CollectionVisibilityData.normalizeZoomToNearest(COLLECTION_WEBMAP_ZOOM.get());
    }

    private static boolean validateDefaultPathStyle() {
        FrontierData.PathStyle normalized = getDefaultFrontierPathStyle();
        boolean dirty = !FRONTIER_DEFAULT_PATH_STYLE_START.get().equals(normalized.startMarker.toString())
                || !FRONTIER_DEFAULT_PATH_STYLE_INNER.get().equals(normalized.innerMarker.toString())
                || !FRONTIER_DEFAULT_PATH_STYLE_END.get().equals(normalized.endMarker.toString())
                || !FRONTIER_DEFAULT_PATH_STYLE_SEGMENT.get().equals(normalized.segmentMarker.toString())
                || FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_START.get() != normalized.labelAtStart
                || FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_MIDDLE.get() != normalized.labelAtMiddle
                || FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_END.get() != normalized.labelAtEnd;

        if (dirty) {
            setDefaultFrontierPathStyle(normalized);
        }

        return dirty;
    }

    private static boolean validateDefaultTerritoryNamesAndColors() {
        boolean dirty = false;

        String frontierName1 = normalizeName(FRONTIER_DEFAULT_NAME_1.get(), FrontierData.MAX_NAME_CHARACTERS);
        if (!FRONTIER_DEFAULT_NAME_1.get().equals(frontierName1)) {
            FRONTIER_DEFAULT_NAME_1.set(frontierName1);
            dirty = true;
        }

        String frontierName2 = normalizeName(FRONTIER_DEFAULT_NAME_2.get(), FrontierData.MAX_NAME_CHARACTERS);
        if (!FRONTIER_DEFAULT_NAME_2.get().equals(frontierName2)) {
            FRONTIER_DEFAULT_NAME_2.set(frontierName2);
            dirty = true;
        }

        String collectionName = normalizeName(COLLECTION_DEFAULT_NAME.get(), CollectionData.MAX_NAME_CHARACTERS);
        if (!COLLECTION_DEFAULT_NAME.get().equals(collectionName)) {
            COLLECTION_DEFAULT_NAME.set(collectionName);
            dirty = true;
        }

        int frontierColor = normalizeOpaqueColor(FRONTIER_DEFAULT_COLOR.get());
        if (FRONTIER_DEFAULT_COLOR.get() != frontierColor) {
            FRONTIER_DEFAULT_COLOR.set(frontierColor);
            dirty = true;
        }

        int collectionColor = normalizeOpaqueColor(COLLECTION_DEFAULT_COLOR.get());
        if (COLLECTION_DEFAULT_COLOR.get() != collectionColor) {
            COLLECTION_DEFAULT_COLOR.set(collectionColor);
            dirty = true;
        }

        return dirty;
    }

    private static boolean validatePathActivationDistances() {
        int enterDistance = Math.max(0, PATH_PROXIMITY_ENTER_DISTANCE.get());
        int exitDistance = Math.max(enterDistance, PATH_PROXIMITY_EXIT_DISTANCE.get());
        boolean dirty = PATH_PROXIMITY_ENTER_DISTANCE.get() != enterDistance
                || PATH_PROXIMITY_EXIT_DISTANCE.get() != exitDistance;

        if (dirty) {
            PATH_PROXIMITY_ENTER_DISTANCE.set(enterDistance);
            PATH_PROXIMITY_EXIT_DISTANCE.set(exitDistance);
        }

        return dirty;
    }

    private static boolean validateCollectionVisibilityZooms() {
        int previousFullscreenZoom = COLLECTION_FULLSCREEN_ZOOM.get();
        int previousMinimapZoom = COLLECTION_MINIMAP_ZOOM.get();
        int previousWebmapZoom = COLLECTION_WEBMAP_ZOOM.get();
        int fullscreenZoom = CollectionVisibilityData.normalizeZoomToNearest(previousFullscreenZoom);
        int minimapZoom = CollectionVisibilityData.normalizeZoomToNearest(previousMinimapZoom);
        int webmapZoom = CollectionVisibilityData.normalizeZoomToNearest(previousWebmapZoom);
        boolean dirty = previousFullscreenZoom != fullscreenZoom
                || previousMinimapZoom != minimapZoom
                || previousWebmapZoom != webmapZoom;

        if (dirty) {
            List<String> correctedEntries = new ArrayList<>(3);
            if (previousFullscreenZoom != fullscreenZoom) {
                correctedEntries.add("fullscreen " + previousFullscreenZoom + " -> " + fullscreenZoom);
            }
            if (previousMinimapZoom != minimapZoom) {
                correctedEntries.add("minimap " + previousMinimapZoom + " -> " + minimapZoom);
            }
            if (previousWebmapZoom != webmapZoom) {
                correctedEntries.add("webmap " + previousWebmapZoom + " -> " + webmapZoom);
            }

            COLLECTION_FULLSCREEN_ZOOM.set(fullscreenZoom);
            COLLECTION_MINIMAP_ZOOM.set(minimapZoom);
            COLLECTION_WEBMAP_ZOOM.set(webmapZoom);
            MapFrontiers.LOGGER.warn("Corrected invalid collection forced zoom config values to nearest supported zooms: {}",
                    String.join(", ", correctedEntries));
        }

        return dirty;
    }

    private static boolean validateDefaultCollectionVisibilityZooms() {
        int previousFullscreenZoom = COLLECTION_DEFAULT_FULLSCREEN_ZOOM.get();
        int previousMinimapZoom = COLLECTION_DEFAULT_MINIMAP_ZOOM.get();
        int previousWebmapZoom = COLLECTION_DEFAULT_WEBMAP_ZOOM.get();
        int fullscreenZoom = CollectionVisibilityData.normalizeZoomToNearest(previousFullscreenZoom);
        int minimapZoom = CollectionVisibilityData.normalizeZoomToNearest(previousMinimapZoom);
        int webmapZoom = CollectionVisibilityData.normalizeZoomToNearest(previousWebmapZoom);
        boolean dirty = previousFullscreenZoom != fullscreenZoom
                || previousMinimapZoom != minimapZoom
                || previousWebmapZoom != webmapZoom;

        if (dirty) {
            COLLECTION_DEFAULT_FULLSCREEN_ZOOM.set(fullscreenZoom);
            COLLECTION_DEFAULT_MINIMAP_ZOOM.set(minimapZoom);
            COLLECTION_DEFAULT_WEBMAP_ZOOM.set(webmapZoom);
            MapFrontiers.LOGGER.warn("Corrected invalid collection default visibility zoom config values to nearest supported zooms: fullscreen {} -> {}, minimap {} -> {}, webmap {} -> {}",
                    previousFullscreenZoom, fullscreenZoom, previousMinimapZoom, minimapZoom, previousWebmapZoom, webmapZoom);
        }

        return dirty;
    }

    private static boolean validateSorting() {
        List<String> sorting = new ArrayList<>(TERRITORY_LIST_SORTING.get());
        List<Boolean> direction = new ArrayList<>(TERRITORY_LIST_SORTING_DIRECTION.get());
        boolean dirty = false;

        for (int i = 0; i < sorting.size(); ++i) {
            if (sorting.get(i).equals("VertexChunk")) {
                sorting.set(i, TerritoryListSorting.Shape.name());
                dirty = true;
            }
        }

        if (sorting.size() > TerritoryListSorting.VALUES.length || direction.size() != sorting.size()) {
            TERRITORY_LIST_SORTING.set(DEFAULT_TERRITORY_LIST_SORTING);
            TERRITORY_LIST_SORTING_DIRECTION.set(DEFAULT_TERRITORY_LIST_SORTING_DIRECTION);
            return true;
        }

        List<TerritoryListSorting> missing = new ArrayList<>();
        for (TerritoryListSorting sort : TerritoryListSorting.VALUES) {
            int count = Collections.frequency(sorting, sort.name());
            if (count > 1) {
                TERRITORY_LIST_SORTING.set(DEFAULT_TERRITORY_LIST_SORTING);
                TERRITORY_LIST_SORTING_DIRECTION.set(DEFAULT_TERRITORY_LIST_SORTING_DIRECTION);
                return true;
            }
            if (count == 0) {
                missing.add(sort);
            }
        }

        if (!missing.isEmpty()) {
            dirty = true;
            for (TerritoryListSorting missingSort : missing) {
                sorting.add(missingSort.name());
                int defaultIndex = DEFAULT_TERRITORY_LIST_SORTING.indexOf(missingSort.name());
                direction.add(DEFAULT_TERRITORY_LIST_SORTING_DIRECTION.get(defaultIndex));
            }

            TERRITORY_LIST_SORTING.set(sorting);
            TERRITORY_LIST_SORTING_DIRECTION.set(direction);
        } else if (dirty) {
            TERRITORY_LIST_SORTING.set(sorting);
        }

        return dirty;
    }

    private static boolean validateHUDSlots() {
        List<HUDSlot> normalized = normalizeHUDSlots(HUD_SLOTS.get().stream().map(ClientConfig::parseHUDSlot).toList());
        List<String> normalizedStrings = normalized.stream().map(Enum::name).toList();
        if (!HUD_SLOTS.get().equals(normalizedStrings)) {
            HUD_SLOTS.set(normalizedStrings);
            return true;
        }

        return false;
    }

    private static boolean isValidTerritoryListSorting(String value) {
        if (value.equals("VertexChunk")) {
            return true;
        }

        try {
            TerritoryListSorting.valueOf(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isValidHUDSlot(String value) {
        try {
            HUDSlot.valueOf(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static HUDSlot parseHUDSlot(String value) {
        try {
            return HUDSlot.valueOf(value);
        } catch (Exception ignored) {
            return HUDSlot.None;
        }
    }

    private static <T extends ConfigEntry<?, ?>> T register(T entry) {
        return FILE.register(entry);
    }

    private static String translation(String... path) {
        return MapFrontiers.MODID + ".config." + String.join(".", path);
    }

    private static String normalizeName(String value, int maxCharacters) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > maxCharacters) {
            normalized = normalized.substring(0, maxCharacters);
        }
        return normalized;
    }

    private static int normalizeOpaqueColor(int color) {
        return color | 0xFF000000;
    }

    private static FrontierData.PathStyle normalizeDefaultPathStyle(FrontierData.PathStyle pathStyle) {
        FrontierData.PathStyle normalized = new FrontierData.PathStyle(pathStyle);
        normalized.startMarker = normalized.startMarker == null ? FrontierData.PathStyle.BIG_DOT : normalized.startMarker;
        normalized.innerMarker = normalized.innerMarker == null ? FrontierData.PathStyle.NONE : normalized.innerMarker;
        normalized.endMarker = normalized.endMarker == null ? FrontierData.PathStyle.BIG_DOT : normalized.endMarker;
        normalized.segmentMarker = normalized.segmentMarker == null ? FrontierData.PathStyle.SMALL_DOT : normalized.segmentMarker;
        normalized.normalizeForPersistence();
        return normalized;
    }

    private static BooleanConfigEntry getDefaultFrontierVisibilityEntry(FrontierVisibility visibility) {
        return switch (visibility) {
            case Frontier -> FRONTIER_DEFAULT_VISIBLE;
            case AnnounceInChat -> FRONTIER_DEFAULT_ANNOUNCE_IN_CHAT;
            case AnnounceInTitle -> FRONTIER_DEFAULT_ANNOUNCE_IN_TITLE;
            case MentionCollection -> FRONTIER_DEFAULT_MENTION_COLLECTION;
            case Fullscreen -> FRONTIER_DEFAULT_FULLSCREEN_VISIBLE;
            case FullscreenName -> FRONTIER_DEFAULT_FULLSCREEN_NAME;
            case FullscreenCollection -> FRONTIER_DEFAULT_FULLSCREEN_COLLECTION;
            case FullscreenOwner -> FRONTIER_DEFAULT_FULLSCREEN_OWNER;
            case FullscreenBanner -> FRONTIER_DEFAULT_FULLSCREEN_BANNER;
            case FullscreenDay -> FRONTIER_DEFAULT_FULLSCREEN_DAY;
            case FullscreenNight -> FRONTIER_DEFAULT_FULLSCREEN_NIGHT;
            case FullscreenUnderground -> FRONTIER_DEFAULT_FULLSCREEN_UNDERGROUND;
            case FullscreenTopo -> FRONTIER_DEFAULT_FULLSCREEN_TOPO;
            case FullscreenBiome -> FRONTIER_DEFAULT_FULLSCREEN_BIOME;
            case Minimap -> FRONTIER_DEFAULT_MINIMAP_VISIBLE;
            case MinimapName -> FRONTIER_DEFAULT_MINIMAP_NAME;
            case MinimapCollection -> FRONTIER_DEFAULT_MINIMAP_COLLECTION;
            case MinimapOwner -> FRONTIER_DEFAULT_MINIMAP_OWNER;
            case MinimapBanner -> FRONTIER_DEFAULT_MINIMAP_BANNER;
            case MinimapDay -> FRONTIER_DEFAULT_MINIMAP_DAY;
            case MinimapNight -> FRONTIER_DEFAULT_MINIMAP_NIGHT;
            case MinimapUnderground -> FRONTIER_DEFAULT_MINIMAP_UNDERGROUND;
            case MinimapTopo -> FRONTIER_DEFAULT_MINIMAP_TOPO;
            case MinimapBiome -> FRONTIER_DEFAULT_MINIMAP_BIOME;
            case Webmap -> FRONTIER_DEFAULT_WEBMAP_VISIBLE;
            case WebmapName -> FRONTIER_DEFAULT_WEBMAP_NAME;
            case WebmapCollection -> FRONTIER_DEFAULT_WEBMAP_COLLECTION;
            case WebmapOwner -> FRONTIER_DEFAULT_WEBMAP_OWNER;
            case WebmapBanner -> FRONTIER_DEFAULT_WEBMAP_BANNER;
            case WebmapDay -> FRONTIER_DEFAULT_WEBMAP_DAY;
            case WebmapNight -> FRONTIER_DEFAULT_WEBMAP_NIGHT;
            case WebmapUnderground -> FRONTIER_DEFAULT_WEBMAP_UNDERGROUND;
            case WebmapTopo -> FRONTIER_DEFAULT_WEBMAP_TOPO;
            case WebmapBiome -> FRONTIER_DEFAULT_WEBMAP_BIOME;
        };
    }

    private static BooleanConfigEntry getDefaultCollectionBooleanVisibilityEntry(CollectionVisibilityField field) {
        return switch (field) {
            case Visible -> COLLECTION_DEFAULT_VISIBLE;
            case FullscreenName -> COLLECTION_DEFAULT_FULLSCREEN_NAME;
            case FullscreenOwner -> COLLECTION_DEFAULT_FULLSCREEN_OWNER;
            case FullscreenBanner -> COLLECTION_DEFAULT_FULLSCREEN_BANNER;
            case MinimapName -> COLLECTION_DEFAULT_MINIMAP_NAME;
            case MinimapOwner -> COLLECTION_DEFAULT_MINIMAP_OWNER;
            case MinimapBanner -> COLLECTION_DEFAULT_MINIMAP_BANNER;
            case WebmapName -> COLLECTION_DEFAULT_WEBMAP_NAME;
            case WebmapOwner -> COLLECTION_DEFAULT_WEBMAP_OWNER;
            case WebmapBanner -> COLLECTION_DEFAULT_WEBMAP_BANNER;
            default -> throw new IllegalArgumentException("Field " + field + " is not a default collection boolean visibility field");
        };
    }

    private static IntConfigEntry getDefaultCollectionZoomVisibilityEntry(CollectionVisibilityField field) {
        return switch (field) {
            case FullscreenZoom -> COLLECTION_DEFAULT_FULLSCREEN_ZOOM;
            case MinimapZoom -> COLLECTION_DEFAULT_MINIMAP_ZOOM;
            case WebmapZoom -> COLLECTION_DEFAULT_WEBMAP_ZOOM;
            default -> throw new IllegalArgumentException("Field " + field + " is not a default collection zoom visibility field");
        };
    }

    private static Identifier parsePathMarker(String value, Identifier fallback) {
        try {
            return Identifier.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static BooleanConfigEntry boolEntry(boolean defaultValue, String... path) {
        return new BooleanConfigEntry(defaultValue, path);
    }

    private static IntConfigEntry intEntry(int defaultValue, int minValue, int maxValue, String... path) {
        return new IntConfigEntry(defaultValue, minValue, maxValue, path);
    }

    private static ColorConfigEntry colorEntry(int defaultValue, String... path) {
        return new ColorConfigEntry(defaultValue, path);
    }

    private static DoubleConfigEntry doubleEntry(double defaultValue, double minValue, double maxValue, String... path) {
        return new DoubleConfigEntry(defaultValue, minValue, maxValue, path);
    }

    private static StringConfigEntry stringEntry(String defaultValue, String... path) {
        return new StringConfigEntry(defaultValue, path);
    }

    private static <E extends Enum<E>> EnumConfigEntry<E> enumEntry(Class<E> enumClass, E defaultValue, String... path) {
        return new EnumConfigEntry<>(enumClass, defaultValue, path);
    }

    private static StringListConfigEntry stringListEntry(List<String> defaultValue, java.util.function.Predicate<String> validator, String... path) {
        return new StringListConfigEntry(defaultValue, validator, path);
    }

    private static BooleanListConfigEntry booleanListEntry(List<Boolean> defaultValue, String... path) {
        return new BooleanListConfigEntry(defaultValue, path);
    }

    private static EnumConfigEntry<FrontierDisplayVisibility> frontierVisibilityEntry(String comment, String... path) {
        return register(enumEntry(FrontierDisplayVisibility.class, FrontierDisplayVisibility.Custom, path)
                .comment(comment));
    }

    private static EnumConfigEntry<FrontierDisplayVisibility> collectionVisibilityEntry(String comment, String... path) {
        return register(enumEntry(FrontierDisplayVisibility.class, FrontierDisplayVisibility.Custom, path)
                .comment(comment));
    }

    private ClientConfig() {
    }
}
