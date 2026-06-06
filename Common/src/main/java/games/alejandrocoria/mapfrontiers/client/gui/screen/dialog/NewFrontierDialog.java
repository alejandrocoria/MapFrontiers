package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.AfterCreatingFrontier;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ChunkShapePresetSelector;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.PathShapePresetSelector;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.VertexShapePresetSelector;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class NewFrontierDialog extends PanelDialog {
    private static final Component FRONTIER_TYPE_LABEL = Component.translatable("mapfrontiers.frontier_type");
    private static final Component PERSONAL_LABEL = Component.translatable("mapfrontiers.personal_type");
    private static final Component GLOBAL_LABEL = Component.translatable("mapfrontiers.global_type");
    private static final Component FRONTIER_SHAPE_LABEL = Component.translatable("mapfrontiers.frontier_shape");
    private static final Component AFTER_CREATING_LABEL = Component.translatable("mapfrontiers.after_creating");
    private static final Component VERTEX_COUNT_LABEL = Component.translatable("mapfrontiers.shape_vertex_count");
    private static final Component POINT_COUNT_LABEL = Component.translatable("mapfrontiers.shape_point_count");
    private static final Component SIZE_INFO_LABEL = Component.translatable("mapfrontiers.shape_size_info");
    private static final String VERTICES_KEY = "mapfrontiers.vertices";
    private static final String POINTS_KEY = "mapfrontiers.points";
    private static final String CHUNKS_KEY = "mapfrontiers.chunks";
    private static final Component CREATE_LABEL = Component.translatable("mapfrontiers.create");
    private static final String CONTEXTUAL_HINT_KEY = "mapfrontiers.new_frontier_contextual_hint";
    private static final String CONTEXTUAL_HINT_TEMPORARY_KEY = "mapfrontiers.new_frontier_contextual_hint_temporary";
    private static final String CONTEXTUAL_HINT_IN_COLLECTION_KEY = "mapfrontiers.new_frontier_contextual_hint_in_collection";
    private static final String CONTEXTUAL_HINT_TEMPORARY_IN_COLLECTION_KEY = "mapfrontiers.new_frontier_contextual_hint_temporary_in_collection";
    private static final int OPTION_BUTTON_MIN_WIDTH = 64;
    private static final int TEXTBOX_MIN_WIDTH = 64;
    private static final int OPTION_BUTTON_HORIZONTAL_PADDING = 8;

    private enum FrontierTypeOption {
        GLOBAL,
        PERSONAL
    }

    public interface ResultHandler {
        void beforeCreate(NewFrontierDialog dialog, AfterCreatingFrontier action);

        void onFrontierCreated(FrontierOverlay frontier, AfterCreatingFrontier action);
    }

    private final IClientAPI jmAPI;
    private final BlockPos centerPos;
    private final @Nullable Boolean forcedPersonal;
    private final TerritoryLifetime frontierLifetime;
    private final @Nullable UUID collectionId;
    private final ResultHandler resultHandler;
    private final Object createdFrontierListenerOwner = new Object();
    private @Nullable UUID pendingCreatedFrontierId;

    private OptionButton buttonFrontierType;
    private OptionButton buttonFrontierShape;
    private OptionButton buttonAfterCreate;
    private VertexShapePresetSelector vertexShapePresetSelector;
    private ChunkShapePresetSelector chunkShapePresetSelector;
    private PathShapePresetSelector pathShapePresetSelector;
    private StringWidget labelCount;
    private StringWidget labelCountInfo;
    private TextBoxInt textCount;
    private StringWidget labelSize;
    private StringWidget labelSizeInfo;
    private TextBoxInt textSize;

    public NewFrontierDialog(IClientAPI jmAPI, BlockPos centerPos, @Nullable Boolean forcedPersonal,
                             TerritoryLifetime frontierLifetime, @Nullable UUID collectionId,
                             ResultHandler resultHandler) {
        super();
        this.jmAPI = jmAPI;
        this.centerPos = centerPos;
        this.forcedPersonal = forcedPersonal;
        this.frontierLifetime = frontierLifetime;
        this.collectionId = collectionId;
        this.resultHandler = resultHandler;

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            onClose();
            new NewFrontierDialog(jmAPI, centerPos, forcedPersonal, frontierLifetime, collectionId, resultHandler).display();
        });
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);
        LayoutSettings leftColumnSettings = LayoutSettings.defaults().alignHorizontallyRight();
        LayoutSettings rightColumnSettings = LayoutSettings.defaults().alignHorizontallyLeft();
        LayoutSettings centerColumnSettings = LayoutSettings.defaults().alignHorizontallyCenter();

        Component vertexLabel = ClientConfig.getTranslatedEnum(FrontierShape.Vertex);
        Component chunkLabel = ClientConfig.getTranslatedEnum(FrontierShape.Chunk);
        Component pathLabel = ClientConfig.getTranslatedEnum(FrontierShape.Path);
        Component infoScreenLabel = ClientConfig.getTranslatedEnum(AfterCreatingFrontier.InfoScreen);
        Component editShapeLabel = ClientConfig.getTranslatedEnum(AfterCreatingFrontier.EditShape);
        Component doNothingLabel = ClientConfig.getTranslatedEnum(AfterCreatingFrontier.DoNothing);
        int optionButtonWidth = ScreenHelper.getPaddedMaxTextWidth(font, OPTION_BUTTON_MIN_WIDTH,
                OPTION_BUTTON_HORIZONTAL_PADDING, GLOBAL_LABEL, PERSONAL_LABEL, vertexLabel, chunkLabel, pathLabel,
                infoScreenLabel, editShapeLabel, doNothingLabel);

        buttonFrontierType = createFrontierTypeButton(optionButtonWidth);
        if (shouldShowContextualHint()) {
            boolean personal = resolvePersonalSelection();
            Component contextualHint = createContextualTypeHint(personal).copy().withColor(ColorConstants.TEXT);
            MultiLineTextWidget hintWidget = new MultiLineTextWidget(contextualHint, font).setCentered(true);
            mainLayout.addChild(hintWidget, 0, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());
        } else {
            mainLayout.addChild(new StringWidget(FRONTIER_TYPE_LABEL, font).setColor(ColorConstants.TEXT), 0, 0, leftColumnSettings);
            mainLayout.addChild(buttonFrontierType, 0, 1, rightColumnSettings);
        }

        mainLayout.addChild(new StringWidget(FRONTIER_SHAPE_LABEL, font).setColor(ColorConstants.TEXT), 1, 0, leftColumnSettings);
        buttonFrontierShape = new OptionButton(font, optionButtonWidth, (b) -> {
                    ClientConfig.NEW_FRONTIER_SHAPE.set(FrontierShape.VALUES[b.getSelected()]);
                    shapePresetUpdated();
        });
        buttonFrontierShape.addOption(vertexLabel);
        buttonFrontierShape.addOption(chunkLabel);
        buttonFrontierShape.addOption(pathLabel);
        buttonFrontierShape.setSelected(ClientConfig.NEW_FRONTIER_SHAPE.get().ordinal());
        mainLayout.addChild(buttonFrontierShape, 1, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(AFTER_CREATING_LABEL, font).setColor(ColorConstants.TEXT), 2, 0, leftColumnSettings);
        buttonAfterCreate = new OptionButton(font, optionButtonWidth,
                (b) -> ClientConfig.AFTER_CREATING_FRONTIER.set(AfterCreatingFrontier.values()[b.getSelected()]));
        buttonAfterCreate.addOption(infoScreenLabel);
        buttonAfterCreate.addOption(editShapeLabel);
        buttonAfterCreate.addOption(doNothingLabel);
        buttonAfterCreate.setSelected(ClientConfig.AFTER_CREATING_FRONTIER.get().ordinal());
        mainLayout.addChild(buttonAfterCreate, 2, 1, rightColumnSettings);

        vertexShapePresetSelector = new VertexShapePresetSelector(font, ClientConfig.NEW_FRONTIER_VERTEX_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(vertexShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);
        chunkShapePresetSelector = new ChunkShapePresetSelector(font, ClientConfig.NEW_FRONTIER_CHUNK_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(chunkShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);
        pathShapePresetSelector = new PathShapePresetSelector(font, ClientConfig.NEW_FRONTIER_PATH_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(pathShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);

        labelCount = mainLayout.addChild(new StringWidget(VERTEX_COUNT_LABEL, font).setColor(ColorConstants.NEW_FRONTIER_DETAILS_TEXT), 4, 0, leftColumnSettings);
        textCount = new TextBoxInt(ClientConfig.NEW_FRONTIER_VERTEX_COUNT, font, TEXTBOX_MIN_WIDTH);
        textCount.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_VERTEX_COUNT.get()));
        textCount.setValueChangedCallback(ClientConfig.NEW_FRONTIER_VERTEX_COUNT::set);
        mainLayout.addChild(textCount, 4, 1, rightColumnSettings);

        labelCountInfo = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.NEW_FRONTIER_DETAILS_TEXT), 4, 0, 1, 2, centerColumnSettings);

        labelSize = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.NEW_FRONTIER_DETAILS_TEXT), 5, 0, leftColumnSettings);
        textSize = new TextBoxInt(ClientConfig.NEW_FRONTIER_PATH_SEGMENT_LENGTH, font, TEXTBOX_MIN_WIDTH);
        textSize.setValueChangedCallback(value -> {
            IntConfigEntry entry = activeSizeConfigEntry();
            if (entry != null) {
                entry.set(value);
            }

            if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Chunk) {
                if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Width) {
                    chunkShapePresetSelector.setSize(value);
                } else if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Length) {
                    chunkShapePresetSelector.setSize(value);
                }
            }
        });
        mainLayout.addChild(textSize, 5, 1, rightColumnSettings);

        labelSizeInfo = mainLayout.addChild(new StringWidget(SIZE_INFO_LABEL, font).setColor(ColorConstants.NEW_FRONTIER_DETAILS_TEXT), 5, 0, 1, 2, centerColumnSettings);

        addConfirmButton(CREATE_LABEL, (b) -> {
            boolean personal = resolvePersonalSelection();
            AfterCreatingFrontier afterCreate = ClientConfig.AFTER_CREATING_FRONTIER.get();
            resultHandler.beforeCreate(this, afterCreate);
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState != null) {
                FrontierCreateSpec createSpec = createFrontierSpec(personal, uiState.dimension);
                if (createSpec != null) {
                    FrontierOverlay createdFrontier = MapFrontiersClient.getOperationService().createNewFrontierAndReturn(createSpec);
                    if (createdFrontier != null) {
                        resultHandler.onFrontierCreated(createdFrontier, afterCreate);
                    } else {
                        awaitCreatedFrontier(createSpec.getFrontierId(), afterCreate);
                    }
                }
            }
        });
        addCancelButton();

        shapePresetUpdated();
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        if (pendingCreatedFrontierId == null) {
            MapFrontiersClient.getFrontierEvents().unsubscribe(createdFrontierListenerOwner);
        }
        ClientGlobalEvents.unsubscribeAllEvents(this);
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    public void closeToFullscreenMap() {
        closeAndReturnToFullscreenMap();
    }

    private void awaitCreatedFrontier(UUID frontierId, AfterCreatingFrontier afterCreate) {
        pendingCreatedFrontierId = frontierId;
        MapFrontiersClient.getFrontierEvents().unsubscribe(createdFrontierListenerOwner);
        MapFrontiersClient.getFrontierEvents().subscribeCreated(createdFrontierListenerOwner, (frontier, playerId) -> {
            if (!frontierId.equals(frontier.getId())) {
                return;
            }

            pendingCreatedFrontierId = null;
            MapFrontiersClient.getFrontierEvents().unsubscribe(createdFrontierListenerOwner);
            resultHandler.onFrontierCreated(frontier, afterCreate);
        });
    }

    private void shapePresetUpdated() {
        if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Vertex) {
            vertexShapePresetSelector.visible = true;
            chunkShapePresetSelector.visible = false;
            pathShapePresetSelector.visible = false;

            int selected = vertexShapePresetSelector.getSelected();
            ClientConfig.NEW_FRONTIER_VERTEX_SHAPE.set(selected);
            setLabelCountMessage(VERTEX_COUNT_LABEL);

            if (selected == 11) {
                labelCount.visible = true;
                textCount.visible = true;
                labelCountInfo.visible = false;
            } else {
                labelCount.visible = false;
                textCount.visible = false;
                labelCountInfo.visible = true;
                setLabelCountInfoMessage(VERTICES_KEY, vertexShapePresetSelector.getVertexCount());
            }

            labelSizeInfo.visible = false;

            if (selected == 0 || selected == 1) {
                labelSize.visible = false;
                textSize.visible = false;
                repositionElements();
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Width) {
                setLabelSizeMessage("mapfrontiers.shape_width");
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_WIDTH);
            } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
                setLabelSizeMessage("mapfrontiers.shape_radius");
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_RADIUS);
            }
        } else if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Chunk) {
            vertexShapePresetSelector.visible = false;
            chunkShapePresetSelector.visible = true;
            pathShapePresetSelector.visible = false;

            int selected = chunkShapePresetSelector.getSelected();
            ClientConfig.NEW_FRONTIER_CHUNK_SHAPE.set(selected);

            labelCount.visible = false;
            textCount.visible = false;
            labelCountInfo.visible = true;
            setLabelCountInfoMessage(CHUNKS_KEY, chunkShapePresetSelector.getChunkCount());

            labelSizeInfo.visible = selected == 7;

            if (selected == 0 || selected == 1 || selected == 7) {
                labelSize.visible = false;
                textSize.visible = false;
                repositionElements();
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Width) {
                setLabelSizeMessage("mapfrontiers.shape_width");
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH);
                chunkShapePresetSelector.setSize(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get());
            } else if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Length) {
                setLabelSizeMessage("mapfrontiers.shape_length");
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH);
                chunkShapePresetSelector.setSize(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get());
            }
        } else {
            vertexShapePresetSelector.visible = false;
            chunkShapePresetSelector.visible = false;
            pathShapePresetSelector.visible = true;

            int selected = pathShapePresetSelector.getSelected();
            ClientConfig.NEW_FRONTIER_PATH_SHAPE.set(selected);
            setLabelCountMessage(POINT_COUNT_LABEL);

            labelCount.visible = false;
            textCount.visible = false;
            labelCountInfo.visible = true;
            setLabelCountInfoMessage(POINTS_KEY, pathShapePresetSelector.getPointCount());

            labelSizeInfo.visible = false;

            if (pathShapePresetSelector.getShapeMeasure() == PathShapePresetSelector.ShapeMeasure.None) {
                labelSize.visible = false;
                textSize.visible = false;
                repositionElements();
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;
            setLabelSizeMessage("mapfrontiers.shape_segment_length");
            setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_PATH_SEGMENT_LENGTH);
        }

        repositionElements();
    }

    private OptionButton createFrontierTypeButton(int width) {
        OptionButton button = new OptionButton(font, width, OptionButton.DO_NOTHING);
        button.addOption(GLOBAL_LABEL);
        button.addOption(PERSONAL_LABEL);
        button.setSelected(FrontierTypeOption.GLOBAL.ordinal());

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        boolean canCreateGlobal = MapFrontiersClient.isModOnServer()
                && profile != null
                && profile.createFrontier == SettingsProfile.State.Enabled;
        if (!canCreateGlobal) {
            button.setSelected(FrontierTypeOption.PERSONAL.ordinal());
            button.active = false;
        }
        if (forcedPersonal != null) {
            button.setSelected(forcedPersonal ? FrontierTypeOption.PERSONAL.ordinal() : FrontierTypeOption.GLOBAL.ordinal());
            button.active = false;
        }
        if (frontierLifetime == TerritoryLifetime.SESSION_ONLY) {
            button.setSelected(FrontierTypeOption.PERSONAL.ordinal());
            button.active = false;
        }

        return button;
    }

    private boolean resolvePersonalSelection() {
        if (frontierLifetime == TerritoryLifetime.SESSION_ONLY) {
            return true;
        }
        if (forcedPersonal != null) {
            return forcedPersonal;
        }
        return buttonFrontierType.getSelected() == FrontierTypeOption.PERSONAL.ordinal();
    }

    private boolean shouldShowContextualHint() {
        return collectionId != null || forcedPersonal != null || frontierLifetime == TerritoryLifetime.SESSION_ONLY;
    }

    private Component createContextualTypeHint(boolean personal) {
        if (collectionId != null) {
            Component collectionComponent = resolveCollectionNameComponent();
            if (frontierLifetime == TerritoryLifetime.SESSION_ONLY) {
                return Component.translatable(CONTEXTUAL_HINT_TEMPORARY_IN_COLLECTION_KEY, collectionComponent);
            }

            Component typeComponent = personal ? PERSONAL_LABEL : GLOBAL_LABEL;
            return Component.translatable(CONTEXTUAL_HINT_IN_COLLECTION_KEY, typeComponent, collectionComponent);
        }

        if (frontierLifetime == TerritoryLifetime.SESSION_ONLY) {
            return Component.translatable(CONTEXTUAL_HINT_TEMPORARY_KEY);
        }

        Component typeComponent = personal ? PERSONAL_LABEL : GLOBAL_LABEL;
        return Component.translatable(CONTEXTUAL_HINT_KEY, typeComponent);
    }

    private Component resolveCollectionNameComponent() {
        if (collectionId == null) {
            return Component.empty();
        }

        var collection = MapFrontiersClient.getCollection(collectionId);
        if (collection == null || StringUtil.isBlank(collection.getName())) {
            MutableComponent unnamed = Component.translatable("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            unnamed.withStyle(style -> style.withItalic(true));
            return unnamed;
        }

        return Component.literal(collection.getName().trim());
    }

    private void setLabelCountMessage(Component message) {
        labelCount.setMessage(message);
        labelCount.setWidth(font.width(message));
    }

    private void setLabelSizeMessage(String key) {
        labelSize.setMessage(Component.translatable(key));
        labelSize.setWidth(font.width(labelSize.getMessage()));
    }

    private void setLabelCountInfoMessage(String key, int count) {
        labelCountInfo.setMessage(Component.translatable(key, count));
        labelCountInfo.setWidth(font.width(labelCountInfo.getMessage()));
    }

    private IntConfigEntry activeSizeConfigEntry() {
        if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Vertex) {
            if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Width) {
                return ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_WIDTH;
            } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
                return ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_RADIUS;
            }
        } else if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Chunk) {
            if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Width) {
                return ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH;
            } else if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Length) {
                return ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH;
            }
        } else if (pathShapePresetSelector.getShapeMeasure() == PathShapePresetSelector.ShapeMeasure.Length) {
            return ClientConfig.NEW_FRONTIER_PATH_SEGMENT_LENGTH;
        }

        return null;
    }

    private @Nullable FrontierCreateSpec createFrontierSpec(boolean personal, ResourceKey<Level> dimension) {
        if (minecraft.player == null) {
            return null;
        }

        FrontierData defaults = new FrontierData();
        SettingsUser owner = new SettingsUser(minecraft.player);
        UUID frontierId = UUID.randomUUID();
        FrontierVisibilityData visibility = new FrontierVisibilityData(defaults.getVisibilityData());
        BannerData banner = defaults.getBannerData() == null ? null : new BannerData(defaults.getBannerData());
        FrontierData.PathStyle pathStyle = ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Path
                ? ClientConfig.getDefaultPathStyle()
                : defaults.getPathStyle();
        int color = ColorHelper.getRandomColor();
        TerritoryLifetime lifetime = frontierLifetime == null ? TerritoryLifetime.PERSISTENT : frontierLifetime;

        if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Path) {
            return FrontierCreateSpec.path(frontierId, owner, personal, dimension, lifetime, collectionId,
                    null, defaults.getName1(), defaults.getName2(), color, visibility, banner, calculatePoints(), pathStyle);
        }

        if (ClientConfig.NEW_FRONTIER_SHAPE.get() == FrontierShape.Chunk) {
            return FrontierCreateSpec.chunk(frontierId, owner, personal, dimension, lifetime, collectionId,
                    null, defaults.getName1(), defaults.getName2(), color, visibility, banner, new LinkedHashSet<>(calculateChunks()),
                    pathStyle);
        }

        return FrontierCreateSpec.vertex(frontierId, owner, personal, dimension, lifetime, collectionId,
                null, defaults.getName1(), defaults.getName2(), color, visibility, banner, calculateVertices(), pathStyle);
    }

    private void setSizeTextBoxValue(IntConfigEntry entry) {
        textSize.setRange(entry);
        textSize.setValue(String.valueOf(entry.get()));
    }

    private List<BlockPos> calculateVertices() {
        if (ClientConfig.NEW_FRONTIER_SHAPE.get() != FrontierShape.Vertex) {
            return null;
        }

        List<Vec2> shapeVertices;
        if (vertexShapePresetSelector.getSelected() == 11) {
            shapeVertices = vertexShapePresetSelector.getVertices(ClientConfig.NEW_FRONTIER_VERTEX_COUNT.get());
        } else {
            shapeVertices = vertexShapePresetSelector.getVertices();
        }

        if (shapeVertices == null) {
            return new ArrayList<>();
        }

        double radius = 0.0;

        if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Width) {
            radius = ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_WIDTH.get();
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
            radius = ClientConfig.NEW_FRONTIER_VERTEX_SHAPE_RADIUS.get();
            if (radius < 1) {
                radius = 1;
            }
        }

        Set<BlockPos> polygonVertices = new LinkedHashSet<>();

        for (Vec2 vertex : shapeVertices) {
            int x = round(vertex.x * radius) + centerPos.getX();
            int z = round(vertex.y * radius) + centerPos.getZ();
            polygonVertices.add(new BlockPos(x, 70, z));
        }

        return new ArrayList<>(polygonVertices);
    }

    private static int round(double value) {
        if (Math.abs(value - Math.floor(value) - 0.5) < 0.001) {
            return (int) Math.ceil(value);
        } else {
            return (int) Math.round(value);
        }
    }

    private List<ChunkPos> calculateChunks() {
        if (ClientConfig.NEW_FRONTIER_SHAPE.get() != FrontierShape.Chunk) {
            return null;
        }

        List<ChunkPos> chunks = new ArrayList<>();
        ChunkPos playerChunk = ChunkPos.containing(centerPos);
        int selected = chunkShapePresetSelector.getSelected();

        if (selected == 1) {
            chunks.add(playerChunk);
        } else if (selected == 2) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x() - shapeWidth / 2, playerChunk.z() - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                chunks.add(new ChunkPos(start.x() + (i % shapeWidth), start.z() + i / shapeWidth));
            }
        } else if (selected == 3) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x() - shapeWidth / 2, playerChunk.z() - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                if (i < shapeWidth || i >= shapeWidth * (shapeWidth - 1) || (i % shapeWidth) == 0 || (i % shapeWidth) == shapeWidth - 1) {
                    chunks.add(new ChunkPos(start.x() + (i % shapeWidth), start.z() + i / shapeWidth));
                }
            }
        } else if (selected == 4) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x() - shapeWidth / 2, playerChunk.z() - shapeWidth / 2);
            for (int z = start.z(); z < start.z() + shapeWidth; ++z) {
                for (int x = start.x(); x < start.x() + shapeWidth; ++x) {
                    int deltaX = x - playerChunk.x();
                    int deltaZ = z - playerChunk.z();
                    if (shapeWidth % 2 == 0) {
                        deltaX += deltaX < 0 ? 1 : 0;
                        deltaZ += deltaZ < 0 ? 1 : 0;
                    }
                    if (Math.abs(deltaX) + Math.abs(deltaZ) <= (shapeWidth - 1) / 2) {
                        chunks.add(new ChunkPos(x, z));
                    }
                }
            }
        } else if (selected == 5) {
            int shapeLength = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get();
            int start = playerChunk.x() - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(start + i, playerChunk.z()));
            }
        } else if (selected == 6) {
            int shapeLength = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get();
            int start = playerChunk.z() - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(playerChunk.x(), start + i));
            }
        } else if (selected == 7) {
            ChunkPos start = new ChunkPos(Math.floorDiv(playerChunk.x(), 32) * 32, Math.floorDiv(playerChunk.z(), 32) * 32);
            for (int z = 0; z < 32; ++z) {
                for (int x = 0; x < 32; ++x) {
                    chunks.add(new ChunkPos(start.x() + x, start.z() + z));
                }
            }
        }

        return chunks;
    }

    private List<BlockPos> calculatePoints() {
        if (ClientConfig.NEW_FRONTIER_SHAPE.get() != FrontierShape.Path) {
            return null;
        }

        int selected = pathShapePresetSelector.getSelected();
        int length = Math.max(1, ClientConfig.NEW_FRONTIER_PATH_SEGMENT_LENGTH.get());
        List<BlockPos> points = new ArrayList<>();
        BlockPos start = centerPos.atY(70);

        points.add(start);

        if (selected == 0) {
            points.clear();
            return points;
        }

        if (selected == 1) {
            return points;
        }

        if (selected == 2) {
            points.clear();
            points.add(start.offset(-length, 0, 0));
            points.add(start);
            points.add(start.offset(length, 0, 0));
            return points;
        }

        if (selected == 3) {
            points.clear();
            points.add(start.offset(0, 0, -length));
            points.add(start);
            points.add(start.offset(0, 0, length));
            return points;
        }

        BlockPos end = switch (selected) {
            case 4 -> start.offset(0, 0, -length);
            case 5 -> start.offset(length, 0, 0);
            case 6 -> start.offset(0, 0, length);
            case 7 -> start.offset(-length, 0, 0);
            default -> start;
        };
        points.add(end);
        return points;
    }
}
