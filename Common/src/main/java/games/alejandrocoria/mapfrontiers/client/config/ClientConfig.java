package games.alejandrocoria.mapfrontiers.client.config;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.BooleanListConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigFile;
import games.alejandrocoria.mapfrontiers.common.config.DoubleConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.EnumConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.StringConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.StringListConfigEntry;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class ClientConfig {
    public enum AfterCreatingFrontier {
        InfoScreen, EditShape, DoNothing
    }

    public enum Visibility {
        Custom, Always, Never
    }

    public enum Sorting {
        Name, Owner, Shape, Area, Modified, Created;

        public static final Sorting[] VALUES = values();
    }

    public enum FilterFrontierType {
        All, Global, Personal
    }

    public enum FilterFrontierOwner {
        All, Self, Others
    }

    public enum HUDAnchor {
        ScreenTop, ScreenTopRight, ScreenRight, ScreenBottomRight, ScreenBottom, ScreenBottomLeft, ScreenLeft, ScreenTopLeft,
        Minimap, MinimapHorizontal, MinimapVertical;

        public static final HUDAnchor[] VALUES = values();
    }

    public enum HUDSlot {
        None, Collection, Name, Owner, Banner
    }

    public enum TextColor {
        FrontierColor, FrontierColorBright, White
    }

    private static final List<String> DEFAULT_SORTING = List.of(
            Sorting.Created.name(),
            Sorting.Name.name(),
            Sorting.Owner.name(),
            Sorting.Shape.name(),
            Sorting.Area.name(),
            Sorting.Modified.name()
    );
    private static final List<Boolean> DEFAULT_SORTING_DIRECTION = List.of(false, true, true, true, true, false);

    public static final int CURRENT_VERSION = 1;
    public static final String DIMENSION_FILTER_ALL = "mapfrontiers:all";
    public static final String DIMENSION_FILTER_CURRENT = "mapfrontiers:current";

    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory().resolve(MapFrontiers.MODID + "-client.toml");
    private static final ConfigFile FILE = new ConfigFile(CONFIG_PATH, CURRENT_VERSION, ClientConfigMigrations.INSTANCE);
    private static boolean initialized = false;

    public static final IntConfigEntry TITLE_ANNOUNCEMENT_DURATION = register(intEntry(70, 0, 1200, "announcement", "title", "duration")
            .comment("Duration of title announcement, in game ticks.")
            .translation(translation("announcement", "title", "duration")));
    public static final IntConfigEntry TITLE_ANNOUNCEMENT_TIMEOUT = register(intEntry(0, 0, 1200, "announcement", "title", "timeout")
            .comment("Minimum time between consecutive title announcement, in game ticks.")
            .translation(translation("announcement", "title", "timeout")));
    public static final BooleanConfigEntry TITLE_ANNOUNCEMENT_ABOVE_HOTBAR = register(boolEntry(false, "announcement", "title", "aboveHotbar")
            .comment("Show the frontier announcement above the hotbar instead of showing it as a title.")
            .translation(translation("announcement", "title", "aboveHotbar")));
    public static final BooleanConfigEntry ANNOUNCE_UNNAMED_FRONTIERS = register(boolEntry(false, "announcement", "announceUnnamed")
            .comment("Announce unnamed frontiers in chat/title.")
            .translation(translation("announcement", "announceUnnamed")));

    public static final IntConfigEntry SNAP_DISTANCE = register(intEntry(8, 0, 16, "editing", "snapDistance")
            .comment("Distance at which vertices snap to nearby vertices.")
            .translation(translation("editing", "snapDistance")));

    public static final BooleanConfigEntry HIDE_NAMES_THAT_DONT_FIT = register(boolEntry(false, "appearance", "hideNamesThatDontFit")
            .comment("Hide frontier names when they do not fit at the current zoom level.")
            .translation(translation("appearance", "hideNamesThatDontFit")));
    public static final DoubleConfigEntry POLYGONS_OPACITY = register(doubleEntry(0.4, 0.0, 1.0, "appearance", "polygons", "opacity")
            .comment("Transparency of the frontier polygons. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("appearance", "polygons", "opacity")));
    public static final IntConfigEntry BORDER_WIDTH = register(intEntry(0, 0, 64, "appearance", "border", "width")
            .comment("Width of the frontier border.")
            .translation(translation("appearance", "border", "width")));
    public static final DoubleConfigEntry BORDER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "appearance", "border", "opacity")
            .comment("Transparency of the frontier border. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("appearance", "border", "opacity")));
    public static final IntConfigEntry PATH_MARKER_SIZE = register(intEntry(2, 1, 5, "appearance", "pathMarkers", "size")
            .comment("Size of path markers.")
            .translation(translation("appearance", "pathMarkers", "size")));
    public static final DoubleConfigEntry PATH_MARKER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "appearance", "pathMarkers", "opacity")
            .comment("Transparency of path markers. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("appearance", "pathMarkers", "opacity")));
    public static final IntConfigEntry TEXT_SIZE = register(intEntry(2, 1, 5, "appearance", "text", "size")
            .comment("Size of the frontier text.")
            .translation(translation("appearance", "text", "size")));
    public static final DoubleConfigEntry TEXT_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "appearance", "text", "opacity")
            .comment("Transparency of the frontier text. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("appearance", "text", "opacity")));
    public static final EnumConfigEntry<TextColor> TEXT_COLOR = register(enumEntry(TextColor.class, TextColor.FrontierColor, "appearance", "text", "color")
            .comment("Color of the frontier text. FrontierColor uses the frontier color. FrontierColorBright uses the same color at maximum brightness.")
            .translation(translation("appearance", "text", "color")));
    public static final IntConfigEntry BANNER_SIZE = register(intEntry(1, 1, 5, "appearance", "banner", "size")
            .comment("Size of the frontier banner.")
            .translation(translation("appearance", "banner", "size")));
    public static final DoubleConfigEntry BANNER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "appearance", "banner", "opacity")
            .comment("Transparency of the frontier banner. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("appearance", "banner", "opacity")));
    public static final StringConfigEntry PATH_DEFAULT_STYLE_START = register(stringEntry(FrontierData.PathStyle.BIG_DOT.toString(), "path", "defaultStyle", "start")
            .comment("Marker identifier used by default for the start of new path frontiers."));
    public static final StringConfigEntry PATH_DEFAULT_STYLE_INNER = register(stringEntry(FrontierData.PathStyle.NONE.toString(), "path", "defaultStyle", "inner")
            .comment("Marker identifier used by default for inner points of new path frontiers."));
    public static final StringConfigEntry PATH_DEFAULT_STYLE_END = register(stringEntry(FrontierData.PathStyle.BIG_DOT.toString(), "path", "defaultStyle", "end")
            .comment("Marker identifier used by default for the end of new path frontiers."));
    public static final StringConfigEntry PATH_DEFAULT_STYLE_SEGMENT = register(stringEntry(FrontierData.PathStyle.SMALL_DOT.toString(), "path", "defaultStyle", "segment")
            .comment("Marker identifier used by default for segments of new path frontiers."));
    public static final BooleanConfigEntry PATH_DEFAULT_STYLE_LABEL_AT_START = register(boolEntry(true, "path", "defaultStyle", "labelAtStart")
            .comment("Show path labels and banner at the start by default."));
    public static final BooleanConfigEntry PATH_DEFAULT_STYLE_LABEL_AT_MIDDLE = register(boolEntry(false, "path", "defaultStyle", "labelAtMiddle")
            .comment("Show path labels and banner at the midpoint by default."));
    public static final BooleanConfigEntry PATH_DEFAULT_STYLE_LABEL_AT_END = register(boolEntry(false, "path", "defaultStyle", "labelAtEnd")
            .comment("Show path labels and banner at the end by default."));
    public static final IntConfigEntry PATH_PROXIMITY_ENTER_DISTANCE = register(intEntry(8, 0, 128, "path", "proximity", "enterDistance")
            .comment("Distance in blocks used to activate Path frontiers for HUD and announcements.")
            .translation(translation("path", "proximity", "enterDistance")));
    public static final IntConfigEntry PATH_PROXIMITY_EXIT_DISTANCE = register(intEntry(10, 0, 128, "path", "proximity", "exitDistance")
            .comment("Distance in blocks used to keep Path frontiers active for HUD and announcements.")
            .translation(translation("path", "proximity", "exitDistance")));

    public static final EnumConfigEntry<Visibility> FRONTIER_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden. In Custom, you can decide for each frontier.",
            "visibility", "frontier");
    public static final EnumConfigEntry<Visibility> ANNOUNCE_IN_CHAT = visibilityEntry(
            "Force all frontiers to be announced in chat. In Custom, you can decide for each frontier.",
            "visibility", "announceInChat");
    public static final EnumConfigEntry<Visibility> ANNOUNCE_IN_TITLE = visibilityEntry(
            "Force all frontiers to be announced as titles. In Custom, you can decide for each frontier.",
            "visibility", "announceInTitle");
    public static final EnumConfigEntry<Visibility> MENTION_COLLECTION = visibilityEntry(
            "Force collection names to be mentioned in local frontier announcements. In Custom, you can decide for each frontier.",
            "visibility", "mentionCollection");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "frontier");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_NAME_VISIBILITY = visibilityEntry(
            "Force all frontier names to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "name");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_COLLECTION_VISIBILITY = visibilityEntry(
            "Force all frontier collection names to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "collection");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_OWNER_VISIBILITY = visibilityEntry(
            "Force all frontier owners to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "owner");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_BANNER_VISIBILITY = visibilityEntry(
            "Force all frontier banners to be shown or hidden on the fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "banner");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_DAY_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the day fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "day");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_NIGHT_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the night fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "night");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_UNDERGROUND_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the underground fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "underground");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_TOPO_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the topo fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "topo");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_BIOME_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the biome fullscreen map. In Custom, you can decide for each frontier.",
            "visibility", "fullscreen", "biome");
    public static final EnumConfigEntry<Visibility> MINIMAP_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "frontier");
    public static final EnumConfigEntry<Visibility> MINIMAP_NAME_VISIBILITY = visibilityEntry(
            "Force all frontier names to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "name");
    public static final EnumConfigEntry<Visibility> MINIMAP_COLLECTION_VISIBILITY = visibilityEntry(
            "Force all frontier collection names to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "collection");
    public static final EnumConfigEntry<Visibility> MINIMAP_OWNER_VISIBILITY = visibilityEntry(
            "Force all frontier owners to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "owner");
    public static final EnumConfigEntry<Visibility> MINIMAP_BANNER_VISIBILITY = visibilityEntry(
            "Force all frontier banners to be shown or hidden on the minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "banner");
    public static final EnumConfigEntry<Visibility> MINIMAP_DAY_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the day minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "day");
    public static final EnumConfigEntry<Visibility> MINIMAP_NIGHT_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the night minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "night");
    public static final EnumConfigEntry<Visibility> MINIMAP_UNDERGROUND_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the underground minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "underground");
    public static final EnumConfigEntry<Visibility> MINIMAP_TOPO_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the topo minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "topo");
    public static final EnumConfigEntry<Visibility> MINIMAP_BIOME_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the biome minimap. In Custom, you can decide for each frontier.",
            "visibility", "minimap", "biome");
    public static final EnumConfigEntry<Visibility> WEBMAP_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "frontier");
    public static final EnumConfigEntry<Visibility> WEBMAP_NAME_VISIBILITY = visibilityEntry(
            "Force all frontier names to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "name");
    public static final EnumConfigEntry<Visibility> WEBMAP_COLLECTION_VISIBILITY = visibilityEntry(
            "Force all frontier collection names to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "collection");
    public static final EnumConfigEntry<Visibility> WEBMAP_OWNER_VISIBILITY = visibilityEntry(
            "Force all frontier owners to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "owner");
    public static final EnumConfigEntry<Visibility> WEBMAP_BANNER_VISIBILITY = visibilityEntry(
            "Force all frontier banners to be shown or hidden on the webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "banner");
    public static final EnumConfigEntry<Visibility> WEBMAP_DAY_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the day webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "day");
    public static final EnumConfigEntry<Visibility> WEBMAP_NIGHT_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the night webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "night");
    public static final EnumConfigEntry<Visibility> WEBMAP_UNDERGROUND_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the underground webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "underground");
    public static final EnumConfigEntry<Visibility> WEBMAP_TOPO_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the topo webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "topo");
    public static final EnumConfigEntry<Visibility> WEBMAP_BIOME_VISIBILITY = visibilityEntry(
            "Force all frontiers to be shown or hidden on the biome webmap. In Custom, you can decide for each frontier.",
            "visibility", "webmap", "biome");

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
    public static final EnumConfigEntry<HUDSlot> HUD_SLOT_1 = register(enumEntry(HUDSlot.class, HUDSlot.Name, "hud", "slot1")
            .comment("HUD element on slot 1.")
            .translation(translation("hud", "slot1")));
    public static final EnumConfigEntry<HUDSlot> HUD_SLOT_2 = register(enumEntry(HUDSlot.class, HUDSlot.Owner, "hud", "slot2")
            .comment("HUD element on slot 2.")
            .translation(translation("hud", "slot2")));
    public static final EnumConfigEntry<HUDSlot> HUD_SLOT_3 = register(enumEntry(HUDSlot.class, HUDSlot.Banner, "hud", "slot3")
            .comment("HUD element on slot 3.")
            .translation(translation("hud", "slot3")));
    public static final List<EnumConfigEntry<HUDSlot>> HUD_SLOTS = List.of(HUD_SLOT_1, HUD_SLOT_2, HUD_SLOT_3);
    public static final EnumConfigEntry<HUDAnchor> HUD_ANCHOR = register(enumEntry(HUDAnchor.class, HUDAnchor.MinimapHorizontal, "hud", "anchor")
            .comment("Anchor point of the HUD. When anchored to the minimap, coordinates are relative to the minimap's default position.")
            .translation(translation("hud", "anchor")));
    public static final IntConfigEntry HUD_X_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "xPosition")
            .comment("Horizontal HUD offset relative to the selected anchor."));
    public static final IntConfigEntry HUD_Y_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "yPosition")
            .comment("Vertical HUD offset relative to the selected anchor."));

    public static final EnumConfigEntry<FrontierData.Mode> NEW_FRONTIER_MODE = register(enumEntry(FrontierData.Mode.class, FrontierData.Mode.Vertex, "newFrontier", "mode")
            .comment("Mode used when creating a new frontier."));
    public static final EnumConfigEntry<AfterCreatingFrontier> AFTER_CREATING_FRONTIER = register(enumEntry(AfterCreatingFrontier.class, AfterCreatingFrontier.InfoScreen, "newFrontier", "afterCreation")
            .comment("Action to perform after creating a new frontier."));
    public static final IntConfigEntry NEW_FRONTIER_SHAPE = register(intEntry(6, 0, 11, "newFrontier", "shape")
            .comment("Shape preset used when creating a new vertex frontier."));
    public static final IntConfigEntry NEW_FRONTIER_COUNT = register(intEntry(16, 3, 999, "newFrontier", "vertexCount")
            .comment("Number of vertices used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_SHAPE_WIDTH = register(intEntry(10, 0, 999, "newFrontier", "shapeWidth")
            .comment("Width used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_SHAPE_RADIUS = register(intEntry(20, 0, 999, "newFrontier", "shapeRadius")
            .comment("Radius used by the selected vertex shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE = register(intEntry(2, 0, 7, "newFrontier", "chunkShape")
            .comment("Shape preset used when creating a new chunk frontier."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_WIDTH = register(intEntry(5, 0, 32, "newFrontier", "chunkShapeWidth")
            .comment("Width used by the selected chunk shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_LENGTH = register(intEntry(5, 0, 32, "newFrontier", "chunkShapeLength")
            .comment("Length used by the selected chunk shape preset."));
    public static final IntConfigEntry NEW_FRONTIER_PATH_SHAPE = register(intEntry(1, 0, 7, "newFrontier", "pathShape")
            .comment("Shape preset used when creating a new path frontier."));
    public static final IntConfigEntry NEW_FRONTIER_PATH_SEGMENT_LENGTH = register(intEntry(10, 1, 999, "newFrontier", "pathSegmentLength")
            .comment("Segment length used by the selected path shape preset."));

    public static final BooleanConfigEntry PASTE_NAME = register(boolEntry(false, "paste", "name")
            .comment("Paste the frontier name when pasting info."));
    public static final BooleanConfigEntry PASTE_VISIBILITY = register(boolEntry(true, "paste", "visibility")
            .comment("Paste visibility settings when pasting info."));
    public static final BooleanConfigEntry PASTE_PATH_STYLE = register(boolEntry(true, "paste", "pathStyle")
            .comment("Paste the frontier path style when pasting info."));
    public static final BooleanConfigEntry PASTE_COLOR = register(boolEntry(true, "paste", "color")
            .comment("Paste the frontier color when pasting info."));
    public static final BooleanConfigEntry PASTE_BANNER = register(boolEntry(true, "paste", "banner")
            .comment("Paste the frontier banner when pasting info."));
    public static final BooleanConfigEntry PASTE_OPTIONS_VISIBLE = register(boolEntry(false, "paste", "optionsVisible")
            .comment("Whether paste options are currently expanded."));

    static {
        FILE.registerSectionComment("list", "Frontier list settings.");
        FILE.registerSectionComment("path", "Path settings.");
    }

    public static final StringListConfigEntry FRONTIER_SORTING = register(stringListEntry(DEFAULT_SORTING, ClientConfig::isValidSorting, "list", "sorting", "priority")
            .comment("Order of the frontier list sorting modes."));
    public static final BooleanListConfigEntry FRONTIER_SORTING_DIRECTION = register(booleanListEntry(DEFAULT_SORTING_DIRECTION, "list", "sorting", "directions")
            .comment("Direction of the frontier list sorting modes. True means ascending and false means descending."));
    public static final EnumConfigEntry<FilterFrontierType> FILTER_FRONTIER_TYPE = register(enumEntry(FilterFrontierType.class, FilterFrontierType.All, "list", "filters", "type")
            .comment("Selected frontier type filter in the frontier list."));
    public static final EnumConfigEntry<FilterFrontierOwner> FILTER_FRONTIER_OWNER = register(enumEntry(FilterFrontierOwner.class, FilterFrontierOwner.All, "list", "filters", "owner")
            .comment("Selected frontier owner filter in the frontier list."));
    public static final StringConfigEntry FILTER_FRONTIER_DIMENSION = register(stringEntry(DIMENSION_FILTER_ALL, "list", "filters", "dimension")
            .comment("Selected dimension filter in the frontier list. Use \"" + DIMENSION_FILTER_ALL + "\" to show every dimension or \"" + DIMENSION_FILTER_CURRENT + "\" to show only the current one."));

    public static final StringConfigEntry SEND_COMMAND = register(stringEntry("msg", "chatSharing", "sendCommand")
            .comment("Chat command used to send shared frontiers to another player."));

    public static boolean load() {
        boolean dirty = FILE.load();
        dirty |= validateDefaultPathStyle();
        dirty |= validatePathActivationDistances();
        dirty |= validateSorting();
        return dirty;
    }

    public static void save() {
        validatePathActivationDistances();
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

    public static boolean getVisibilityValue(Visibility visibility, boolean customValue) {
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
        List<HUDSlot> configuredSlots = HUD_SLOTS.stream().map(ConfigEntry::get).toList();
        List<HUDSlot> resolvedSlots = new ArrayList<>(configuredSlots.size());
        EnumSet<HUDSlot> seenSlots = EnumSet.noneOf(HUDSlot.class);

        for (HUDSlot slot : configuredSlots) {
            if (slot == HUDSlot.None || seenSlots.add(slot)) {
                resolvedSlots.add(slot);
            } else {
                resolvedSlots.add(HUDSlot.None);
            }
        }

        return resolvedSlots;
    }

    public static List<Sorting> getFrontierSortingValues() {
        return FRONTIER_SORTING.get().stream().map(Sorting::valueOf).toList();
    }

    public static void setFrontierSortingValues(List<Sorting> sorting) {
        FRONTIER_SORTING.set(sorting.stream().map(Enum::name).toList());
    }

    public static List<Boolean> getFrontierSortingDirectionValues() {
        return FRONTIER_SORTING_DIRECTION.get();
    }

    public static FrontierData.PathStyle getDefaultPathStyle() {
        FrontierData.PathStyle pathStyle = new FrontierData.PathStyle();
        pathStyle.startMarker = parsePathMarker(PATH_DEFAULT_STYLE_START.get(), FrontierData.PathStyle.BIG_DOT);
        pathStyle.innerMarker = parsePathMarker(PATH_DEFAULT_STYLE_INNER.get(), FrontierData.PathStyle.NONE);
        pathStyle.endMarker = parsePathMarker(PATH_DEFAULT_STYLE_END.get(), FrontierData.PathStyle.BIG_DOT);
        pathStyle.segmentMarker = parsePathMarker(PATH_DEFAULT_STYLE_SEGMENT.get(), FrontierData.PathStyle.SMALL_DOT);
        pathStyle.labelAtStart = PATH_DEFAULT_STYLE_LABEL_AT_START.get();
        pathStyle.labelAtMiddle = PATH_DEFAULT_STYLE_LABEL_AT_MIDDLE.get();
        pathStyle.labelAtEnd = PATH_DEFAULT_STYLE_LABEL_AT_END.get();
        pathStyle.normalizeForPersistence();
        return pathStyle;
    }

    public static void setDefaultPathStyle(FrontierData.PathStyle pathStyle) {
        FrontierData.PathStyle normalized = normalizeDefaultPathStyle(pathStyle);
        PATH_DEFAULT_STYLE_START.set(normalized.startMarker.toString());
        PATH_DEFAULT_STYLE_INNER.set(normalized.innerMarker.toString());
        PATH_DEFAULT_STYLE_END.set(normalized.endMarker.toString());
        PATH_DEFAULT_STYLE_SEGMENT.set(normalized.segmentMarker.toString());
        PATH_DEFAULT_STYLE_LABEL_AT_START.set(normalized.labelAtStart);
        PATH_DEFAULT_STYLE_LABEL_AT_MIDDLE.set(normalized.labelAtMiddle);
        PATH_DEFAULT_STYLE_LABEL_AT_END.set(normalized.labelAtEnd);
    }

    public static double getPathActivationDistance(boolean alreadyActive) {
        return alreadyActive ? PATH_PROXIMITY_EXIT_DISTANCE.get() : PATH_PROXIMITY_ENTER_DISTANCE.get();
    }

    public static void setFrontierSortingDirectionValues(List<Boolean> direction) {
        FRONTIER_SORTING_DIRECTION.set(direction);
    }

    private static boolean validateDefaultPathStyle() {
        FrontierData.PathStyle normalized = getDefaultPathStyle();
        boolean dirty = !PATH_DEFAULT_STYLE_START.get().equals(normalized.startMarker.toString())
                || !PATH_DEFAULT_STYLE_INNER.get().equals(normalized.innerMarker.toString())
                || !PATH_DEFAULT_STYLE_END.get().equals(normalized.endMarker.toString())
                || !PATH_DEFAULT_STYLE_SEGMENT.get().equals(normalized.segmentMarker.toString())
                || PATH_DEFAULT_STYLE_LABEL_AT_START.get() != normalized.labelAtStart
                || PATH_DEFAULT_STYLE_LABEL_AT_MIDDLE.get() != normalized.labelAtMiddle
                || PATH_DEFAULT_STYLE_LABEL_AT_END.get() != normalized.labelAtEnd;

        if (dirty) {
            setDefaultPathStyle(normalized);
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

    private static boolean validateSorting() {
        List<String> sorting = new ArrayList<>(FRONTIER_SORTING.get());
        List<Boolean> direction = new ArrayList<>(FRONTIER_SORTING_DIRECTION.get());
        boolean dirty = false;

        for (int i = 0; i < sorting.size(); ++i) {
            if (sorting.get(i).equals("VertexChunk")) {
                sorting.set(i, Sorting.Shape.name());
                dirty = true;
            }
        }

        if (sorting.size() > Sorting.VALUES.length || direction.size() != sorting.size()) {
            FRONTIER_SORTING.set(DEFAULT_SORTING);
            FRONTIER_SORTING_DIRECTION.set(DEFAULT_SORTING_DIRECTION);
            return true;
        }

        List<Sorting> missing = new ArrayList<>();
        for (Sorting sort : Sorting.VALUES) {
            int count = Collections.frequency(sorting, sort.name());
            if (count > 1) {
                FRONTIER_SORTING.set(DEFAULT_SORTING);
                FRONTIER_SORTING_DIRECTION.set(DEFAULT_SORTING_DIRECTION);
                return true;
            }
            if (count == 0) {
                missing.add(sort);
            }
        }

        if (!missing.isEmpty()) {
            dirty = true;
            for (Sorting missingSort : missing) {
                sorting.add(missingSort.name());
                int defaultIndex = DEFAULT_SORTING.indexOf(missingSort.name());
                direction.add(DEFAULT_SORTING_DIRECTION.get(defaultIndex));
            }

            FRONTIER_SORTING.set(sorting);
            FRONTIER_SORTING_DIRECTION.set(direction);
        } else if (dirty) {
            FRONTIER_SORTING.set(sorting);
        }

        return dirty;
    }

    private static boolean isValidSorting(String value) {
        if (value.equals("VertexChunk")) {
            return true;
        }

        try {
            Sorting.valueOf(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static <T extends ConfigEntry<?, ?>> T register(T entry) {
        return FILE.register(entry);
    }

    private static String translation(String... path) {
        return MapFrontiers.MODID + ".config." + String.join(".", path);
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

    private static EnumConfigEntry<Visibility> visibilityEntry(String comment, String... path) {
        return register(enumEntry(Visibility.class, Visibility.Custom, path)
                .comment(comment));
    }

    private ClientConfig() {
    }
}
