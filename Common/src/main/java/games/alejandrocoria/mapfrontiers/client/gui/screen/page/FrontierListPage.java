package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.frontier.CollectionUiStateStore;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SortToolbar;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.CollectionBorderCapListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.CollectionListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.FrontierListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.FrontierListRowElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.RadioListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.SectionHeaderListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.SpacerListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteCollectionConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteFrontierConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.NewFrontierDialog;
import games.alejandrocoria.mapfrontiers.common.config.EnumConfigEntry;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.ChatFormatting;
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
public class FrontierListPage extends PageScreen
{
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_frontiers");
    private static final Component RESET_FILTERS_LABEL = Component.translatable("mapfrontiers.reset_filters");
    private static final Component FILTER_TYPE_LABEL = Component.translatable("mapfrontiers.filter_type");
    private static final Component FILTER_OWNER_LABEL = Component.translatable("mapfrontiers.filter_owner");
    private static final Component FILTER_DIMENSION_LABEL = Component.translatable("mapfrontiers.filter_dimension");
    private static final Component CONFIG_ALL_LABEL = Component.translatable("mapfrontiers.config.All");
    private static final Component CONFIG_CURRENT_LABEL = Component.translatable("mapfrontiers.config.Current");
    private static final Component OVERWORLD_LABEL = Component.literal("minecraft:overworld");
    private static final Component THE_NETHER_LABEL = Component.literal("minecraft:the_nether");
    private static final Component THE_END_LABEL = Component.literal("minecraft:the_end");
    private static final Component CREATE_LABEL = Component.translatable("mapfrontiers.create");
    private static final Component INFO_LABEL = Component.translatable("mapfrontiers.info");
    private static final Component DELETE_LABEL = Component.translatable("mapfrontiers.delete");
    private static final Component HIDE_LABEL = Component.translatable("mapfrontiers.hide");
    private static final Component SETTINGS_LABEL = Component.translatable("mapfrontiers.settings");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final int CONTENT_TOP = 60;
    private static final int FRONTIERS_WIDTH = 450;
    private static final int FRONTIERS_ELEMENT_HEIGHT = 25;
    private static final int FILTER_WIDTH = 200;
    private static final int FILTER_ELEMENT_HEIGHT = 15;
    private static final int FRONTIERS_MIN_ROWS = 7;
    private static final int FILTER_MIN_ROWS = 3;
    private static final int FILTER_DIMENSION_MIN_ROWS = 2;
    private static final float MIN_COLLECTION_BRIGHTNESS = 0.3f;
    private static final String PERSONAL_VIRTUAL_COLLECTION_ID = "mapfrontiers:personal_virtual_collection";
    private static final String TEMPORARY_VIRTUAL_COLLECTION_ID = "mapfrontiers:temporary_virtual_collection";
    private static final String GLOBAL_VIRTUAL_COLLECTION_ID = "mapfrontiers:global_virtual_collection";
    private static final String NEW_ACTION_LABEL = "Nueva";
    private static final String MOVE_HERE_ACTION_LABEL = "Mover aca";

    private final IClientAPI jmAPI;
    private final FullscreenMap fullscreenMap;
    private final Set<UUID> markedFrontierIds = new HashSet<>();
    private MarkedType markedType = MarkedType.NONE;

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
    private @Nullable String selectedRowId;

    private enum MarkedType {
        NONE,
        PERSONAL_PERSISTENT,
        PERSONAL_SESSION,
        GLOBAL
    }

    public FrontierListPage(IClientAPI jmAPI, FullscreenMap fullscreenMap) {
        super(TITLE_LABEL);
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

        MapFrontiersClient.getCollectionEvents().subscribeCreated(this, collection -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getCollectionEvents().subscribeUpdated(this, collection -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getCollectionEvents().subscribeDeleted(this, collectionId -> {
            updateFrontiers();
            updateButtons();
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            updateFrontiers();
            updateButtons();
        });
    }

    @Override
    protected void initScreen() {
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
    protected int getMinimumLayoutExtraWidth() {
        return LayoutConstants.PAGE_MARGIN * 2;
    }

    @Override
    protected int getMinimumLayoutExtraHeight() {
        return CONTENT_TOP + LayoutConstants.PAGE_MARGIN + 1;
    }

    @Override
    protected void resetContentToMinimumSize() {
        frontiers.setVisibleRows(FRONTIERS_MIN_ROWS);
        filterDimension.setVisibleRows(FILTER_DIMENSION_MIN_ROWS);
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        frontiers.setSize(FRONTIERS_WIDTH, Math.max(ScrollBox.heightForRows(FRONTIERS_MIN_ROWS, FRONTIERS_ELEMENT_HEIGHT),
                getAvailableScrollHeightInsideBackground(frontiers)));
        filterDimension.setSize(FILTER_WIDTH, Math.max(ScrollBox.heightForRows(FILTER_DIMENSION_MIN_ROWS, FILTER_ELEMENT_HEIGHT),
                getAvailableScrollHeightInsideBackground(filterDimension)));
    }

    private int getAvailableScrollHeightInsideBackground(ScrollBox scrollBox) {
        int scrollBoxY = CONTENT_TOP + scrollBox.getY() - content.getY();
        return Math.max(0, actualHeight - scrollBoxY - LayoutConstants.PAGE_MARGIN - 1);
    }

    @Override
    protected void positionContent() {
        content.setPosition((actualWidth - content.getWidth()) / 2, CONTENT_TOP);
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
        frontiers = ScrollBox.withRows(FRONTIERS_MIN_ROWS, FRONTIERS_WIDTH, FRONTIERS_ELEMENT_HEIGHT);
        frontiers.setElementClickedCallback(this::onFrontierRowClicked);
        mainLayout.addChild(frontiers, 1, 0, LayoutSettings.defaults().alignHorizontallyRight());
    }

    private void buildFiltersColumn(GridLayout mainLayout) {
        mainLayout.addChild(createResetFiltersButton(), 0, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        LinearLayout filtersColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        filtersColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(filtersColumn, 1, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        buildTypeFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildOwnerFilter(filtersColumn);
        filtersColumn.addChild(SpacerElement.height(4));
        buildDimensionFilter(filtersColumn);
    }

    private void buildTypeFilter(LinearLayout column) {
        column.addChild(createSectionLabel(FILTER_TYPE_LABEL));

        filterType = createFilterScrollBox(FILTER_MIN_ROWS);
        addEnumFilterOptions(filterType, ClientConfig.FILTER_FRONTIER_TYPE);
        filterType.setElementClickedCallback(this::onTypeFilterSelected);
        column.addChild(filterType);
    }

    private void buildOwnerFilter(LinearLayout column) {
        column.addChild(createSectionLabel(FILTER_OWNER_LABEL));

        filterOwner = createFilterScrollBox(FILTER_MIN_ROWS);
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
        buttonCreate = addBottomButton(createCreateButton());
        buttonInfo = addBottomButton(createInfoButton());
        buttonDelete = addBottomButton(createDeleteButton());
        buttonVisible = addBottomButton(createVisibleButton());
        buttonSettings = addBottomButton(createSettingsButton());
        addBottomButton(createDoneButton());
    }

    private SimpleButton createResetFiltersButton() {
        return new SimpleButton(font, 110, RESET_FILTERS_LABEL, button -> onResetFiltersPressed());
    }

    private SimpleButton createCreateButton() {
        return new SimpleButton(font, 110, CREATE_LABEL, button -> onCreatePressed());
    }

    private SimpleButton createInfoButton() {
        return new SimpleButton(font, 110, INFO_LABEL, button -> onInfoPressed());
    }

    private SimpleButton createDeleteButton() {
        SimpleButton button = new SimpleButton(font, 110, DELETE_LABEL, pressedButton -> onDeletePressed());
        button.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        return button;
    }

    private SimpleButton createVisibleButton() {
        return new SimpleButton(font, 110, HIDE_LABEL, button -> onVisiblePressed());
    }

    private SimpleButton createSettingsButton() {
        return new SimpleButton(font, 110, SETTINGS_LABEL, button -> onSettingsPressed());
    }

    private SimpleButton createDoneButton() {
        return new SimpleButton(font, 110, DONE_LABEL, button -> onDonePressed());
    }

    private StringWidget createSectionLabel(Component label) {
        return new StringWidget(label, font).setColor(ColorConstants.TEXT);
    }

    private <T> RadioListElement<T> createRadioFilterOption(Component label, T value) {
        return new RadioListElement<>(font, label, value);
    }

    private ScrollBox createFilterScrollBox(int rows) {
        return ScrollBox.withRows(rows, FILTER_WIDTH, FILTER_ELEMENT_HEIGHT);
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
        clearMarkedFrontiers();
        updateFrontiers();
        refreshViewState();
    }

    private void onFrontierRowClicked(ScrollElement element) {
        if (element instanceof FrontierListElement frontierElement && frontierElement.consumeMarkToggleRequested()) {
            toggleFrontierMarked(frontierElement.getFrontier());
            return;
        }

        if (!(element instanceof FrontierListRowElement rowElement)) {
            return;
        }

        if (element instanceof CollectionListElement collectionElement) {
            if (collectionElement.consumeCollapseToggleRequested()) {
                CollectionUiStateStore collapseState = getCollectionUiStateStore();
                collapseState.setCollapsed(rowElement.getRowId(), !collapseState.isCollapsed(rowElement.getRowId()));
                updateFrontiers();
                refreshViewState();
                return;
            }

            if (collectionElement.consumeMarkToggleRequested()) {
                toggleCollectionGroupMarked(collectionElement);
                return;
            }

            if (collectionElement.consumeActionRequested()) {
                if (collectionElement.isActionEnabled()) {
                    selectedRowId = rowElement.getRowId();
                    frontiers.selectElement(collectionElement);
                    fullscreenMap.selectFrontier(null);
                    if (isMarkedModeActive()) {
                        moveMarkedFrontiersToCollection(collectionElement);
                        updateFrontiers();
                    } else if (collectionElement.isVirtualRow()) {
                        createCollectionFromVirtualRow(collectionElement);
                    }
                }
                refreshViewState();
                return;
            }
        }

        selectedRowId = rowElement.getRowId();

        if (element instanceof CollectionListElement) {
            fullscreenMap.selectFrontier(null);
            refreshViewState();
            return;
        }

        if (element instanceof FrontierListElement frontierElement) {
            fullscreenMap.selectFrontier(frontierElement.getFrontier());
        } else {
            fullscreenMap.selectFrontier(null);
        }

        refreshViewState();
    }

    private void onResetFiltersPressed() {
        resetFiltersToDefaults();
        syncFilterSelectionsFromConfig();
        notifyFiltersChanged();
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
            CollectionListElement selectedCollectionElement = getSelectedCollectionElement();
            if (selectedCollectionElement != null) {
                if (selectedCollectionElement.getScope() == CollectionScope.PERSONAL_SESSION) {
                    showTemporaryFrontierCreateConfirmation(() -> openNewFrontierDialog(selectedCollectionElement));
                } else {
                    openNewFrontierDialog(selectedCollectionElement);
                }
            } else {
                new NewFrontierDialog(jmAPI, minecraft.player.blockPosition(), createNewFrontierResultHandler()).display();
            }
        }
    }

    private void openNewFrontierDialog(CollectionListElement selectedCollectionElement) {
        if (minecraft.player == null) {
            return;
        }

        UUID collectionId = selectedCollectionElement.getCollection() == null ? null : selectedCollectionElement.getCollection().getId();
        FrontierData.FrontierLifetime lifetime = selectedCollectionElement.getScope() == CollectionScope.PERSONAL_SESSION
                ? FrontierData.FrontierLifetime.SESSION_ONLY
                : FrontierData.FrontierLifetime.PERSISTENT;
        new NewFrontierDialog(jmAPI, minecraft.player.blockPosition(), selectedCollectionElement.isPersonal(), lifetime, collectionId,
                createNewFrontierResultHandler()).display();
    }

    private NewFrontierDialog.ResultHandler createNewFrontierResultHandler() {
        return new NewFrontierDialog.ResultHandler() {
            @Override
            public void beforeCreate(NewFrontierDialog dialog, ClientConfig.AfterCreatingFrontier action) {
                if (action == ClientConfig.AfterCreatingFrontier.EditShape) {
                    dialog.closeToFullscreenMap();
                } else {
                    dialog.onClose();
                }
            }

            @Override
            public void onFrontierCreated(FrontierOverlay frontier, ClientConfig.AfterCreatingFrontier action) {
                if (action == ClientConfig.AfterCreatingFrontier.EditShape) {
                    fullscreenMap.beginEditingFrontier(frontier);
                    return;
                }

                selectCreatedFrontier(frontier);
                if (action == ClientConfig.AfterCreatingFrontier.InfoScreen) {
                    new FrontierInfoPage(jmAPI, frontier).display();
                }
            }
        };
    }

    private void selectCreatedFrontier(FrontierOverlay frontier) {
        selectedRowId = frontier.getId().toString();
        fullscreenMap.selectFrontier(frontier);
        updateFrontiers();
        frontiers.scrollSelectedElementIntoView();
        refreshViewState();
    }

    private void onInfoPressed() {
        FrontierOverlay frontier = getSelectedFrontier();
        if (frontier != null) {
            new FrontierInfoPage(jmAPI, frontier).display();
            return;
        }

        CollectionData collection = getSelectedCollection();
        if (collection != null) {
            new CollectionInfoPage(collection).display();
        }
    }

    private void onDeletePressed() {
        CollectionData collection = getSelectedCollection();
        if (collection != null) {
            if (ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.get()) {
                showDeleteCollectionConfirmation();
            } else {
                deleteSelectedCollection();
            }
            return;
        }

        if (getSelectedFrontier() == null) {
            return;
        }

        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            showDeleteConfirmation();
        } else {
            deleteSelectedFrontier();
        }
    }

    private void showDeleteConfirmation() {
        FrontierOverlay frontier = getSelectedFrontier();
        if (frontier == null) {
            return;
        }

        new DeleteFrontierConfirmationDialog(frontier, response -> {
            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                ClientGlobalEvents.postUpdatedConfigEvent();
            }
            deleteSelectedFrontier();
        }).display();
    }

    private void showDeleteCollectionConfirmation() {
        new DeleteCollectionConfirmationDialog(response -> {
            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.set(false);
                ClientGlobalEvents.postUpdatedConfigEvent();
            }
            deleteSelectedCollection();
        }).display();
    }

    private void onVisiblePressed() {
        FrontierOverlay frontier = getSelectedFrontier();
        if (frontier == null) {
            return;
        }

        frontier.toggleVisibility(FrontierData.VisibilityData.Visibility.Frontier);
        MapFrontiersClient.getOperationService().updateFrontier(frontier);
        refreshViewState();
    }

    private void onSettingsPressed() {
        clearMarkedFrontiers();
        updateFrontiers();
        refreshViewState();
        new ModSettingsPage(true).display();
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
        clearMarkedFrontiers();
        updateFrontiers();
        ClientGlobalEvents.postUpdatedConfigEvent();
        refreshViewState();
    }

    private void refreshInitialSelection() {
        if (selectedRowId != null) {
            selectRowIfPresent(selectedRowId);
            frontiers.scrollSelectedElementIntoView();
            return;
        }

        FrontierOverlay selectedFrontier = fullscreenMap.getSelected();
        if (selectedFrontier != null) {
            selectedRowId = selectedFrontier.getId().toString();
            selectRowIfPresent(selectedRowId);
            frontiers.scrollSelectedElementIntoView();
        }
    }

    private void refreshViewState() {
        updateButtons();
    }

    private void deleteSelectedFrontier() {
        FrontierOverlay frontier = getSelectedFrontier();
        if (frontier == null) {
            return;
        }

        MapFrontiersClient.getOperationService().deleteFrontier(frontier);
        selectedRowId = null;
        fullscreenMap.selectFrontier(null);
        updateFrontiers();
        refreshViewState();
    }

    private void deleteSelectedCollection() {
        CollectionData collection = getSelectedCollection();
        if (collection == null) {
            return;
        }

        MapFrontiersClient.getOperationService().deleteCollection(collection);
        selectedRowId = null;
        updateFrontiers();
        refreshViewState();
    }

    private void createCollectionFromVirtualRow(CollectionListElement virtualRow) {
        if (minecraft.player == null || !virtualRow.isVirtualRow() || !canCreateCollection(virtualRow.getScope())) {
            return;
        }

        if (virtualRow.getScope() == CollectionScope.PERSONAL_SESSION) {
            showTemporaryCollectionCreateConfirmation(() -> createCollectionFromVirtualRowConfirmed(virtualRow));
            return;
        }
        createCollectionFromVirtualRowConfirmed(virtualRow);
    }

    private void createCollectionFromVirtualRowConfirmed(CollectionListElement virtualRow) {
        if (minecraft.player == null) {
            return;
        }

        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setPersonal(virtualRow.isPersonal());
        collection.setLifetime(virtualRow.getScope() == CollectionScope.PERSONAL_SESSION
                ? FrontierData.FrontierLifetime.SESSION_ONLY
                : FrontierData.FrontierLifetime.PERSISTENT);
        collection.setOwner(new SettingsUser(minecraft.player));

        selectedRowId = collection.getId().toString();
        MapFrontiersClient.getOperationService().createCollection(collection);
        new CollectionInfoPage(collection).display();
    }

    private void showTemporaryFrontierCreateConfirmation(Runnable onConfirm) {
        if (!ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE.get()) {
            onConfirm.run();
            return;
        }

        new ConfirmationDialog(
                "mapfrontiers.create_temporary_frontier_dialog",
                "mapfrontiers.create_temporary_frontier_dialog_desc",
                "mapfrontiers.create",
                "gui.cancel",
                "mapfrontiers.create_and_dont_ask_again",
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

        new ConfirmationDialog(
                "mapfrontiers.create_temporary_collection_dialog",
                "mapfrontiers.create_temporary_collection_dialog_desc",
                "mapfrontiers.create",
                "gui.cancel",
                "mapfrontiers.create_and_dont_ask_again",
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

    private void updateFrontiers() {
        String previousSelection = selectedRowId;
        FrontierOverlay selectedFrontierBeforeUpdate = getSelectedFrontier();
        boolean previousSelectionWasFrontier = previousSelection != null
                && selectedFrontierBeforeUpdate != null
                && previousSelection.equals(selectedFrontierBeforeUpdate.getId().toString());
        int previousScrollOffset = frontiers.getScrollOffset();
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
                rows.add(new SpacerListElement(FRONTIERS_WIDTH, 5));
            }
            rows.addAll(block);
            firstBlock = false;
        }
        pruneMarkedFrontiers(visibleFilteredFrontiers);

        Set<String> validRowIds = new HashSet<>();
        for (ScrollElement row : rows) {
            if (row instanceof FrontierListRowElement frontierRow) {
                validRowIds.add(frontierRow.getRowId());
            }
        }
        collapseState.prune(validRowIds);

        frontiers.removeAll();
        for (ScrollElement row : rows) {
            frontiers.addElement(row);
        }
        frontiers.setScrollOffset(previousScrollOffset);

        if (previousSelection != null && selectRowIfPresent(previousSelection)) {
            selectedRowId = previousSelection;
            frontiers.scrollSelectedElementIntoView();
        } else {
            selectedRowId = null;
            if (previousSelectionWasFrontier) {
                fullscreenMap.selectFrontier(null);
            }
        }

        updateButtons();
    }

    private boolean selectRowIfPresent(String rowId) {
        frontiers.selectElementIf(element -> element instanceof FrontierListRowElement rowElement && rowElement.getRowId().equals(rowId));
        return frontiers.getSelectedElement() instanceof FrontierListRowElement rowElement && rowElement.getRowId().equals(rowId);
    }

    private List<ScrollElement> buildBlockRows(CollectionScope scope, Set<UUID> visibleFilteredFrontiers) {
        List<ScrollElement> rows = new ArrayList<>();
        List<CollectionGroupModel> collectionGroups = buildCollectionGroups(scope);
        boolean includeVirtualRow = scope != CollectionScope.GLOBAL_PERSISTENT || shouldShowGlobalVirtualRow() || !collectionGroups.isEmpty();

        if (!includeVirtualRow) {
            return rows;
        }

        CollectionGroupModel virtualGroup = buildVirtualGroup(scope);
        String headerText = getHeaderText(scope);
        rows.add(new SectionHeaderListElement(font, headerText, FRONTIERS_WIDTH, ColorConstants.SCROLL_HEADER));
        rows.add(createCollectionRowElement(virtualGroup, ColorConstants.VIRTUAL_COLLECTION));
        if (!virtualGroup.collapsed) {
            addFrontierChildren(rows, virtualGroup.filteredFrontiers, visibleFilteredFrontiers, ColorConstants.VIRTUAL_COLLECTION);
        }
        rows.add(new CollectionBorderCapListElement(FRONTIERS_WIDTH, ColorConstants.VIRTUAL_COLLECTION));

        collectionGroups.sort(this::compareCollectionGroups);
        for (CollectionGroupModel group : collectionGroups) {
            rows.add(new SpacerListElement(FRONTIERS_WIDTH, 2));
            int collectionColor = ColorHelper.ensureMinBrightness(group.collection.getColor(), MIN_COLLECTION_BRIGHTNESS);
            rows.add(createCollectionRowElement(group, collectionColor));
            if (!group.collapsed) {
                addFrontierChildren(rows, group.filteredFrontiers, visibleFilteredFrontiers, collectionColor);
            }
            rows.add(new CollectionBorderCapListElement(FRONTIERS_WIDTH, collectionColor));
        }

        rows.stream()
                .filter(row -> row instanceof CollectionListElement)
                .map(row -> (CollectionListElement) row)
                .forEach(row -> visibleFilteredFrontiers.addAll(row.getEligibleFrontierIds()));

        return rows;
    }

    private CollectionListElement createCollectionRowElement(CollectionGroupModel group, int collectionColor) {
        List<UUID> eligibleFrontierIds = group.filteredFrontiers.stream()
                .filter(this::canMarkFrontier)
                .map(FrontierOverlay::getId)
                .toList();
        boolean canMarkGroup = canMarkCollectionGroup(group, eligibleFrontierIds);

        return new CollectionListElement(group.rowId, font, group.collection, group.virtualRow, group.scope, group.title,
                formatCollectionCounters(group.totalFrontiers, group.filteredFrontiers.size()),
                collectionColor,
                group.collapsed,
                shouldShowCheckboxInMarkedMode(canMarkGroup, isCompatibleWithMarkedType(group, eligibleFrontierIds)),
                shouldShowCheckboxOnHover(canMarkGroup),
                countMarkedFrontiers(eligibleFrontierIds),
                eligibleFrontierIds.size(),
                getCollectionActionLabel(group),
                isCollectionActionEnabled(group),
                getCollectionActionWidth(),
                eligibleFrontierIds,
                FRONTIERS_WIDTH);
    }

    private void addFrontierChildren(List<ScrollElement> rows, List<FrontierOverlay> filteredFrontiers,
                                     Set<UUID> visibleFilteredFrontiers, int collectionColor) {
        filteredFrontiers.sort(this::compareFrontiers);
        for (FrontierOverlay frontier : filteredFrontiers) {
            if (canMarkFrontier(frontier)) {
                visibleFilteredFrontiers.add(frontier.getId());
            }
            rows.add(new FrontierListElement(font, frontier, FRONTIERS_WIDTH, collectionColor,
                    shouldShowCheckboxInMarkedMode(canMarkFrontier(frontier), isCompatibleWithMarkedType(frontier)),
                    shouldShowCheckboxOnHover(canMarkFrontier(frontier)), isFrontierMarked(frontier)));
        }
    }

    private CollectionGroupModel buildVirtualGroup(CollectionScope scope) {
        String rowId = getVirtualCollectionRowId(scope);
        CollectionUiStateStore collapseState = getCollectionUiStateStore();
        List<FrontierOverlay> allFrontiers = new ArrayList<>(MapFrontiersClient.getFrontiersWithoutCollection(scope));
        List<FrontierOverlay> filteredFrontiers = filterFrontiers(allFrontiers);

        return new CollectionGroupModel(rowId,
                null,
                true,
                scope,
                I18n.get("mapfrontiers.no_collection"),
                collapseState.isCollapsed(rowId),
                allFrontiers,
                filteredFrontiers);
    }

    private List<CollectionGroupModel> buildCollectionGroups(CollectionScope scope) {
        CollectionUiStateStore collapseState = getCollectionUiStateStore();
        List<CollectionGroupModel> groups = new ArrayList<>();
        for (CollectionData collection : MapFrontiersClient.getCollections(scope)) {
            List<FrontierOverlay> allFrontiers = new ArrayList<>(MapFrontiersClient.getFrontiersInCollection(collection.getId()));
            List<FrontierOverlay> filteredFrontiers = filterFrontiers(allFrontiers);
            String title = collection.getName();
            if (StringUtil.isBlank(title)) {
                title = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            }

            String rowId = collection.getId().toString();
            groups.add(new CollectionGroupModel(rowId,
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
        return checkFilterType(frontier) && checkFilterOwner(frontier) && checkFilterDimension(frontier) && checkSearch(frontier);
    }

    private boolean checkFilterType(FrontierOverlay frontier) {
        return ClientConfig.FILTER_FRONTIER_TYPE.get() == ClientConfig.FilterFrontierType.All
                || ClientConfig.FILTER_FRONTIER_TYPE.get() == (frontier.getPersonal()
                ? ClientConfig.FilterFrontierType.Personal
                : ClientConfig.FilterFrontierType.Global);
    }

    private boolean checkFilterOwner(FrontierOverlay frontier) {
        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == ClientConfig.FilterFrontierOwner.All) {
            return true;
        }

        boolean ownerIsPlayer = minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player));

        if (ClientConfig.FILTER_FRONTIER_OWNER.get() == ClientConfig.FilterFrontierOwner.Self) {
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

    private int compareCollectionGroups(CollectionGroupModel a, CollectionGroupModel b) {
        return compareByConfiguredSorting(a, b, this::compareCollectionGroupsBySort);
    }

    private int compareFrontiers(FrontierOverlay a, FrontierOverlay b) {
        return compareByConfiguredSorting(a, b, this::compareFrontiersBySort);
    }

    private <T> int compareByConfiguredSorting(T a, T b, SortComparator<T> comparator) {
        List<ClientConfig.Sorting> sorting = ClientConfig.getFrontierSortingValues();
        List<Boolean> directions = ClientConfig.getFrontierSortingDirectionValues();
        for (int i = 0; i < sorting.size(); ++i) {
            int order = comparator.compare(sorting.get(i), a, b);
            if (order != 0) {
                return directions.get(i) ? order : -order;
            }
        }
        return 0;
    }

    private int compareFrontiersBySort(ClientConfig.Sorting sort, FrontierOverlay a, FrontierOverlay b) {
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

    private int compareCollectionGroupsBySort(ClientConfig.Sorting sort, CollectionGroupModel a, CollectionGroupModel b) {
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
            case PERSONAL_SESSION -> I18n.get("mapfrontiers.temporary") + " " + I18n.get("mapfrontiers.personal_frontiers_header");
            case GLOBAL_PERSISTENT -> I18n.get("mapfrontiers.global_frontiers_header");
        };
    }

    private String getVirtualCollectionRowId(CollectionScope scope) {
        return switch (scope) {
            case PERSONAL_PERSISTENT -> PERSONAL_VIRTUAL_COLLECTION_ID;
            case PERSONAL_SESSION -> TEMPORARY_VIRTUAL_COLLECTION_ID;
            case GLOBAL_PERSISTENT -> GLOBAL_VIRTUAL_COLLECTION_ID;
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

    private @Nullable String getCollectionActionLabel(CollectionGroupModel group) {
        if (!isMarkedModeActive()) {
            if (group.virtualRow && canCreateCollection(group.scope)) {
                return NEW_ACTION_LABEL;
            }
            return null;
        }

        if (!isCompatibleWithMarkedType(group, getEligibleFrontierIds(group))) {
            return null;
        }

        if (!canUseCollectionAsMoveTarget(group)) {
            return null;
        }

        return MOVE_HERE_ACTION_LABEL;
    }

    private boolean isCollectionActionEnabled(CollectionGroupModel group) {
        if (!isMarkedModeActive()) {
            return group.virtualRow && canCreateCollection(group.scope);
        }

        if (!isCompatibleWithMarkedType(group, getEligibleFrontierIds(group))) {
            return false;
        }

        if (!canUseCollectionAsMoveTarget(group)) {
            return false;
        }

        UUID targetCollectionId = group.collection == null ? null : group.collection.getId();
        for (FrontierOverlay frontier : getMarkedFrontiers()) {
            if (!Objects.equals(frontier.getCollectionId(), targetCollectionId)) {
                return true;
            }
        }

        return false;
    }

    private int getCollectionActionWidth() {
        return Math.max(font.width(NEW_ACTION_LABEL), font.width(MOVE_HERE_ACTION_LABEL)) + 8;
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

        updateFrontiers();
        refreshViewState();
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

        updateFrontiers();
        refreshViewState();
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

    private boolean isCompatibleWithMarkedType(CollectionGroupModel group, List<UUID> eligibleFrontierIds) {
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

    private boolean canMarkCollectionGroup(CollectionGroupModel group, List<UUID> eligibleFrontierIds) {
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

    private List<UUID> getEligibleFrontierIds(CollectionGroupModel group) {
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

    private boolean canCreateFrontierInSelection() {
        CollectionListElement selectedCollectionElement = getSelectedCollectionElement();
        if (selectedCollectionElement == null) {
            return true;
        }

        CollectionData selectedCollection = selectedCollectionElement.getCollection();
        if (selectedCollectionElement.isPersonal()) {
            return minecraft.player != null;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && profile.createFrontier == SettingsProfile.State.Enabled;
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

    private boolean canUseCollectionAsMoveTarget(CollectionGroupModel group) {
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

    private @Nullable FrontierOverlay getSelectedFrontier() {
        ScrollElement selectedElement = frontiers.getSelectedElement();
        if (selectedElement instanceof FrontierListElement frontierElement) {
            return frontierElement.getFrontier();
        }
        return null;
    }

    private @Nullable CollectionListElement getSelectedCollectionElement() {
        ScrollElement selectedElement = frontiers.getSelectedElement();
        if (selectedElement instanceof CollectionListElement collectionElement) {
            return collectionElement;
        }
        return null;
    }

    private @Nullable CollectionData getSelectedCollection() {
        CollectionListElement selectedCollectionElement = getSelectedCollectionElement();
        return selectedCollectionElement == null ? null : selectedCollectionElement.getCollection();
    }

    private void updateButtons() {
        if (minecraft.player == null) {
            return;
        }

        if (isMarkedModeActive()) {
            buttonCreate.active = false;
            buttonInfo.active = false;
            buttonDelete.active = false;
            buttonVisible.active = false;
            buttonVisible.setMessage(Component.translatable("mapfrontiers.hide"));
            buttonSettings.active = true;
            return;
        }

        CollectionListElement selectedCollectionElement = getSelectedCollectionElement();
        if (selectedCollectionElement != null) {
            CollectionData selectedCollection = selectedCollectionElement.getCollection();
            buttonCreate.active = canCreateFrontierInSelection();
            buttonInfo.active = selectedCollection != null;
            buttonDelete.active = selectedCollection != null && canDeleteSelectedCollection(selectedCollection);
            buttonVisible.active = false;
            buttonVisible.setMessage(Component.translatable("mapfrontiers.hide"));
            buttonSettings.active = true;
            return;
        }

        FrontierOverlay selectedFrontier = getSelectedFrontier();
        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, selectedFrontier, playerUser);

        buttonCreate.active = true;
        buttonInfo.active = selectedFrontier != null;
        buttonDelete.active = actions.canDelete;
        buttonVisible.active = actions.canUpdate;
        buttonSettings.active = true;

        if (selectedFrontier != null && selectedFrontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier)) {
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

    private static CollectionUiStateStore getCollectionUiStateStore() {
        return MapFrontiersClient.getCollectionUiStateStore();
    }

    @FunctionalInterface
    private interface SortComparator<T> {
        int compare(ClientConfig.Sorting sort, T a, T b);
    }

    private static class CollectionGroupModel {
        private final String rowId;
        private final @Nullable CollectionData collection;
        private final boolean virtualRow;
        private final CollectionScope scope;
        private final String title;
        private final boolean collapsed;
        private final List<FrontierOverlay> allFrontiers;
        private final List<FrontierOverlay> filteredFrontiers;
        private final SettingsUser owner;
        private final @Nullable Date created;
        private final @Nullable Date modified;
        private final int totalFrontiers;
        private final float totalArea;

        private CollectionGroupModel(String rowId,
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
            this.allFrontiers = allFrontiers;
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
