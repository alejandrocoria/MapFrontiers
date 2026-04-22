package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
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
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class NewFrontierDialog extends PanelDialog {
    private static final Component FRONTIER_TYPE_LABEL = Component.translatable("mapfrontiers.frontier_type");
    private static final Component FRONTIER_MODE_LABEL = Component.translatable("mapfrontiers.frontier_mode");
    private static final Component AFTER_CREATING_LABEL = Component.translatable("mapfrontiers.after_creating");
    private static final Component VERTEX_COUNT_LABEL = Component.translatable("mapfrontiers.shape_vertex_count");
    private static final Component POINT_COUNT_LABEL = Component.translatable("mapfrontiers.shape_point_count");
    private static final Component SIZE_INFO_LABEL = Component.translatable("mapfrontiers.shape_size_info");
    private static final String VERTICES_KEY = "mapfrontiers.vertices";
    private static final String POINTS_KEY = "mapfrontiers.points";
    private static final String CHUNKS_KEY = "mapfrontiers.chunks";
    private static final Component CREATE_LABEL = Component.translatable("mapfrontiers.create");

    private final IClientAPI jmAPI;
    private final BlockPos centerPos;

    private OptionButton buttonFrontierType;
    private OptionButton buttonFrontierMode;
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

    public NewFrontierDialog(IClientAPI jmAPI, BlockPos centerPos) {
        super();
        this.jmAPI = jmAPI;
        this.centerPos = centerPos;

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            onClose();
            new NewFrontierDialog(jmAPI, centerPos).display();
        });
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);
        LayoutSettings leftColumnSettings = LayoutSettings.defaults().alignHorizontallyRight();
        LayoutSettings rightColumnSettings = LayoutSettings.defaults().alignHorizontallyLeft();
        LayoutSettings centerColumnSettings = LayoutSettings.defaults().alignHorizontallyCenter();

        mainLayout.addChild(new StringWidget(FRONTIER_TYPE_LABEL, font).setColor(ColorConstants.TEXT), 0, 0, leftColumnSettings);
        buttonFrontierType = new OptionButton(font, 130, OptionButton.DO_NOTHING);
        buttonFrontierType.addOption(ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierType.Global));
        buttonFrontierType.addOption(ClientConfig.getTranslatedEnum(ClientConfig.FilterFrontierType.Personal));
        buttonFrontierType.setSelected(0);
        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        boolean canCreateGlobal = MapFrontiersClient.isModOnServer()
                && profile != null
                && profile.createFrontier == SettingsProfile.State.Enabled;
        if (!canCreateGlobal) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }
        mainLayout.addChild(buttonFrontierType, 0, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(FRONTIER_MODE_LABEL, font).setColor(ColorConstants.TEXT), 1, 0, leftColumnSettings);
        buttonFrontierMode = new OptionButton(font, 130, (b) -> {
                    ClientConfig.NEW_FRONTIER_MODE.set(FrontierData.Mode.VALUES[b.getSelected()]);
                    shapePresetUpdated();
        });
        buttonFrontierMode.addOption(ClientConfig.getTranslatedEnum(FrontierData.Mode.Vertex));
        buttonFrontierMode.addOption(ClientConfig.getTranslatedEnum(FrontierData.Mode.Chunk));
        buttonFrontierMode.addOption(ClientConfig.getTranslatedEnum(FrontierData.Mode.Path));
        buttonFrontierMode.setSelected(ClientConfig.NEW_FRONTIER_MODE.get().ordinal());
        mainLayout.addChild(buttonFrontierMode, 1, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(AFTER_CREATING_LABEL, font).setColor(ColorConstants.TEXT), 2, 0, leftColumnSettings);
        buttonAfterCreate = new OptionButton(font, 130,
                (b) -> ClientConfig.AFTER_CREATING_FRONTIER.set(ClientConfig.AfterCreatingFrontier.values()[b.getSelected()]));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.InfoScreen));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.EditShape));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.DoNothing));
        buttonAfterCreate.setSelected(ClientConfig.AFTER_CREATING_FRONTIER.get().ordinal());
        mainLayout.addChild(buttonAfterCreate, 2, 1, rightColumnSettings);

        vertexShapePresetSelector = new VertexShapePresetSelector(font, ClientConfig.NEW_FRONTIER_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(vertexShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);
        chunkShapePresetSelector = new ChunkShapePresetSelector(font, ClientConfig.NEW_FRONTIER_CHUNK_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(chunkShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);
        pathShapePresetSelector = new PathShapePresetSelector(font, ClientConfig.NEW_FRONTIER_PATH_SHAPE.get(), (s) -> shapePresetUpdated());
        mainLayout.addChild(pathShapePresetSelector, 3, 0, 1, 2, centerColumnSettings);

        labelCount = mainLayout.addChild(new StringWidget(VERTEX_COUNT_LABEL, font).setColor(ColorConstants.WHITE), 4, 0, leftColumnSettings);
        textCount = new TextBoxInt(ClientConfig.NEW_FRONTIER_COUNT, font, 64);
        textCount.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_COUNT.get()));
        textCount.setValueChangedCallback(ClientConfig.NEW_FRONTIER_COUNT::set);
        mainLayout.addChild(textCount, 4, 1, rightColumnSettings);

        labelCountInfo = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE), 4, 0, 1, 2, centerColumnSettings);

        labelSize = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE), 5, 0, leftColumnSettings);
        textSize = new TextBoxInt(ClientConfig.NEW_FRONTIER_PATH_SEGMENT_LENGTH, font, 64);
        textSize.setValueChangedCallback(value -> {
            IntConfigEntry entry = activeSizeConfigEntry();
            if (entry != null) {
                entry.set(value);
            }

            if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Chunk) {
                if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Width) {
                    chunkShapePresetSelector.setSize(value);
                } else if (chunkShapePresetSelector.getShapeMeasure() == ChunkShapePresetSelector.ShapeMeasure.Length) {
                    chunkShapePresetSelector.setSize(value);
                }
            }
        });
        mainLayout.addChild(textSize, 5, 1, rightColumnSettings);

        labelSizeInfo = mainLayout.addChild(new StringWidget(SIZE_INFO_LABEL, font).setColor(ColorConstants.WHITE), 5, 0, 1, 2, centerColumnSettings);

        addConfirmButton(CREATE_LABEL, (b) -> {
            boolean personal = buttonFrontierType.getSelected() == 1;
            closeAndReturnToFullscreenMap();
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState != null) {
                FrontierData.Mode mode = ClientConfig.NEW_FRONTIER_MODE.get();
                FrontierData.PathStyle pathStyle = mode == FrontierData.Mode.Path ? ClientConfig.getDefaultPathStyle() : null;
                MapFrontiersClient.getOperationService().createNewFrontier(personal, uiState.dimension,
                        calculateVertices(), calculateChunks(), calculatePoints(), pathStyle);
            }
        });
        addCancelButton();

        shapePresetUpdated();
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private void shapePresetUpdated() {
        if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Vertex) {
            vertexShapePresetSelector.visible = true;
            chunkShapePresetSelector.visible = false;
            pathShapePresetSelector.visible = false;

            int selected = vertexShapePresetSelector.getSelected();
            ClientConfig.NEW_FRONTIER_SHAPE.set(selected);
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
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_SHAPE_WIDTH);
            } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
                setLabelSizeMessage("mapfrontiers.shape_radius");
                setSizeTextBoxValue(ClientConfig.NEW_FRONTIER_SHAPE_RADIUS);
            }
        } else if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Chunk) {
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
        if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Vertex) {
            if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Width) {
                return ClientConfig.NEW_FRONTIER_SHAPE_WIDTH;
            } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
                return ClientConfig.NEW_FRONTIER_SHAPE_RADIUS;
            }
        } else if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Chunk) {
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

    private void setSizeTextBoxValue(IntConfigEntry entry) {
        textSize.setRange(entry);
        textSize.setValue(String.valueOf(entry.get()));
    }

    private List<BlockPos> calculateVertices() {
        if (ClientConfig.NEW_FRONTIER_MODE.get() != FrontierData.Mode.Vertex) {
            return null;
        }

        List<Vec2> shapeVertices;
        if (vertexShapePresetSelector.getSelected() == 11) {
            shapeVertices = vertexShapePresetSelector.getVertices(ClientConfig.NEW_FRONTIER_COUNT.get());
        } else {
            shapeVertices = vertexShapePresetSelector.getVertices();
        }

        if (shapeVertices == null) {
            return new ArrayList<>();
        }

        double radius = 0.0;

        if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Width) {
            radius = ClientConfig.NEW_FRONTIER_SHAPE_WIDTH.get();
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (vertexShapePresetSelector.getShapeMeasure() == VertexShapePresetSelector.ShapeMeasure.Radius) {
            radius = ClientConfig.NEW_FRONTIER_SHAPE_RADIUS.get();
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
        if (ClientConfig.NEW_FRONTIER_MODE.get() != FrontierData.Mode.Chunk) {
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
        if (ClientConfig.NEW_FRONTIER_MODE.get() != FrontierData.Mode.Path) {
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
