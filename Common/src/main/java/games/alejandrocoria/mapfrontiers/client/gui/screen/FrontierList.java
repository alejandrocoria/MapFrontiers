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
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.client.gui.GuiGraphics;
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

    private SortToolbar sortToolbar;
    private TextBox searchBox;
    private ScrollBox frontiers;
    private ScrollBox filterType;
    private ScrollBox filterOwner;
    private ScrollBox filterDimension;
    private SimpleButton buttonResetFilters;
    private SimpleButton buttonCreate;
    private SimpleButton buttonInfo;
    private SimpleButton buttonDelete;
    private SimpleButton buttonVisible;
    private SimpleButton buttonSettings;
    private SimpleButton buttonDone;

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
        GridLayout mainLayout = new GridLayout().columnSpacing(8).rowSpacing(4);
        content.addChild(mainLayout);
        LayoutSettings alignRightSettings = LayoutSettings.defaults().alignHorizontallyRight();
        LayoutSettings alignLeftSettings = LayoutSettings.defaults().alignHorizontallyLeft();


        LinearLayout toolbar = LinearLayout.horizontal();
        toolbar.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(toolbar, 0, 0, alignLeftSettings);

        sortToolbar = new SortToolbar(font, this::updateFrontiers);
        toolbar.addChild(sortToolbar);
        toolbar.addChild(SpacerElement.width(16));

        searchBox = new TextBox(font, 100, I18n.get("mapfrontiers.search"));
        searchBox.setMaxLength(40);
        searchBox.setHeight(16);
        searchBox.setValueChangedCallback(value -> updateFrontiers());
        toolbar.addChild(searchBox);



        frontiers = new ScrollBox(actualHeight - 120, 450, 24);
        frontiers.setElementDeletedCallback(element -> updateButtons());
        frontiers.setElementClickedCallback(element -> {
            FrontierOverlay frontier = ((FrontierListElement) element).getFrontier();
            fullscreenMap.selectFrontier(frontier);
            updateButtons();
        });
        mainLayout.addChild(frontiers, 1, 0, alignRightSettings);



        buttonResetFilters = new SimpleButton(font, 110, resetFiltersLabel, (b) -> {
            ClientConfig.FILTER_FRONTIER_TYPE.set(ClientConfig.FilterFrontierType.All);
            filterType.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_TYPE.get().ordinal());
            ClientConfig.FILTER_FRONTIER_OWNER.set(ClientConfig.FilterFrontierOwner.All);
            filterOwner.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_OWNER.get().ordinal());
            ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.DIMENSION_FILTER_ALL);
            filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_DIMENSION.get().hashCode());
            updateFrontiers();
            updateButtons();
        });
        mainLayout.addChild(buttonResetFilters, 0, 1, alignLeftSettings);


        LinearLayout rightColumn = LinearLayout.vertical().spacing(2);
        rightColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(rightColumn, 1, 1, alignLeftSettings);

        rightColumn.addChild(new StringWidget(filterTypeLabel, font).setColor(ColorConstants.TEXT));
        filterType = new ScrollBox(52, 200, 16);
        filterType.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierType.All), ClientConfig.FilterFrontierType.All.ordinal()));
        filterType.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierType.Global), ClientConfig.FilterFrontierType.Global.ordinal()));
        filterType.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierType.Personal), ClientConfig.FilterFrontierType.Personal.ordinal()));
        filterType.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_TYPE.get().ordinal());
        filterType.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            ClientConfig.FILTER_FRONTIER_TYPE.set(ClientConfig.FilterFrontierType.values()[selected]);
            updateFrontiers();
            ClientGlobalEvents.postUpdatedConfigEvent();
            updateButtons();
        });
        rightColumn.addChild(filterType);

        rightColumn.addChild(SpacerElement.height(4));
        rightColumn.addChild(new StringWidget(filterOwnerLabel, font).setColor(ColorConstants.TEXT));
        filterOwner = new ScrollBox(52, 200, 16);
        filterOwner.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierOwner.All), ClientConfig.FilterFrontierOwner.All.ordinal()));
        filterOwner.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierOwner.Self), ClientConfig.FilterFrontierOwner.Self.ordinal()));
        filterOwner.addElement(new RadioListElement(font, ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierOwner.Others), ClientConfig.FilterFrontierOwner.Others.ordinal()));
        filterOwner.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_OWNER.get().ordinal());
        filterOwner.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            ClientConfig.FILTER_FRONTIER_OWNER.set(ClientConfig.FilterFrontierOwner.values()[selected]);
            updateFrontiers();
            ClientGlobalEvents.postUpdatedConfigEvent();
            updateButtons();
        });
        rightColumn.addChild(filterOwner);

        rightColumn.addChild(SpacerElement.height(4));
        rightColumn.addChild(new StringWidget(filterDimensionLabel, font).setColor(ColorConstants.TEXT));
        filterDimension = new ScrollBox(actualHeight - 274, 200, 16);
        filterDimension.addElement(new RadioListElement(font, configAllLabel, ClientConfig.DIMENSION_FILTER_ALL.hashCode()));
        filterDimension.addElement(new RadioListElement(font, configCurrentLabel, ClientConfig.DIMENSION_FILTER_CURRENT.hashCode()));
        filterDimension.addElement(new RadioListElement(font, overworldLabel, "minecraft:overworld".hashCode()));
        filterDimension.addElement(new RadioListElement(font, theNetherLabel, "minecraft:the_nether".hashCode()));
        filterDimension.addElement(new RadioListElement(font, theEndLabel, "minecraft:the_end".hashCode()));
        addDimensionsToFilter();
        filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_DIMENSION.get().hashCode());
        filterDimension.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            if (selected == ClientConfig.DIMENSION_FILTER_ALL.hashCode()) {
                ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.DIMENSION_FILTER_ALL);
            } else if (selected == ClientConfig.DIMENSION_FILTER_CURRENT.hashCode()) {
                ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.DIMENSION_FILTER_CURRENT);
            } else {
                ClientConfig.FILTER_FRONTIER_DIMENSION.set(getDimensionFromHash(selected));
            }
            updateFrontiers();
            ClientGlobalEvents.postUpdatedConfigEvent();
            updateButtons();
        });
        if (filterDimension.getSelectedElement() == null) {
            ClientConfig.FILTER_FRONTIER_DIMENSION.set(ClientConfig.DIMENSION_FILTER_ALL);
            filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == ClientConfig.FILTER_FRONTIER_DIMENSION.get().hashCode());
        }
        rightColumn.addChild(filterDimension);

        buttonCreate = bottomButtons.addChild(new SimpleButton(font, 110, createLabel, (b) -> new NewFrontier(jmAPI, minecraft.player.blockPosition()).display()));
        buttonInfo = bottomButtons.addChild(new SimpleButton(font, 110, infoLabel, (b) -> {
            FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
            new FrontierInfo(jmAPI, frontier).display();
        }));
        buttonDelete = bottomButtons.addChild(new SimpleButton(font, 110, deleteLabel, (b) -> {
            if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
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
            } else {
                deleteSelectedFrontier();
            }
        }));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonVisible = bottomButtons.addChild(new SimpleButton(font, 110, hideLabel, (b) -> {
            FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
            frontier.toggleVisibility(FrontierData.VisibilityData.Visibility.Frontier);
            MapFrontiersClient.getOperationService().updateFrontier(frontier);
            updateButtons();
        }));
        buttonSettings = bottomButtons.addChild(new SimpleButton(font, 110, settingsLabel, (b) -> new ModSettings(true).display()));
        buttonDone = bottomButtons.addChild(new SimpleButton(font, 110, doneLabel, (b) -> onClose()));

        updateFrontiers();

        if (fullscreenMap.getSelected() != null) {
            frontiers.selectElementIf((element) -> ((FrontierListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
        }

        updateButtons();
    }

    @Override
    public void repositionElements() {
        frontiers.setSize(450, actualHeight - 100);
        filterDimension.setSize(200, actualHeight - 269);
        super.repositionElements();
        content.setPosition((actualWidth - content.getWidth()) / 2, 60);
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
                filterDimension.addElement(new RadioListElement(font, Component.literal(dimension), dimension.hashCode()));
            }
        }
    }

    private String getDimensionFromHash(int hash) {
        List<String> dimensions = Services.JOURNEYMAP.getDimensionList();
        for (String dimension : dimensions) {
            if (dimension.hashCode() == hash) {
                return dimension;
            }
        }

        return "";
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
                    case ClientConfig.Sorting.VertexChunk -> Integer.compare(Math.max(a.getChunkCount(), a.getVertexCount()), Math.max(b.getChunkCount(), b.getVertexCount()));
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
}
