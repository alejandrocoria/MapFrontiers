package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.AfterCreatingFrontier;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.FilterFrontierOwner;
import games.alejandrocoria.mapfrontiers.client.config.FilterFrontierShape;
import games.alejandrocoria.mapfrontiers.client.config.TerritoryListSorting;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SortToolbar;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.RadioListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.SpacerListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory.CollectionBorderCapListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory.CollectionListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory.FrontierListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory.SectionHeaderListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory.TerritoryListRowElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.CreateConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteCollectionConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteFrontierConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.NewFrontierDialog;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionUiStateStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.config.EnumConfigEntry;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVirtualIds;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class TerritoryListPage extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_mapfrontiers");
    private static final Component RESET_FILTERS_LABEL = Component.translatable("mapfrontiers.reset_filters");
    private static final Component FILTER_SHAPE_LABEL = Component.translatable("mapfrontiers.filter_shape");
    private static final Component FILTER_OWNER_LABEL = Component.translatable("mapfrontiers.filter_owner");
    private static final Component FILTER_DIMENSION_LABEL = Component.translatable("mapfrontiers.filter_dimension");
    private static final Component CONFIG_ALL_LABEL = Component.translatable("mapfrontiers.config.All");
    private static final Component CONFIG_CURRENT_LABEL = Component.translatable("mapfrontiers.config.Current");
    private static final Component OVERWORLD_LABEL = Component.literal("minecraft:overworld");
    private static final Component THE_NETHER_LABEL = Component.literal("minecraft:the_nether");
    private static final Component THE_END_LABEL = Component.literal("minecraft:the_end");
    private static final Component SETTINGS_LABEL = Component.translatable("mapfrontiers.settings");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final int TERRITORIES_WIDTH = 542;
    private static final int TERRITORY_ROW_HEIGHT = 25;
    private static final int FILTER_WIDTH = 200;
    private static final int FILTER_ELEMENT_HEIGHT = 15;
    private static final int TERRITORIES_MIN_VIEWPORT_HEIGHT = 175;
    private static final int FILTER_SHAPE_MIN_ROWS = 4;
    private static final int FILTER_OWNER_MIN_ROWS = 3;
    private static final int FILTER_DIMENSION_MIN_ROWS = 2;
    private static final float MIN_COLLECTION_BRIGHTNESS = 0.3f;

    private final IClientAPI jmAPI;
    private final FullscreenMap fullscreenMap;
    private final Set<UUID> markedFrontierIds = new HashSet<>();
    private MarkedType markedType = MarkedType.NONE;

    private TextBox searchBox;
    private ScrollBox territories;
    private ScrollBox filterShape;
    private ScrollBox filterOwner;
    private ScrollBox filterDimension;

    private enum MarkedType {
        NONE,
        PERSONAL_PERSISTENT,
        PERSONAL_SESSION,
        GLOBAL
    }

    public TerritoryListPage(IClientAPI jmAPI, FullscreenMap fullscreenMap) {
        super(TITLE_LABEL);
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getFrontierEvents().subscribeCreated(this, (frontierOverlay, playerID) -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getCollectionEvents().subscribeCreated(this, collection -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getCollectionEvents().subscribeUpdated(this, collection -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getCollectionEvents().subscribeDeleted(this, collectionId -> {
            rebuildTerritories();
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            rebuildTerritories();
        });
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = createMainLayout();

        buildToolbar(mainLayout);
        buildTerritoriesList(mainLayout);
        buildFiltersColumn(mainLayout);

        buildBottomButtons();

        rebuildTerritories();
    }

    @Override
    protected void resetContentToMinimumSize() {
        territories.setViewportHeight(TERRITORIES_MIN_VIEWPORT_HEIGHT);
        filterDimension.setViewportHeight(ScrollBox.rowsToHeight(FILTER_DIMENSION_MIN_ROWS, FILTER_ELEMENT_HEIGHT));
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        territories.setViewportSize(TERRITORIES_WIDTH, Math.max(TERRITORIES_MIN_VIEWPORT_HEIGHT,
                getAvailableScrollHeightInsideBackground(territories)));
        filterDimension.setViewportSize(FILTER_WIDTH, Math.max(ScrollBox.rowsToHeight(FILTER_DIMENSION_MIN_ROWS, FILTER_ELEMENT_HEIGHT),
                getAvailableScrollHeightInsideBackground(filterDimension)));
    }

    private int getAvailableScrollHeightInsideBackground(ScrollBox scrollBox) {
        int scrollBoxY = scrollBox.getY() - content.getY();
        return Math.max(0, actualHeight - scrollBoxY - getMinimumLayoutExtraHeight());
    }

    @Override
    protected void positionContent() {
        content.setPosition((actualWidth - content.getWidth()) / 2, LayoutConstants.PAGE_MARGIN + LayoutConstants.BOX_PADDING);
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, actualWidth - LayoutConstants.PAGE_MARGIN * 2, actualHeight - LayoutConstants.PAGE_MARGIN * 2);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox scrollBox) {
                scrollBox.mouseReleased();
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        super.onClose();
    }

    private GridLayout createMainLayout() {
        GridLayout mainLayout = new GridLayout().columnSpacing(LayoutConstants.SPACING_MEDIUM).rowSpacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);
        return mainLayout;
    }

    private void buildToolbar(GridLayout mainLayout) {
        LinearLayout toolbar = LinearLayout.horizontal();
        toolbar.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(toolbar, 0, 0, LayoutSettings.defaults().alignHorizontallyLeft());

        toolbar.addChild(new SortToolbar(font, this::rebuildTerritories));
        toolbar.addChild(SpacerElement.width(16));

        searchBox = new TextBox(font, 100, I18n.get("mapfrontiers.search"));
        searchBox.setMaxLength(40);
        searchBox.setHeight(15);
        searchBox.setValueChangedCallback(this::onSearchValueChanged);
        toolbar.addChild(searchBox);
    }

    private void buildTerritoriesList(GridLayout mainLayout) {
        territories = new ScrollBox(TERRITORIES_MIN_VIEWPORT_HEIGHT, TERRITORIES_WIDTH, TERRITORY_ROW_HEIGHT);
        territories.setElementClickedCallback(this::onTerritoryRowClicked);
        mainLayout.addChild(territories, 1, 0, LayoutSettings.defaults().alignHorizontallyRight());
    }

    private void buildFiltersColumn(GridLayout mainLayout) {
        mainLayout.addChild(createResetFiltersButton(), 0, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        LinearLayout filtersColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        filtersColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(filtersColumn, 1, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        buildShapeFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildOwnerFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildDimensionFilter(filtersColumn);
    }

    private void buildShapeFilter(LinearLayout column) {
        column.addChild(createSectionLabel(FILTER_SHAPE_LABEL));

        filterShape = createFilterScrollBox(FILTER_SHAPE_MIN_ROWS);
        addEnumFilterOptions(filterShape, ClientConfig.FILTER_FRONTIER_SHAPE);
        filterShape.setElementClickedCallback(this::onShapeFilterSelected);
        column.addChild(filterShape);
    }

    private void buildOwnerFilter(LinearLayout column) {
        column.addChild(createSectionLabel(FILTER_OWNER_LABEL));

        filterOwner = createFilterScrollBox(FILTER_OWNER_MIN_ROWS);
        addEnumFilterOptions(filterOwner, ClientConfig.FILTER_FRONTIER_OWNER);
        filterOwner.setElementClickedCallback(this::onOwnerFilterSelected);
        column.addChild(filterOwner);
    }

    private void buildDimensionFilter(LinearLayout column) {
        column.addChild(createSectionLabel(FILTER_DIMENSION_LABEL));

        filterDimension = createFilterScrollBox(FILTER_DIMENSION_MIN_ROWS);
        filterDimension.addElement(createRadioFilterOption(CONFIG_ALL_LABEL, ClientConfig.DIMENSION_FILTER_ALL));
        filterDimension.addElement(createRadioFilterOption(CONFIG_CURRENT_LABEL, ClientConfig.DIMENSION_FILTER_CURRENT));
        filterDimension.addElement(createRadioFilterOption(OVERWORLD_LABEL, "minecraft:overworld"));
        filterDimension.addElement(createRadioFilterOption(THE_NETHER_LABEL, "minecraft:the_nether"));
        filterDimension.addElement(createRadioFilterOption(THE_END_LABEL, "minecraft:the_end"));
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
        addBottomButton(createSettingsButton());
        addBottomButton(createDoneButton());
    }

    private SimpleButton createResetFiltersButton() {
        return new SimpleButton(font, 110, RESET_FILTERS_LABEL, button -> onResetFiltersPressed());
    }

    private SimpleButton createSettingsButton() {
        return new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, SETTINGS_LABEL, button -> onSettingsPressed());
    }

    private SimpleButton createDoneButton() {
        return new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, DONE_LABEL, button -> onDonePressed());
    }

    private StringWidget createSectionLabel(Component label) {
        return new StringWidget(label, font).setColor(ColorConstants.TEXT);
    }

    private <T> RadioListElement<T> createRadioFilterOption(Component label, T value) {
        return new RadioListElement<>(font, label, value);
    }

    private ScrollBox createFilterScrollBox(int rows) {
        return new ScrollBox(ScrollBox.rowsToHeight(rows, FILTER_ELEMENT_HEIGHT), FILTER_WIDTH, FILTER_ELEMENT_HEIGHT);
    }

    private <E extends Enum<E>> void addEnumFilterOptions(ScrollBox filter, EnumConfigEntry<E> entry) {
        for (E value : entry.values()) {
            filter.addElement(createRadioFilterOption(ClientConfig.getTranslatedEnum(value), value));
        }
        selectRadioByValue(filter, entry.get());
    }

    private void selectRadioByValue(ScrollBox scrollBox, Object value) {
        scrollBox.setSelectedElementIf(element -> Objects.equals(((RadioListElement<?>) element).value(), value));
    }

    private static <T> T radioValue(ScrollElement element, Class<T> type) {
        return type.cast(((RadioListElement<?>) element).value());
    }

    private void onSearchValueChanged(String value) {
        clearMarkedFrontiers();
        rebuildTerritories();
    }

    private void onTerritoryRowClicked(ScrollElement element) {
        if (element instanceof FrontierListElement frontierElement && frontierElement.consumeMarkToggleRequested()) {
            toggleFrontierMarked(frontierElement.getFrontier());
            return;
        }

        if (element instanceof FrontierListElement frontierElement && frontierElement.consumeVisibilityRequested()) {
            onFrontierVisibilityPressed(frontierElement.getFrontier());
            return;
        }

        if (element instanceof FrontierListElement frontierElement && frontierElement.consumeDeleteRequested()) {
            onFrontierDeletePressed(frontierElement.getFrontier());
            return;
        }

        if (element instanceof SectionHeaderListElement headerElement && headerElement.consumeAddRequested()) {
            createCollectionFromHeader(headerElement.getRowId());
            return;
        }

        if (!(element instanceof TerritoryListRowElement rowElement)) {
            return;
        }

        if (element instanceof CollectionListElement collectionElement) {
            if (collectionElement.consumeCollapseToggleRequested()) {
                CollectionUiStateStore collapseState = getCollectionUiStateStore();
                collapseState.setCollapsed(rowElement.getRowId(), !collapseState.isCollapsed(rowElement.getRowId()));
                rebuildTerritories();
                return;
            }

            if (collectionElement.consumeMarkToggleRequested()) {
                toggleCollectionGroupMarked(collectionElement);
                return;
            }

            if (collectionElement.consumeVisibilityRequested()) {
                CollectionData collection = collectionElement.getCollection();
                if (collection != null) {
                    onCollectionVisibilityPressed(collection);
                }
                return;
            }

            if (collectionElement.consumeDeleteRequested()) {
                CollectionData collection = collectionElement.getCollection();
                if (collection != null) {
                    onCollectionDeletePressed(collection);
                }
                return;
            }

            if (collectionElement.consumeMoveHereRequested()) {
                if (isMarkedModeActive()) {
                    moveMarkedFrontiersToCollection(collectionElement);
                    rebuildTerritories();
                }
                return;
            }

            if (collectionElement.consumeCreateRequested()) {
                createFrontierFromCollectionRow(collectionElement);
                return;
            }
        }

        if (isMarkedModeActive()) {
            clearMarkedFrontiers();
            rebuildTerritories();
        }

        if (element instanceof FrontierListElement frontierElement) {
            fullscreenMap.selectFrontier(frontierElement.getFrontier());
            syncSelectedRowWithMapSelection();
            new FrontierInfoPage(jmAPI, frontierElement.getFrontier()).display();
            return;
        }

        if (element instanceof CollectionListElement collectionElement) {
            CollectionData collection = collectionElement.getCollection();
            if (collection != null) {
                fullscreenMap.selectCollection(collection);
                syncSelectedRowWithMapSelection();
                new CollectionInfoPage(collection).display();
            }
        }
    }

    private void onResetFiltersPressed() {
        resetFiltersToDefaults();
        syncFilterSelectionsFromConfig();
        notifyFiltersChanged();
    }

    private void onShapeFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_SHAPE.set(radioValue(element, FilterFrontierShape.class));
        notifyFiltersChanged();
    }

    private void onOwnerFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_OWNER.set(radioValue(element, FilterFrontierOwner.class));
        notifyFiltersChanged();
    }

    private void onDimensionFilterSelected(ScrollElement element) {
        ClientConfig.FILTER_FRONTIER_DIMENSION.set(radioValue(element, String.class));
        notifyFiltersChanged();
    }

    private void openNewFrontierDialog(CollectionListElement selectedCollectionElement) {
        if (minecraft.player == null) {
            return;
        }

        UUID collectionId = selectedCollectionElement.getCollection() == null ? null : selectedCollectionElement.getCollection().getId();
        TerritoryLifetime lifetime = selectedCollectionElement.getScope() == CollectionScope.PERSONAL_SESSION
                ? TerritoryLifetime.SESSION_ONLY
                : TerritoryLifetime.PERSISTENT;
        new NewFrontierDialog(jmAPI, minecraft.player.blockPosition(), selectedCollectionElement.isPersonal(), lifetime, collectionId,
                createNewFrontierResultHandler()).display();
    }

    private NewFrontierDialog.ResultHandler createNewFrontierResultHandler() {
        return new NewFrontierDialog.ResultHandler() {
            @Override
            public void beforeCreate(NewFrontierDialog dialog, AfterCreatingFrontier action) {
                if (action == AfterCreatingFrontier.EditShape) {
                    dialog.closeToFullscreenMap();
                } else {
                    dialog.onClose();
                }
            }

            @Override
            public void onFrontierCreated(FrontierOverlay frontier, AfterCreatingFrontier action) {
                if (action == AfterCreatingFrontier.EditShape) {
                    fullscreenMap.beginEditingFrontier(frontier);
                    return;
                }

                selectCreatedFrontier(frontier);
                if (action == AfterCreatingFrontier.InfoScreen) {
                    new FrontierInfoPage(jmAPI, frontier).display();
                }
            }
        };
    }

    private void selectCreatedFrontier(FrontierOverlay frontier) {
        fullscreenMap.selectFrontier(frontier);
        rebuildTerritories();
    }

    private void onFrontierVisibilityPressed(FrontierOverlay frontier) {
        frontier.toggleVisibility(FrontierVisibility.Frontier);
        FrontierChange change = new FrontierChange();
        change.setVisibility(frontier.getVisibilityData());
        MapFrontiersClient.getOperationService().updateFrontier(frontier, change);
    }

    private void onCollectionVisibilityPressed(CollectionData collection) {
        collection.getVisibilityData().setVisible(!collection.getVisibilityData().isVisible());
        MapFrontiersClient.getOperationService().updateCollection(collection);
    }

    private void onFrontierDeletePressed(FrontierOverlay frontier) {
        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            showDeleteFrontierConfirmation(frontier);
        } else {
            deleteFrontier(frontier);
        }
    }

    private void onCollectionDeletePressed(CollectionData collection) {
        if (ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.get()) {
            showDeleteCollectionConfirmation(collection);
        } else {
            deleteCollection(collection);
        }
    }

    private void showDeleteFrontierConfirmation(FrontierOverlay frontier) {
        new DeleteFrontierConfirmationDialog(frontier, response -> {
            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                ClientGlobalEvents.postUpdatedConfigEvent();
            }
            deleteFrontier(frontier);
        }).display();
    }

    private void showDeleteCollectionConfirmation(CollectionData collection) {
        new DeleteCollectionConfirmationDialog(collection, response -> {
            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.set(false);
                ClientGlobalEvents.postUpdatedConfigEvent();
            }
            deleteCollection(collection);
        }).display();
    }

    private void onSettingsPressed() {
        clearMarkedFrontiers();
        rebuildTerritories();
        new ModSettingsPage(true).display();
    }

    private void onDonePressed() {
        onClose();
    }

    private void resetFiltersToDefaults() {
        ClientConfig.FILTER_FRONTIER_SHAPE.set(ClientConfig.FILTER_FRONTIER_SHAPE.defaultValue());
        ClientConfig.FILTER_FRONTIER_OWNER.set(ClientConfig.FILTER_FRONTIER_OWNER.defaultValue());
        ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.FILTER_FRONTIER_DIMENSION.defaultValue());
    }

    private void syncFilterSelectionsFromConfig() {
        selectRadioByValue(filterShape, ClientConfig.FILTER_FRONTIER_SHAPE.get());
        selectRadioByValue(filterOwner, ClientConfig.FILTER_FRONTIER_OWNER.get());
        selectRadioByValue(filterDimension, ClientConfig.FILTER_FRONTIER_DIMENSION.get());
    }

    private void notifyFiltersChanged() {
        clearMarkedFrontiers();
        rebuildTerritories();
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void deleteFrontier(FrontierOverlay frontier) {
        MapFrontiersClient.getOperationService().deleteFrontier(frontier);
        if (frontier == fullscreenMap.getSelected()) {
            fullscreenMap.selectFrontier(null);
        }
        rebuildTerritories();
    }

    private void deleteCollection(CollectionData collection) {
        MapFrontiersClient.getOperationService().deleteCollection(collection);
        CollectionData selectedCollection = fullscreenMap.getSelectedCollection();
        if (selectedCollection != null && selectedCollection.getId().equals(collection.getId())) {
            fullscreenMap.selectCollection(null);
        }
        rebuildTerritories();
    }

    private void createCollectionFromHeader(String headerRowId) {
        CollectionScope scope = getScopeFromHeaderRowId(headerRowId);
        if (scope == null || minecraft.player == null || !canCreateCollection(scope)) {
            return;
        }

        if (scope == CollectionScope.PERSONAL_SESSION) {
            showTemporaryCollectionCreateConfirmation(() -> createCollectionConfirmed(scope));
            return;
        }
        createCollectionConfirmed(scope);
    }

    private void createCollectionConfirmed(CollectionScope scope) {
        if (minecraft.player == null) {
            return;
        }

        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setPersonal(scope != CollectionScope.GLOBAL_PERSISTENT);
        collection.setLifetime(scope == CollectionScope.PERSONAL_SESSION
                ? TerritoryLifetime.SESSION_ONLY
                : TerritoryLifetime.PERSISTENT);
        collection.setOwner(new SettingsUser(minecraft.player));

        MapFrontiersClient.getOperationService().createCollection(collection);
        new CollectionInfoPage(collection).display();
    }

    private void createFrontierFromCollectionRow(CollectionListElement collectionElement) {
        if (minecraft.player == null) {
            return;
        }

        if (collectionElement.getScope() == CollectionScope.PERSONAL_SESSION) {
            showTemporaryFrontierCreateConfirmation(() -> openNewFrontierDialog(collectionElement));
            return;
        }

        openNewFrontierDialog(collectionElement);
    }

    private void showTemporaryFrontierCreateConfirmation(Runnable onConfirm) {
        if (!ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE.get()) {
            onConfirm.run();
            return;
        }

        new CreateConfirmationDialog(
                "mapfrontiers.create_temporary_frontier_dialog",
                "mapfrontiers.create_temporary_frontier_dialog_desc",
                response -> {
                    if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                        ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE.set(false);
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    onConfirm.run();
                }
        ).display();
    }

    private void showTemporaryCollectionCreateConfirmation(Runnable onConfirm) {
        if (!ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE.get()) {
            onConfirm.run();
            return;
        }

        new CreateConfirmationDialog(
                "mapfrontiers.create_temporary_collection_dialog",
                "mapfrontiers.create_temporary_collection_dialog_desc",
                response -> {
                    if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                        ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE.set(false);
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    onConfirm.run();
                }
        ).display();
    }

    private void addDimensionsToFilter() {
        List<String> dimensions = Services.JOURNEYMAP.getDimensionList();
        for (String dimension : dimensions) {
            if (!dimension.equals("minecraft:overworld") && !dimension.equals("minecraft:the_nether") && !dimension.equals("minecraft:the_end")) {
                filterDimension.addElement(createRadioFilterOption(Component.literal(dimension), dimension));
            }
        }
    }

    private void rebuildTerritories() {
        int previousScrollOffset = territories.getScrollOffset();
        ScrollBox.FocusSnapshot focusSnapshot = territories.captureFocusSnapshot();
        CollectionUiStateStore collapseState = getCollectionUiStateStore();
        List<ScrollElement> rows = new ArrayList<>();
        Set<UUID> visibleFilteredFrontiers = new HashSet<>();

        List<List<ScrollElement>> blocks = List.of(
                buildBlockRows(CollectionScope.PERSONAL_PERSISTENT, visibleFilteredFrontiers),
                buildBlockRows(CollectionScope.PERSONAL_SESSION, visibleFilteredFrontiers),
                buildBlockRows(CollectionScope.GLOBAL_PERSISTENT, visibleFilteredFrontiers));
        boolean firstBlock = true;
        for (List<ScrollElement> block : blocks) {
            if (block.isEmpty()) {
                continue;
            }
            if (!firstBlock) {
                rows.add(new SpacerListElement(TERRITORIES_WIDTH, 9));
            }
            rows.addAll(block);
            firstBlock = false;
        }
        pruneMarkedFrontiers(visibleFilteredFrontiers);

        Set<String> validRowIds = new HashSet<>();
        for (ScrollElement row : rows) {
            if (row instanceof TerritoryListRowElement territoryRow) {
                validRowIds.add(territoryRow.getRowId());
            }
        }
        collapseState.prune(validRowIds);

        territories.removeAll();
        for (ScrollElement row : rows) {
            territories.addElement(row);
        }
        territories.setScrollOffset(previousScrollOffset);
        syncSelectedRowWithMapSelection();
        ComponentPath path = territories.restoreFocusSnapshot(focusSnapshot);
        if (path != null) {
            setFocused(territories);
            path.applyFocus(true);
        }
    }

    private void syncSelectedRowWithMapSelection() {
        CollectionData selectedCollection = fullscreenMap.getSelectedCollection();
        if (selectedCollection != null) {
            UUID selectedCollectionId = selectedCollection.getId();
            territories.setSelectedElementIf(element -> element instanceof CollectionListElement collectionElement
                    && collectionElement.getCollection() != null
                    && collectionElement.getCollection().getId().equals(selectedCollectionId));
            return;
        }

        FrontierOverlay selectedFrontier = fullscreenMap.getSelected();
        if (selectedFrontier == null) {
            territories.clearSelection();
            return;
        }

        UUID selectedFrontierId = selectedFrontier.getId();
        territories.setSelectedElementIf(element -> element instanceof FrontierListElement frontierElement
                && frontierElement.getFrontier().getId().equals(selectedFrontierId));
    }

    private List<ScrollElement> buildBlockRows(CollectionScope scope, Set<UUID> visibleFilteredFrontiers) {
        List<ScrollElement> rows = new ArrayList<>();
        List<TerritoryGroupModel> collectionGroups = buildCollectionGroups(scope);
        boolean includeVirtualRow = scope != CollectionScope.GLOBAL_PERSISTENT || shouldShowGlobalVirtualRow() || !collectionGroups.isEmpty();

        if (!includeVirtualRow) {
            return rows;
        }

        TerritoryGroupModel virtualGroup = buildVirtualGroup(scope);
        String headerText = getHeaderText(scope);
        rows.add(new SectionHeaderListElement(getHeaderRowId(scope), font, headerText, TERRITORIES_WIDTH, ColorConstants.SECTION_HEADER_TEXT,
                !isMarkedModeActive() && canCreateCollection(scope), getCreateCollectionTooltip(scope)));
        rows.add(createCollectionRowElement(virtualGroup, ColorConstants.VIRTUAL_COLLECTION_COLOR));
        if (!virtualGroup.collapsed) {
            addFrontierChildren(rows, virtualGroup.filteredFrontiers, visibleFilteredFrontiers, ColorConstants.VIRTUAL_COLLECTION_COLOR);
        }
        rows.add(new CollectionBorderCapListElement(TERRITORIES_WIDTH, ColorConstants.VIRTUAL_COLLECTION_COLOR));

        collectionGroups.sort(this::compareCollectionGroups);
        for (TerritoryGroupModel group : collectionGroups) {
            rows.add(new SpacerListElement(TERRITORIES_WIDTH, 2));
            int collectionColor = ColorHelper.ensureMinBrightness(group.collection.getColor(), MIN_COLLECTION_BRIGHTNESS);
            rows.add(createCollectionRowElement(group, collectionColor));
            if (!group.collapsed) {
                addFrontierChildren(rows, group.filteredFrontiers, visibleFilteredFrontiers, collectionColor);
            }
            rows.add(new CollectionBorderCapListElement(TERRITORIES_WIDTH, collectionColor));
        }

        rows.stream()
                .filter(row -> row instanceof CollectionListElement)
                .map(row -> (CollectionListElement) row)
                .forEach(row -> visibleFilteredFrontiers.addAll(row.getEligibleFrontierIds()));

        return rows;
    }

    private CollectionListElement createCollectionRowElement(TerritoryGroupModel group, int collectionColor) {
        List<UUID> eligibleFrontierIds = group.filteredFrontiers.stream()
                .filter(this::canMarkFrontier)
                .map(FrontierOverlay::getId)
                .toList();
        boolean canMarkGroup = canMarkCollectionGroup(group, eligibleFrontierIds);
        CollectionListElement.ActionState actionState = getCollectionActionState(group);
        boolean canChangeVisibility = !isMarkedModeActive() && group.collection != null && canUpdateSelectedCollection(group.collection);
        boolean canDelete = !isMarkedModeActive() && group.collection != null && canDeleteSelectedCollection(group.collection);

        return new CollectionListElement(group.rowId, font, group.collection, group.virtualRow, group.scope, group.title,
                formatCollectionCounters(group.totalFrontiers, group.filteredFrontiers.size()),
                collectionColor,
                group.collapsed,
                shouldShowCheckboxInMarkedMode(canMarkGroup, isCompatibleWithMarkedType(group, eligibleFrontierIds)),
                shouldShowCheckboxOnHover(canMarkGroup),
                countMarkedFrontiers(eligibleFrontierIds),
                eligibleFrontierIds.size(),
                actionState,
                canChangeVisibility,
                canDelete,
                eligibleFrontierIds,
                TERRITORIES_WIDTH);
    }

    private void addFrontierChildren(List<ScrollElement> rows, List<FrontierOverlay> filteredFrontiers,
                                     Set<UUID> visibleFilteredFrontiers, int collectionColor) {
        filteredFrontiers.sort(this::compareFrontiers);
        for (FrontierOverlay frontier : filteredFrontiers) {
            if (canMarkFrontier(frontier)) {
                visibleFilteredFrontiers.add(frontier.getId());
            }
            SettingsProfile.AvailableActions actions = getAvailableActions(frontier);
            rows.add(new FrontierListElement(font, frontier, TERRITORIES_WIDTH, collectionColor,
                    shouldShowCheckboxInMarkedMode(canMarkFrontier(frontier), isCompatibleWithMarkedType(frontier)),
                    shouldShowCheckboxOnHover(canMarkFrontier(frontier)),
                    isFrontierMarked(frontier),
                    !isMarkedModeActive() && actions.canUpdate,
                    !isMarkedModeActive() && actions.canDelete));
        }
    }

    private TerritoryGroupModel buildVirtualGroup(CollectionScope scope) {
        String rowId = getVirtualCollectionRowId(scope);
        CollectionUiStateStore collapseState = getCollectionUiStateStore();
        List<FrontierOverlay> allFrontiers = new ArrayList<>(MapFrontiersClient.getFrontiersWithoutCollection(scope));
        List<FrontierOverlay> filteredFrontiers = filterFrontiers(allFrontiers);

        return new TerritoryGroupModel(rowId,
                null,
                true,
                scope,
                I18n.get("mapfrontiers.no_collection"),
                collapseState.isCollapsed(rowId),
                allFrontiers,
                filteredFrontiers);
    }

    private List<TerritoryGroupModel> buildCollectionGroups(CollectionScope scope) {
        CollectionUiStateStore collapseState = getCollectionUiStateStore();
        List<TerritoryGroupModel> groups = new ArrayList<>();
        for (CollectionData collection : MapFrontiersClient.getCollections(scope)) {
            List<FrontierOverlay> allFrontiers = new ArrayList<>(MapFrontiersClient.getFrontiersInCollection(collection.getId()));
            List<FrontierOverlay> filteredFrontiers = filterFrontiers(allFrontiers);
            String title = collection.getName();
            if (StringUtil.isBlank(title)) {
                title = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            }

            String rowId = collection.getId().toString();
            groups.add(new TerritoryGroupModel(rowId,
                    collection,
                    false,
                    scope,
                    title,
                    collapseState.isCollapsed(rowId),
                    allFrontiers,
                    filteredFrontiers));
        }
        return groups;
    }

    private List<FrontierOverlay> filterFrontiers(List<FrontierOverlay> frontiers) {
        List<FrontierOverlay> filtered = new ArrayList<>();
        for (FrontierOverlay frontier : frontiers) {
            if (matchesFilters(frontier)) {
                filtered.add(frontier);
            }
        }
        return filtered;
    }

    private boolean matchesFilters(FrontierOverlay frontier) {
        return checkFilterShape(frontier) && checkFilterOwner(frontier) && checkFilterDimension(frontier) && checkSearch(frontier);
    }

    private boolean checkFilterShape(FrontierOverlay frontier) {
        return switch (ClientConfig.FILTER_FRONTIER_SHAPE.get()) {
            case All -> true;
            case Vertex -> frontier.getShape() == FrontierShape.Vertex;
            case Chunk -> frontier.getShape() == FrontierShape.Chunk;
            case Path -> frontier.getShape() == FrontierShape.Path;
        };
    }

    private boolean checkFilterOwner(FrontierOverlay frontier) {
        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == FilterFrontierOwner.All) {
            return true;
        }

        boolean ownerIsPlayer = minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player));

        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == FilterFrontierOwner.Self) {
            return ownerIsPlayer;
        }

        return !ownerIsPlayer;
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

    private boolean checkSearch(FrontierOverlay frontier) {
        if (StringUtil.isBlank(searchBox.getValue())) {
            return true;
        }

        String searchText = searchBox.getValue().toLowerCase();
        String name = frontier.getName1().toLowerCase() + " " + frontier.getName2().toLowerCase();
        if (name.contains(searchText)) {
            return true;
        }
        if (!StringUtil.isBlank(frontier.getOwner().username) && frontier.getOwner().username.toLowerCase().contains(searchText)) {
            return true;
        }
        return !StringUtil.isBlank(frontier.getOwner().uuid.toString())
                && frontier.getOwner().uuid.toString().toLowerCase().contains(searchText);
    }

    private int compareCollectionGroups(TerritoryGroupModel a, TerritoryGroupModel b) {
        return compareByConfiguredSorting(a, b, this::compareCollectionGroupsBySort);
    }

    private int compareFrontiers(FrontierOverlay a, FrontierOverlay b) {
        return compareByConfiguredSorting(a, b, this::compareFrontiersBySort);
    }

    private <T> int compareByConfiguredSorting(T a, T b, SortComparator<T> comparator) {
        List<TerritoryListSorting> sorting = ClientConfig.getTerritoryListSortingValues();
        List<Boolean> directions = ClientConfig.getTerritoryListSortingDirectionValues();
        for (int i = 0; i < sorting.size(); ++i) {
            int order = comparator.compare(sorting.get(i), a, b);
            if (order != 0) {
                return directions.get(i) ? order : -order;
            }
        }
        return 0;
    }

    private int compareFrontiersBySort(TerritoryListSorting sort, FrontierOverlay a, FrontierOverlay b) {
        return switch (sort) {
            case Name -> {
                int c = a.getName1().compareToIgnoreCase(b.getName1());
                yield c == 0 ? a.getName2().compareToIgnoreCase(b.getName2()) : c;
            }
            case Owner -> a.getOwner().compareTo(b.getOwner());
            case Shape -> Integer.compare(getShapeCount(a), getShapeCount(b));
            case Area -> Float.compare(a.area, b.area);
            case Modified -> compareNullableDates(a.getModified(), b.getModified());
            case Created -> compareNullableDates(a.getCreated(), b.getCreated());
        };
    }

    private int compareCollectionGroupsBySort(TerritoryListSorting sort, TerritoryGroupModel a, TerritoryGroupModel b) {
        return switch (sort) {
            case Name -> a.title.compareToIgnoreCase(b.title);
            case Owner -> a.owner.compareTo(b.owner);
            case Shape -> 0;
            case Area -> Float.compare(a.totalArea, b.totalArea);
            case Modified -> compareNullableDates(a.modified, b.modified);
            case Created -> compareNullableDates(a.created, b.created);
        };
    }

    private static int compareNullableDates(@Nullable Date a, @Nullable Date b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private String formatCollectionCounters(int totalFrontiers, int filteredFrontiers) {
        String text = I18n.get("mapfrontiers.collection_frontiers_count", totalFrontiers);
        int outOfFilter = totalFrontiers - filteredFrontiers;
        if (outOfFilter > 0) {
            text += " (" + I18n.get("mapfrontiers.filtered_out_count", outOfFilter) + ")";
        }
        return text;
    }

    private boolean shouldShowGlobalVirtualRow() {
        return !MapFrontiersClient.getAllFrontiers(false).isEmpty() || canManageGlobalCollections();
    }

    private String getHeaderText(CollectionScope scope) {
        return switch (scope) {
            case PERSONAL_PERSISTENT -> I18n.get("mapfrontiers.personal_frontiers_header");
            case PERSONAL_SESSION -> I18n.get("mapfrontiers.temporary_frontiers_header");
            case GLOBAL_PERSISTENT -> I18n.get("mapfrontiers.global_frontiers_header");
        };
    }

    private String getHeaderRowId(CollectionScope scope) {
        return switch (scope) {
            case PERSONAL_PERSISTENT -> "mapfrontiers:personal_header_row";
            case PERSONAL_SESSION -> "mapfrontiers:temporary_header_row";
            case GLOBAL_PERSISTENT -> "mapfrontiers:global_header_row";
        };
    }

    private @Nullable CollectionScope getScopeFromHeaderRowId(String rowId) {
        return switch (rowId) {
            case "mapfrontiers:personal_header_row" -> CollectionScope.PERSONAL_PERSISTENT;
            case "mapfrontiers:temporary_header_row" -> CollectionScope.PERSONAL_SESSION;
            case "mapfrontiers:global_header_row" -> CollectionScope.GLOBAL_PERSISTENT;
            default -> null;
        };
    }

    private String getVirtualCollectionRowId(CollectionScope scope) {
        return switch (scope) {
            case PERSONAL_PERSISTENT -> CollectionVirtualIds.PERSONAL_VIRTUAL_COLLECTION_ID;
            case PERSONAL_SESSION -> CollectionVirtualIds.TEMPORARY_VIRTUAL_COLLECTION_ID;
            case GLOBAL_PERSISTENT -> CollectionVirtualIds.GLOBAL_VIRTUAL_COLLECTION_ID;
        };
    }

    private boolean isMarkedModeActive() {
        return markedType != MarkedType.NONE && !markedFrontierIds.isEmpty();
    }

    private boolean isFrontierMarked(FrontierOverlay frontier) {
        return markedFrontierIds.contains(frontier.getId());
    }

    private boolean shouldShowCheckboxInMarkedMode(boolean eligible, boolean compatible) {
        return eligible && isMarkedModeActive() && compatible;
    }

    private boolean shouldShowCheckboxOnHover(boolean eligible) {
        return eligible && !isMarkedModeActive();
    }

    private int countMarkedFrontiers(List<UUID> eligibleFrontierIds) {
        int count = 0;
        for (UUID frontierId : eligibleFrontierIds) {
            if (markedFrontierIds.contains(frontierId)) {
                ++count;
            }
        }
        return count;
    }

    private CollectionListElement.ActionState getCollectionActionState(TerritoryGroupModel group) {
        if (!isMarkedModeActive()) {
            return canCreateFrontierInScope(group.scope)
                    ? CollectionListElement.ActionState.CREATE_FRONTIER
                    : CollectionListElement.ActionState.NONE;
        }

        if (!isCompatibleWithMarkedType(group, getEligibleFrontierIds(group))) {
            return CollectionListElement.ActionState.NONE;
        }

        if (!canUseCollectionAsMoveTarget(group)) {
            return CollectionListElement.ActionState.NONE;
        }

        UUID targetCollectionId = group.collection == null ? null : group.collection.getId();
        for (FrontierOverlay frontier : getMarkedFrontiers()) {
            if (!Objects.equals(frontier.getCollectionId(), targetCollectionId)) {
                return CollectionListElement.ActionState.MOVE_HERE_ENABLED;
            }
        }

        return CollectionListElement.ActionState.MOVE_HERE_DISABLED;
    }

    private void toggleFrontierMarked(FrontierOverlay frontier) {
        if (!canMarkFrontier(frontier)) {
            return;
        }

        MarkedType frontierType = getMarkedType(frontier);
        if (markedType == MarkedType.NONE) {
            markedType = frontierType;
        } else if (markedType != frontierType) {
            return;
        }

        if (!markedFrontierIds.add(frontier.getId())) {
            markedFrontierIds.remove(frontier.getId());
        }

        if (markedFrontierIds.isEmpty()) {
            markedType = MarkedType.NONE;
        }

        rebuildTerritories();
    }

    private void toggleCollectionGroupMarked(CollectionListElement groupElement) {
        List<UUID> eligibleFrontierIds = groupElement.getEligibleFrontierIds();
        if (eligibleFrontierIds.isEmpty()) {
            return;
        }

        MarkedType groupType = resolveMarkedType(eligibleFrontierIds);
        if (groupType == null) {
            return;
        }
        if (markedType == MarkedType.NONE) {
            markedType = groupType;
        } else if (markedType != groupType) {
            return;
        }

        int currentlyMarked = 0;
        for (UUID frontierId : eligibleFrontierIds) {
            if (markedFrontierIds.contains(frontierId)) {
                ++currentlyMarked;
            }
        }

        if (currentlyMarked == 0) {
            markedFrontierIds.addAll(eligibleFrontierIds);
        } else {
            markedFrontierIds.removeAll(eligibleFrontierIds);
        }

        if (markedFrontierIds.isEmpty()) {
            markedType = MarkedType.NONE;
        }

        rebuildTerritories();
    }

    private void moveMarkedFrontiersToCollection(CollectionListElement targetElement) {
        UUID targetCollectionId = targetElement.getCollection() == null ? null : targetElement.getCollection().getId();

        for (FrontierOverlay frontier : getMarkedFrontiers()) {
            if (Objects.equals(frontier.getCollectionId(), targetCollectionId)) {
                continue;
            }

            FrontierChange change = new FrontierChange();
            change.setCollectionId(targetCollectionId);
            MapFrontiersClient.getOperationService().updateFrontier(frontier, change);
        }
    }

    private List<FrontierOverlay> getMarkedFrontiers() {
        List<FrontierOverlay> frontiers = new ArrayList<>();
        for (FrontierOverlay frontier : MapFrontiersClient.getAllFrontiers(markedType != MarkedType.GLOBAL)) {
            if (markedFrontierIds.contains(frontier.getId())) {
                frontiers.add(frontier);
            }
        }
        return frontiers;
    }

    private boolean isCompatibleWithMarkedType(FrontierOverlay frontier) {
        return markedType == MarkedType.NONE || markedType == getMarkedType(frontier);
    }

    private boolean isCompatibleWithMarkedType(TerritoryGroupModel group, List<UUID> eligibleFrontierIds) {
        if (markedType == MarkedType.NONE) {
            return true;
        }

        if (group.virtualRow) {
            return markedType == getMarkedType(group.scope);
        }

        if (group.collection != null) {
            return markedType == getMarkedType(group.collection);
        }

        MarkedType groupType = resolveMarkedType(eligibleFrontierIds);
        return groupType != null && markedType == groupType;
    }

    private MarkedType getMarkedType(FrontierOverlay frontier) {
        if (!frontier.getPersonal()) {
            return MarkedType.GLOBAL;
        }
        return frontier.isSessionOnly() ? MarkedType.PERSONAL_SESSION : MarkedType.PERSONAL_PERSISTENT;
    }

    private MarkedType getMarkedType(CollectionData collection) {
        if (!collection.getPersonal()) {
            return MarkedType.GLOBAL;
        }
        return collection.isSessionOnly() ? MarkedType.PERSONAL_SESSION : MarkedType.PERSONAL_PERSISTENT;
    }

    private MarkedType getMarkedType(CollectionScope scope) {
        return switch (scope) {
            case GLOBAL_PERSISTENT -> MarkedType.GLOBAL;
            case PERSONAL_PERSISTENT -> MarkedType.PERSONAL_PERSISTENT;
            case PERSONAL_SESSION -> MarkedType.PERSONAL_SESSION;
        };
    }

    private boolean canMarkCollectionGroup(TerritoryGroupModel group, List<UUID> eligibleFrontierIds) {
        if (eligibleFrontierIds.isEmpty()) {
            return false;
        }

        return resolveMarkedType(eligibleFrontierIds) != null;
    }

    private @Nullable MarkedType resolveMarkedType(List<UUID> frontierIds) {
        MarkedType resolved = null;
        for (UUID frontierId : frontierIds) {
            FrontierOverlay frontier = MapFrontiersClient.getAllFrontiers(true).stream()
                    .filter(candidate -> candidate.getId().equals(frontierId))
                    .findFirst()
                    .orElseGet(() -> MapFrontiersClient.getAllFrontiers(false).stream()
                            .filter(candidate -> candidate.getId().equals(frontierId))
                            .findFirst()
                            .orElse(null));
            if (frontier == null) {
                continue;
            }

            MarkedType frontierType = getMarkedType(frontier);
            if (resolved == null) {
                resolved = frontierType;
            } else if (resolved != frontierType) {
                return null;
            }
        }
        return resolved;
    }

    private List<UUID> getEligibleFrontierIds(TerritoryGroupModel group) {
        return group.filteredFrontiers.stream()
                .filter(this::canMarkFrontier)
                .map(FrontierOverlay::getId)
                .toList();
    }

    private void clearMarkedFrontiers() {
        markedFrontierIds.clear();
        markedType = MarkedType.NONE;
    }

    private void pruneMarkedFrontiers(Set<UUID> visibleFilteredFrontiers) {
        markedFrontierIds.retainAll(visibleFilteredFrontiers);
        if (markedFrontierIds.isEmpty()) {
            markedType = MarkedType.NONE;
        }
    }

    private boolean canManageGlobalCollections() {
        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        if (profile == null) {
            return false;
        }

        return profile.createFrontier == SettingsProfile.State.Enabled
                || profile.updateFrontier == SettingsProfile.State.Enabled;
    }

    private boolean canCreateCollection(CollectionScope scope) {
        if (minecraft.player == null) {
            return false;
        }

        if (scope != CollectionScope.GLOBAL_PERSISTENT) {
            return true;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && profile.createFrontier == SettingsProfile.State.Enabled;
    }

    private boolean canCreateFrontierInScope(CollectionScope scope) {
        if (minecraft.player == null) {
            return false;
        }

        if (scope != CollectionScope.GLOBAL_PERSISTENT) {
            return true;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && profile.createFrontier == SettingsProfile.State.Enabled;
    }

    private Tooltip getCreateCollectionTooltip(CollectionScope scope) {
        return Tooltip.create(Component.translatable(switch (scope) {
            case PERSONAL_PERSISTENT -> "mapfrontiers.create_collection_personal.tooltip";
            case PERSONAL_SESSION -> "mapfrontiers.create_collection_temporary.tooltip";
            case GLOBAL_PERSISTENT -> "mapfrontiers.create_collection_global.tooltip";
        }));
    }

    private SettingsProfile.AvailableActions getAvailableActions(FrontierOverlay frontier) {
        if (minecraft.player == null) {
            return new SettingsProfile.AvailableActions();
        }
        return SettingsProfile.getAvailableActions(MapFrontiersClient.getSettingsProfile(), frontier, new SettingsUser(minecraft.player));
    }

    private boolean canMarkFrontier(FrontierOverlay frontier) {
        if (minecraft.player == null) {
            return false;
        }

        if (frontier.getPersonal()) {
            return frontier.getOwner().equals(new SettingsUser(minecraft.player));
        }

        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(MapFrontiersClient.getSettingsProfile(),
                frontier, new SettingsUser(minecraft.player));
        return actions.canUpdate;
    }

    private boolean canUseCollectionAsMoveTarget(TerritoryGroupModel group) {
        if (group.scope == CollectionScope.GLOBAL_PERSISTENT) {
            return true;
        }

        if (minecraft.player == null) {
            return false;
        }

        if (group.virtualRow) {
            return true;
        }

        if (group.collection == null || !group.collection.getOwner().equals(new SettingsUser(minecraft.player))) {
            return false;
        }

        return markedType == getMarkedType(group.scope);
    }

    private boolean canDeleteSelectedCollection(CollectionData collection) {
        if (minecraft.player == null) {
            return false;
        }

        SettingsUser playerUser = new SettingsUser(minecraft.player);
        if (collection.getPersonal()) {
            return collection.getOwner().equals(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && (profile.deleteFrontier == SettingsProfile.State.Enabled
                || (profile.deleteFrontier == SettingsProfile.State.Owner && collection.getOwner().equals(playerUser)));
    }

    private boolean canUpdateSelectedCollection(CollectionData collection) {
        if (minecraft.player == null) {
            return false;
        }

        return SettingsProfile.canUpdateCollection(MapFrontiersClient.getSettingsProfile(), collection, new SettingsUser(minecraft.player));
    }

    private static int getShapeCount(FrontierOverlay frontier) {
        return switch (frontier.getShape()) {
            case Vertex -> frontier.getVertexCount();
            case Chunk -> frontier.getChunkCount();
            case Path -> frontier.getPointCount();
        };
    }

    private static CollectionUiStateStore getCollectionUiStateStore() {
        return MapFrontiersClient.getCollectionUiStateStore();
    }

    @FunctionalInterface
    private interface SortComparator<T> {
        int compare(TerritoryListSorting sort, T a, T b);
    }

    private static class TerritoryGroupModel {
        private final String rowId;
        private final @Nullable CollectionData collection;
        private final boolean virtualRow;
        private final CollectionScope scope;
        private final String title;
        private final boolean collapsed;
        private final List<FrontierOverlay> filteredFrontiers;
        private final SettingsUser owner;
        private final @Nullable Date created;
        private final @Nullable Date modified;
        private final int totalFrontiers;
        private final float totalArea;

        private TerritoryGroupModel(String rowId,
                                    @Nullable CollectionData collection,
                                    boolean virtualRow,
                                    CollectionScope scope,
                                    String title,
                                    boolean collapsed,
                                    List<FrontierOverlay> allFrontiers,
                                    List<FrontierOverlay> filteredFrontiers) {
            this.rowId = rowId;
            this.collection = collection;
            this.virtualRow = virtualRow;
            this.scope = scope;
            this.title = title;
            this.collapsed = collapsed;
            this.filteredFrontiers = filteredFrontiers;
            owner = collection == null ? new SettingsUser() : collection.getOwner();
            created = collection == null ? null : collection.getCreated();
            modified = collection == null ? null : collection.getModified();
            totalFrontiers = allFrontiers.size();

            float area = 0.f;
            for (FrontierOverlay frontier : allFrontiers) {
                area += frontier.area;
            }
            totalArea = area;
        }
    }
}
