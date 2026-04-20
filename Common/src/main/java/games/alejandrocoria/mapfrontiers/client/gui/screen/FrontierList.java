package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import games.alejandrocoria.mapfrontiers.client.gui.component.SortToolbar;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.FrontierListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.RadioListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.common.config.EnumConfigEntry;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontierList extends AutoScaledScreen {
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_frontiers");
    private static final Component resetFiltersLabel = Component.translatable("mapfrontiers.reset_filters");
    private static final Component filterTypeLabel = Component.translatable("mapfrontiers.filter_type");
    private static final Component filterOwnerLabel = Component.translatable("mapfrontiers.filter_owner");
    private static final Component filterDimensionLabel = Component.translatable("mapfrontiers.filter_dimension");
    private static final Component configAllLabel = Component.translatable("mapfrontiers.config.All");
    private static final Component configCurrentLabel = Component.translatable("mapfrontiers.config.Current");
    private static final Component overworldLabel = Component.literal("minecraft:overworld");
    private static final Component theNetherLabel = Component.literal("minecraft:the_nether");
    private static final Component theEndLabel = Component.literal("minecraft:the_end");
    private static final Component createLabel = Component.translatable("mapfrontiers.create");
    private static final Component infoLabel = Component.translatable("mapfrontiers.info");
    private static final Component deleteLabel = Component.translatable("mapfrontiers.delete");
    private static final Component hideLabel = Component.translatable("mapfrontiers.hide");
    private static final Component settingsLabel = Component.translatable("mapfrontiers.settings");
    private static final Component doneLabel = Component.translatable("gui.done");

    private final IClientAPI jmAPI;
    private final FullscreenMap fullscreenMap;

    private TextBox searchBox;
    private ScrollBox frontiers;
    private ScrollBox filterType;
    private ScrollBox filterOwner;
    private ScrollBox filterDimension;
    private SimpleButton buttonCreate;
    private SimpleButton buttonInfo;
    private SimpleButton buttonDelete;
    private SimpleButton buttonVisible;
    private SimpleButton buttonSettings;

    public FrontierList(IClientAPI jmAPI, FullscreenMap fullscreenMap) {
        super(titleLabel, 778, 302);
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getFrontierEvents().subscribeCreated(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            updateButtons();
        });
    }

    @Override
    public void initScreen() {
        GridLayout mainLayout = createMainLayout();

        buildToolbar(mainLayout);
        buildFrontiersList(mainLayout);
        buildFiltersColumn(mainLayout);

        buildBottomButtons();

        updateFrontiers();
        refreshInitialSelection();
        refreshViewState();
    }

    @Override
    public void repositionElements() {
        frontiers.setSize(450, actualHeight - 100);
        filterDimension.setSize(200, actualHeight - 269);
        super.repositionElements();
        content.setPosition((actualWidth - content.getWidth()) / 2, 60);
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, actualWidth - 60, actualHeight - 60);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox) {
                ((ScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        super.onClose();
    }

    private GridLayout createMainLayout() {
        GridLayout mainLayout = new GridLayout().columnSpacing(8).rowSpacing(4);
        content.addChild(mainLayout);
        return mainLayout;
    }

    private void buildToolbar(GridLayout mainLayout) {
        LinearLayout toolbar = LinearLayout.horizontal();
        toolbar.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(toolbar, 0, 0, LayoutSettings.defaults().alignHorizontallyLeft());

        toolbar.addChild(new SortToolbar(font, this::updateFrontiers));
        toolbar.addChild(SpacerElement.width(16));

        searchBox = new TextBox(font, 100, I18n.get("mapfrontiers.search"));
        searchBox.setMaxLength(40);
        searchBox.setHeight(15);
        searchBox.setValueChangedCallback(this::onSearchValueChanged);
        toolbar.addChild(searchBox);
    }

    private void buildFrontiersList(GridLayout mainLayout) {
        frontiers = new ScrollBox(actualHeight - 120, 450, 25);
        frontiers.setElementDeletedCallback(element -> onFrontierElementDeleted());
        frontiers.setElementClickedCallback(this::onFrontierElementClicked);
        mainLayout.addChild(frontiers, 1, 0, LayoutSettings.defaults().alignHorizontallyRight());
    }

    private void buildFiltersColumn(GridLayout mainLayout) {
        mainLayout.addChild(createResetFiltersButton(), 0, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        LinearLayout filtersColumn = LinearLayout.vertical().spacing(2);
        filtersColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(filtersColumn, 1, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        buildTypeFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildOwnerFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildDimensionFilter(filtersColumn);
    }

    private void buildTypeFilter(LinearLayout column) {
        column.addChild(createSectionLabel(filterTypeLabel));

        filterType = createFilterScrollBox(52);
        addEnumFilterOptions(filterType, ClientConfig.FILTER_FRONTIER_TYPE);
        filterType.setElementClickedCallback(this::onTypeFilterSelected);
        column.addChild(filterType);
    }

    private void buildOwnerFilter(LinearLayout column) {
        column.addChild(createSectionLabel(filterOwnerLabel));

        filterOwner = createFilterScrollBox(52);
        addEnumFilterOptions(filterOwner, ClientConfig.FILTER_FRONTIER_OWNER);
        filterOwner.setElementClickedCallback(this::onOwnerFilterSelected);
        column.addChild(filterOwner);
    }

    private void buildDimensionFilter(LinearLayout column) {
        column.addChild(createSectionLabel(filterDimensionLabel));

        filterDimension = createFilterScrollBox(actualHeight - 274);
        filterDimension.addElement(createRadioFilterOption(configAllLabel, ClientConfig.DIMENSION_FILTER_ALL));
        filterDimension.addElement(createRadioFilterOption(configCurrentLabel, ClientConfig.DIMENSION_FILTER_CURRENT));
        filterDimension.addElement(createRadioFilterOption(overworldLabel, "minecraft:overworld"));
        filterDimension.addElement(createRadioFilterOption(theNetherLabel, "minecraft:the_nether"));
        filterDimension.addElement(createRadioFilterOption(theEndLabel, "minecraft:the_end"));
        addDimensionsToFilter();
        syncFilterSelectionsFromConfig();
        filterDimension.setElementClickedCallback(this::onDimensionFilterSelected);

        if (filterDimension.getSelectedElement() == null) {
            ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.DIMENSION_FILTER_ALL);
            selectRadioByValue(filterDimension, ClientConfig.FILTER_FRONTIER_DIMENSION.get());
        }

        column.addChild(filterDimension);
    }

    private void buildBottomButtons() {
        buttonCreate = bottomButtons.addChild(createCreateButton());
        buttonInfo = bottomButtons.addChild(createInfoButton());
        buttonDelete = bottomButtons.addChild(createDeleteButton());
        buttonVisible = bottomButtons.addChild(createVisibleButton());
        buttonSettings = bottomButtons.addChild(createSettingsButton());
        bottomButtons.addChild(createDoneButton());
    }

    private SimpleButton createResetFiltersButton() {
        return new SimpleButton(font, 110, resetFiltersLabel, button -> onResetFiltersPressed());
    }

    private SimpleButton createCreateButton() {
        return new SimpleButton(font, 110, createLabel, button -> onCreatePressed());
    }

    private SimpleButton createInfoButton() {
        return new SimpleButton(font, 110, infoLabel, button -> onInfoPressed());
    }

    private SimpleButton createDeleteButton() {
        SimpleButton button = new SimpleButton(font, 110, deleteLabel, pressedButton -> onDeletePressed());
        button.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        return button;
    }

    private SimpleButton createVisibleButton() {
        return new SimpleButton(font, 110, hideLabel, button -> onVisiblePressed());
    }

    private SimpleButton createSettingsButton() {
        return new SimpleButton(font, 110, settingsLabel, button -> onSettingsPressed());
    }

    private SimpleButton createDoneButton() {
        return new SimpleButton(font, 110, doneLabel, button -> onDonePressed());
    }

    private StringWidget createSectionLabel(Component label) {
        return new StringWidget(label, font).setColor(ColorConstants.TEXT);
    }

    private <T> RadioListElement<T> createRadioFilterOption(Component label, T value) {
        return new RadioListElement<>(font, label, value);
    }

    private ScrollBox createFilterScrollBox(int height) {
        return new ScrollBox(height, 200, 15);
    }

    private <E extends Enum<E>> void addEnumFilterOptions(ScrollBox filter, EnumConfigEntry<E> entry) {
        for (E value : entry.values()) {
            filter.addElement(createRadioFilterOption(ClientConfig.getTranslatedEnum(value), value));
        }
        selectRadioByValue(filter, entry.get());
    }

    private void selectRadioByValue(ScrollBox scrollBox, Object value) {
        scrollBox.selectElementIf(element -> Objects.equals(((RadioListElement<?>) element).value(), value));
    }

    private static <T> T radioValue(ScrollElement element, Class<T> type) {
        return type.cast(((RadioListElement<?>) element).value());
    }

    private void onSearchValueChanged(String value) {
        updateFrontiers();
    }

    private void onFrontierElementClicked(ScrollElement element) {
        FrontierOverlay frontier = ((FrontierListElement) element).getFrontier();
        fullscreenMap.selectFrontier(frontier);
        refreshViewState();
    }

    private void onFrontierElementDeleted() {
        refreshViewState();
    }

    private void onResetFiltersPressed() {
        resetFiltersToDefaults();
        syncFilterSelectionsFromConfig();
        updateFrontiers();
        refreshViewState();
    }

    private void onTypeFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_TYPE.set(radioValue(element, ClientConfig.FilterFrontierType.class));
        notifyFiltersChanged();
    }

    private void onOwnerFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_OWNER.set(radioValue(element, ClientConfig.FilterFrontierOwner.class));
        notifyFiltersChanged();
    }

    private void onDimensionFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_DIMENSION.set(radioValue(element, String.class));
        notifyFiltersChanged();
    }

    private void onCreatePressed() {
        if (minecraft.player != null) {
            new NewFrontier(jmAPI, minecraft.player.blockPosition()).display();
        }
    }

    private void onInfoPressed() {
        FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        new FrontierInfo(jmAPI, frontier).display();
    }

    private void onDeletePressed() {
        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            showDeleteConfirmation();
        } else {
            deleteSelectedFrontier();
        }
    }

    private void showDeleteConfirmation() {
        new DeleteConfirmationDialog(
                "mapfrontiers.delete_frontier_dialog",
                response -> {
                    if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                        ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    deleteSelectedFrontier();
                }
        ).display();
    }

    private void onVisiblePressed() {
        FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        frontier.toggleVisibility(FrontierData.VisibilityData.Visibility.Frontier);
        MapFrontiersClient.getOperationService().updateFrontier(frontier);
        refreshViewState();
    }

    private void onSettingsPressed() {
        new ModSettings(true).display();
    }

    private void onDonePressed() {
        onClose();
    }

    private void resetFiltersToDefaults() {
        ClientConfig.FILTER_FRONTIER_TYPE.set(ClientConfig.FILTER_FRONTIER_TYPE.defaultValue());
        ClientConfig.FILTER_FRONTIER_OWNER.set(ClientConfig.FILTER_FRONTIER_OWNER.defaultValue());
        ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.FILTER_FRONTIER_DIMENSION.defaultValue());
    }

    private void syncFilterSelectionsFromConfig() {
        selectRadioByValue(filterType, ClientConfig.FILTER_FRONTIER_TYPE.get());
        selectRadioByValue(filterOwner, ClientConfig.FILTER_FRONTIER_OWNER.get());
        selectRadioByValue(filterDimension, ClientConfig.FILTER_FRONTIER_DIMENSION.get());
    }

    private void notifyFiltersChanged() {
        updateFrontiers();
        ClientGlobalEvents.postUpdatedConfigEvent();
        refreshViewState();
    }

    private void refreshInitialSelection() {
        if (fullscreenMap.getSelected() != null) {
            frontiers.selectElementIf(element -> ((FrontierListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
        }
    }

    private void refreshViewState() {
        updateButtons();
    }

    private void deleteSelectedFrontier() {
        FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        MapFrontiersClient.getOperationService().deleteFrontier(frontier);
        frontiers.removeElement(frontiers.getSelectedElement());
        updateButtons();
    }

    private void addDimensionsToFilter() {
        List<String> dimensions = Services.JOURNEYMAP.getDimensionList();
        for (String dimension : dimensions) {
            if (!dimension.equals("minecraft:overworld") && !dimension.equals("minecraft:the_nether") && !dimension.equals("minecraft:the_end")) {
                filterDimension.addElement(createRadioFilterOption(Component.literal(dimension), dimension));
            }
        }
    }

    private void updateFrontiers() {
        FrontierData selectedFrontier = frontiers.getSelectedElement() == null ? null : ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        UUID frontierID = selectedFrontier == null ? null : selectedFrontier.getId();

        List<FrontierOverlay> toAdd = new ArrayList<>();

        if (ClientConfig.FILTER_FRONTIER_TYPE.get() == ClientConfig.FilterFrontierType.All || ClientConfig.FILTER_FRONTIER_TYPE.get() == ClientConfig.FilterFrontierType.Personal) {
            for (FrontierOverlay frontier : MapFrontiersClient.getAllFrontiers(true)) {
                if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                    toAdd.add(frontier);
                }
            }
        }

        if (ClientConfig.FILTER_FRONTIER_TYPE.get() == ClientConfig.FilterFrontierType.All || ClientConfig.FILTER_FRONTIER_TYPE.get() == ClientConfig.FilterFrontierType.Global) {
            for (FrontierOverlay frontier : MapFrontiersClient.getAllFrontiers(false)) {
                if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                    toAdd.add(frontier);
                }
            }
        }

        if (!StringUtil.isBlank(searchBox.getValue())) {
            String searchText = searchBox.getValue().toLowerCase();
            toAdd.removeIf(frontier -> {
                String name = frontier.getName1().toLowerCase() + " " + frontier.getName2().toLowerCase();
                if (name.contains(searchText)) {
                    return false;
                }
                if (!StringUtil.isBlank(frontier.getOwner().username)) {
                    if (frontier.getOwner().username.toLowerCase().contains(searchText)) {
                        return false;
                    }
                }
                if (!StringUtil.isBlank(frontier.getOwner().uuid.toString())) {
                    if (frontier.getOwner().uuid.toString().toLowerCase().contains(searchText)) {
                        return false;
                    }
                }
                return true;
            });
        }

        toAdd.sort((a, b) -> {
            java.util.List<ClientConfig.Sorting> sorting = ClientConfig.getFrontierSortingValues();
            java.util.List<Boolean> direction = ClientConfig.getFrontierSortingDirectionValues();
            for (ClientConfig.Sorting sort : sorting) {
                int order = switch (sort) {
                    case ClientConfig.Sorting.Name -> {
                        int c = a.getName1().compareToIgnoreCase(b.getName1());
                        yield c == 0 ? a.getName2().compareToIgnoreCase(b.getName2()) : c;
                    }
                    case ClientConfig.Sorting.Owner -> a.getOwner().compareTo(b.getOwner());
                    case ClientConfig.Sorting.Shape -> Integer.compare(getShapeCount(a), getShapeCount(b));
                    case ClientConfig.Sorting.Area -> Float.compare(a.area, b.area);
                    case ClientConfig.Sorting.Modified -> {
                        if (a.getModified() == null && b.getModified() == null) {
                            yield 0;
                        } else if (a.getModified() == null) {
                            yield -1;
                        } else if (b.getModified() == null) {
                            yield 1;
                        } else {
                            yield a.getModified().compareTo(b.getModified());
                        }
                    }
                    case ClientConfig.Sorting.Created -> {
                        if (a.getCreated() == null && b.getCreated() == null) {
                            yield 0;
                        } else if (a.getCreated() == null) {
                            yield -1;
                        } else if (b.getCreated() == null) {
                            yield 1;
                        } else {
                            yield a.getCreated().compareTo(b.getCreated());
                        }
                    }
                };

                if (order != 0) {
                    boolean ascending = direction.get(sorting.indexOf(sort));
                    return ascending ? order : -order;
                }
            }

            return 0;
        });

        frontiers.removeAll();
        for (FrontierOverlay frontier : toAdd) {
            frontiers.addElement(new FrontierListElement(font, frontier));
        }

        if (frontierID != null) {
            frontiers.selectElementIf((element) -> ((FrontierListElement) element).getFrontier().getId().equals(frontierID));
        }
    }

    private boolean checkFilterOwner(FrontierOverlay frontier) {
        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == ClientConfig.FilterFrontierOwner.All) {
            return true;
        }

        boolean ownerIsPlayer = minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player));

        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == ClientConfig.FilterFrontierOwner.Self) {
            return ownerIsPlayer;
        } else {
            return !ownerIsPlayer;
        }
    }

    private boolean checkFilterDimension(FrontierOverlay frontier) {
        if (ClientConfig.FILTER_FRONTIER_DIMENSION.get().equals(ClientConfig.DIMENSION_FILTER_ALL)) {
            return true;
        }

        String dimension = ClientConfig.FILTER_FRONTIER_DIMENSION.get();
        if (dimension.equals(ClientConfig.DIMENSION_FILTER_CURRENT) && minecraft.level != null) {
            dimension = minecraft.level.dimension().identifier().toString();
        }

        return frontier.getDimension().identifier().toString().equals(dimension);
    }

    private void updateButtons() {
        if (minecraft.player == null) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        FrontierData frontier = frontiers.getSelectedElement() == null ? null : ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        buttonInfo.active = frontiers.getSelectedElement() != null;
        buttonDelete.active = actions.canDelete;
        buttonVisible.active = actions.canUpdate;

        if (frontier != null && frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier)) {
            buttonVisible.setMessage(Component.translatable("mapfrontiers.hide"));
        } else {
            buttonVisible.setMessage(Component.translatable("mapfrontiers.show"));
        }
    }

    private static int getShapeCount(FrontierOverlay frontier) {
        return switch (frontier.getMode()) {
            case Vertex -> frontier.getVertexCount();
            case Chunk -> frontier.getChunkCount();
            case Path -> frontier.getPointCount();
        };
    }
}
