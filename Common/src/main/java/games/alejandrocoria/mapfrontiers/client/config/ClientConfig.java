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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientConfig {
    public enum AfterCreatingFrontier {
        Info, Edit, Nothing
    }

    public enum Visibility {
        Custom, Always, Never
    }

    public enum Sorting {
        Name, Owner, VertexChunk, Area, Modified, Created
    }

    public enum FilterFrontierType {
        All, Global, Personal
    }

    public enum FilterFrontierOwner {
        All, You, Others
    }

    public enum HUDAnchor {
        ScreenTop, ScreenTopRight, ScreenRight, ScreenBottomRight, ScreenBottom, ScreenBottomLeft, ScreenLeft, ScreenTopLeft,
        Minimap, MinimapHorizontal, MinimapVertical
    }

    public enum HUDSlot {
        None, Name, Owner, Banner
    }

    public enum TextColor {
        Frontier, Bright, White
    }

    private static final List<String> DEFAULT_SORTING = List.of(
            Sorting.Created.name(),
            Sorting.Name.name(),
            Sorting.Owner.name(),
            Sorting.VertexChunk.name(),
            Sorting.Area.name(),
            Sorting.Modified.name()
    );
    private static final List<Boolean> DEFAULT_SORTING_DIRECTION = List.of(false, true, true, true, true, false);
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory().resolve(MapFrontiers.MODID + "-client.toml");
    private static final ConfigFile FILE = new ConfigFile(CONFIG_PATH);
    private static boolean initialized = false;

    public static final IntConfigEntry NEW_FRONTIER_SHAPE = register(intEntry(0, 0, 11, "newFrontierShape"));
    public static final IntConfigEntry NEW_FRONTIER_COUNT = register(intEntry(16, 3, 999, "newFrontierVertexCount"));
    public static final IntConfigEntry NEW_FRONTIER_SHAPE_WIDTH = register(intEntry(10, 0, 999, "newFrontierShapeWidth"));
    public static final IntConfigEntry NEW_FRONTIER_SHAPE_RADIUS = register(intEntry(20, 0, 999, "newFrontierShapeRadius"));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE = register(intEntry(0, 0, 7, "newFrontierChunkShape"));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_WIDTH = register(intEntry(5, 0, 32, "newFrontierChunkShapeWidth"));
    public static final IntConfigEntry NEW_FRONTIER_CHUNK_SHAPE_LENGTH = register(intEntry(5, 0, 32, "newFrontierChunkShapeLength"));
    public static final EnumConfigEntry<FrontierData.Mode> NEW_FRONTIER_MODE = register(enumEntry(FrontierData.Mode.class, FrontierData.Mode.Vertex, "newFrontierMode"));
    public static final EnumConfigEntry<AfterCreatingFrontier> AFTER_CREATING_FRONTIER = register(enumEntry(AfterCreatingFrontier.class, AfterCreatingFrontier.Info, "afterCreatingFrontier"));

    public static final BooleanConfigEntry PASTE_NAME = register(boolEntry(false, "pasteName"));
    public static final BooleanConfigEntry PASTE_VISIBILITY = register(boolEntry(true, "pasteVisibility"));
    public static final BooleanConfigEntry PASTE_COLOR = register(boolEntry(true, "pasteColor"));
    public static final BooleanConfigEntry PASTE_BANNER = register(boolEntry(true, "pasteBanner"));
    public static final BooleanConfigEntry PASTE_OPTIONS_VISIBLE = register(boolEntry(false, "pasteOptionsVisible"));

    public static final EnumConfigEntry<Visibility> FRONTIER_VISIBILITY = visibilityEntry("frontierVisibility",
            "Force all frontier to be shown or hidden. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> ANNOUNCE_IN_CHAT = visibilityEntry("announceInChat",
            "Force all frontier to be announced in chat. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> ANNOUNCE_IN_TITLE = visibilityEntry("announceInTitle",
            "Force all frontier to be announced as a title. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_VISIBILITY = visibilityEntry("fullscreenVisibility",
            "Force all frontier to be shown or hidden on the fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_NAME_VISIBILITY = visibilityEntry("fullscreenNameVisibility",
            "Force all frontier names to be shown or hidden on the fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_OWNER_VISIBILITY = visibilityEntry("fullscreenOwnerVisibility",
            "Force all frontier owners to be shown or hidden on the fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_BANNER_VISIBILITY = visibilityEntry("fullscreenBannerVisibility",
            "Force all frontier banners to be shown or hidden on the fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_DAY_VISIBILITY = visibilityEntry("fullscreenDayVisibility",
            "Force all frontier to be shown or hidden on the day fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_NIGHT_VISIBILITY = visibilityEntry("fullscreenNightVisibility",
            "Force all frontier to be shown or hidden on the night fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_UNDERGROUND_VISIBILITY = visibilityEntry("fullscreenUndergroundVisibility",
            "Force all frontier to be shown or hidden on the underground fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_TOPO_VISIBILITY = visibilityEntry("fullscreenTopoVisibility",
            "Force all frontier to be shown or hidden on the topo fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> FULLSCREEN_BIOME_VISIBILITY = visibilityEntry("fullscreenBiomeVisibility",
            "Force all frontier to be shown or hidden on the biome fullscreen map. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_VISIBILITY = visibilityEntry("minimapVisibility",
            "Force all frontier to be shown or hidden on the minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_NAME_VISIBILITY = visibilityEntry("minimapNameVisibility",
            "Force all frontier names to be shown or hidden on the minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_OWNER_VISIBILITY = visibilityEntry("minimapOwnerVisibility",
            "Force all frontier owners to be shown or hidden on the minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_BANNER_VISIBILITY = visibilityEntry("minimapBannerVisibility",
            "Force all frontier banners to be shown or hidden on the minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_DAY_VISIBILITY = visibilityEntry("minimapDayVisibility",
            "Force all frontier to be shown or hidden on the day minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_NIGHT_VISIBILITY = visibilityEntry("minimapNightVisibility",
            "Force all frontier to be shown or hidden on the night minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_UNDERGROUND_VISIBILITY = visibilityEntry("minimapUndergroundVisibility",
            "Force all frontier to be shown or hidden on the underground minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_TOPO_VISIBILITY = visibilityEntry("minimapTopoVisibility",
            "Force all frontier to be shown or hidden on the topo minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> MINIMAP_BIOME_VISIBILITY = visibilityEntry("minimapBiomeVisibility",
            "Force all frontier to be shown or hidden on the biome minimap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_VISIBILITY = visibilityEntry("webmapVisibility",
            "Force all frontier to be shown or hidden on the webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_NAME_VISIBILITY = visibilityEntry("webmapNameVisibility",
            "Force all frontier names to be shown or hidden on the webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_OWNER_VISIBILITY = visibilityEntry("webmapOwnerVisibility",
            "Force all frontier owners to be shown or hidden on the webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_BANNER_VISIBILITY = visibilityEntry("webmapBannerVisibility",
            "Force all frontier banners to be shown or hidden on the webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_DAY_VISIBILITY = visibilityEntry("webmapDayVisibility",
            "Force all frontier to be shown or hidden on the day webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_NIGHT_VISIBILITY = visibilityEntry("webmapNightVisibility",
            "Force all frontier to be shown or hidden on the night webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_UNDERGROUND_VISIBILITY = visibilityEntry("webmapUndergroundVisibility",
            "Force all frontier to be shown or hidden on the underground webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_TOPO_VISIBILITY = visibilityEntry("webmapTopoVisibility",
            "Force all frontier to be shown or hidden on the topo webmap. In Custom you can decide for each frontier.");
    public static final EnumConfigEntry<Visibility> WEBMAP_BIOME_VISIBILITY = visibilityEntry("webmapBiomeVisibility",
            "Force all frontier to be shown or hidden on the biome webmap. In Custom you can decide for each frontier.");

    public static final IntConfigEntry TITLE_ANNOUNCEMENT_DURATION = register(intEntry(70, 0, 1200, "titleAnnouncementDuration")
            .comment("Duration of title announcement, in game ticks.")
            .translation(translation("titleAnnouncementDuration")));
    public static final IntConfigEntry TITLE_ANNOUNCEMENT_TIMEOUT = register(intEntry(0, 0, 1200, "titleAnnouncementTimeout")
            .comment("Minimum time between consecutive title announcement, in game ticks.")
            .translation(translation("titleAnnouncementTimeout")));
    public static final BooleanConfigEntry TITLE_ANNOUNCEMENT_ABOVE_HOTBAR = register(boolEntry(false, "titleAnnouncementAboveHotbar")
            .comment("Show the frontier announcement above the hotbar instead of showing it as a title.")
            .translation(translation("titleAnnouncementAboveHotbar")));
    public static final BooleanConfigEntry ANNOUNCE_UNNAMED_FRONTIERS = register(boolEntry(false, "announceUnnamedFrontiers")
            .comment("Announce unnamed frontiers in chat/title.")
            .translation(translation("announceUnnamedFrontiers")));
    public static final IntConfigEntry SNAP_DISTANCE = register(intEntry(8, 0, 16, "snapDistance")
            .comment("Distance at which vertices are attached to nearby vertices.")
            .translation(translation("snapDistance")));

    public static final StringConfigEntry SEND_COMMAND = register(stringEntry("msg", "sendCommand"));

    public static final BooleanConfigEntry HIDE_NAMES_THAT_DONT_FIT = register(boolEntry(false, "hideNamesThatDontFit")
            .comment("Hides the name if it is wider than the frontier at the zoom level it is being viewed.")
            .translation(translation("hideNamesThatDontFit")));
    public static final DoubleConfigEntry POLYGONS_OPACITY = register(doubleEntry(0.4, 0.0, 1.0, "polygonsOpacity")
            .comment("Transparency of the frontier polygons. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("polygonsOpacity")));
    public static final IntConfigEntry BORDER_WIDTH = register(intEntry(0, 0, 64, "borderWidth")
            .comment("Width of the frontier border.")
            .translation(translation("borderWidth")));
    public static final DoubleConfigEntry BORDER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "borderOpacity")
            .comment("Transparency of the frontier border. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("borderOpacity")));
    public static final IntConfigEntry TEXT_SIZE = register(intEntry(2, 1, 5, "textSize")
            .comment("Size of the frontier text.")
            .translation(translation("textSize")));
    public static final DoubleConfigEntry TEXT_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "textOpacity")
            .comment("Transparency of the frontier text. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("textOpacity")));
    public static final EnumConfigEntry<TextColor> TEXT_COLOR = register(enumEntry(TextColor.class, TextColor.Frontier, "textColor")
            .comment("Color of the frontier text. Frontier will use the frontier color. Bright will also use the same color but with maximum brightness.")
            .translation(translation("textColor")));
    public static final IntConfigEntry BANNER_SIZE = register(intEntry(1, 1, 5, "bannerSize")
            .comment("Size of the frontier banner.")
            .translation(translation("bannerSize")));
    public static final DoubleConfigEntry BANNER_OPACITY = register(doubleEntry(1.0, 0.0, 1.0, "bannerOpacity")
            .comment("Transparency of the frontier banner. 0.0 is fully transparent and 1.0 is opaque.")
            .translation(translation("bannerOpacity")));

    public static final BooleanConfigEntry FULLSCREEN_BUTTONS = register(boolEntry(true, "fullscreenButtons")
            .comment("Show buttons on fullscreen map.")
            .translation(translation("fullscreenButtons")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_FRONTIER_DELETE = register(boolEntry(true, "askConfirmationFrontierDelete")
            .comment("Show a confirmation dialog before deleting a frontier.")
            .translation(translation("askConfirmationFrontierDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_GROUP_DELETE = register(boolEntry(true, "askConfirmationGroupDelete")
            .comment("Show a confirmation dialog before deleting a group.")
            .translation(translation("askConfirmationGroupDelete")));
    public static final BooleanConfigEntry ASK_CONFIRMATION_USER_DELETE = register(boolEntry(true, "askConfirmationUserDelete")
            .comment("Show a confirmation dialog before deleting an user.")
            .translation(translation("askConfirmationUserDelete")));

    public static final StringListConfigEntry FRONTIER_SORTING = register(stringListEntry(DEFAULT_SORTING, ClientConfig::isValidSorting, "frontierSorting")
            .comment("Order of the frontier list sorting modes."));
    public static final BooleanListConfigEntry FRONTIER_SORTING_DIRECTION = register(booleanListEntry(DEFAULT_SORTING_DIRECTION, "frontierSortingDirection")
            .comment("Direction of the frontier list sorting modes. True means ascending and false means descending."));
    public static final EnumConfigEntry<FilterFrontierType> FILTER_FRONTIER_TYPE = register(enumEntry(FilterFrontierType.class, FilterFrontierType.All, "filterFrontierType"));
    public static final EnumConfigEntry<FilterFrontierOwner> FILTER_FRONTIER_OWNER = register(enumEntry(FilterFrontierOwner.class, FilterFrontierOwner.All, "filterFrontierOwner"));
    public static final StringConfigEntry FILTER_FRONTIER_DIMENSION = register(stringEntry("all", "filterFrontierDimension"));

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
    public static final EnumConfigEntry<HUDAnchor> HUD_ANCHOR = register(enumEntry(HUDAnchor.class, HUDAnchor.MinimapHorizontal, "hud", "anchor")
            .comment("Anchor point of the HUD. In the case of choosing the minimap as an anchor, its default position will be used as a reference in the coordinates.")
            .translation(translation("hud", "anchor")));
    public static final IntConfigEntry HUD_X_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "xPosition"));
    public static final IntConfigEntry HUD_Y_POSITION = register(intEntry(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "hud", "yPosition"));

    public static boolean load() {
        boolean dirty = FILE.load();
        dirty |= validateSorting();
        return dirty;
    }

    public static void save() {
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

    public static List<Sorting> getFrontierSortingValues() {
        return FRONTIER_SORTING.get().stream().map(Sorting::valueOf).toList();
    }

    public static void setFrontierSortingValues(List<Sorting> sorting) {
        FRONTIER_SORTING.set(sorting.stream().map(Enum::name).toList());
    }

    public static List<Boolean> getFrontierSortingDirectionValues() {
        return FRONTIER_SORTING_DIRECTION.get();
    }

    public static void setFrontierSortingDirectionValues(List<Boolean> direction) {
        FRONTIER_SORTING_DIRECTION.set(direction);
    }

    private static boolean validateSorting() {
        List<String> sorting = new ArrayList<>(FRONTIER_SORTING.get());
        List<Boolean> direction = new ArrayList<>(FRONTIER_SORTING_DIRECTION.get());
        boolean dirty = false;

        if (sorting.size() > Sorting.values().length || direction.size() != sorting.size()) {
            FRONTIER_SORTING.set(DEFAULT_SORTING);
            FRONTIER_SORTING_DIRECTION.set(DEFAULT_SORTING_DIRECTION);
            return true;
        }

        List<Sorting> missing = new ArrayList<>();
        for (Sorting sort : Sorting.values()) {
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
        }

        return dirty;
    }

    private static boolean isValidSorting(String value) {
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

    private static EnumConfigEntry<Visibility> visibilityEntry(String name, String comment) {
        return register(enumEntry(Visibility.class, Visibility.Custom, name)
                .comment(comment)
                .translation(translation(name)));
    }

    private ClientConfig() {
    }
}
