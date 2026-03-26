package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeChunkButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeVertexButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.gui.GuiGraphics;
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
public class NewFrontier extends AutoScaledScreen {
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_new_frontier");
    private static final Component frontierTypeLabel = Component.translatable("mapfrontiers.frontier_type");
    private static final Component frontierModeLabel = Component.translatable("mapfrontiers.frontier_mode");
    private static final Component afterCreatingLabel = Component.translatable("mapfrontiers.after_creating");
    private static final Component vertexCountLabel = Component.translatable("mapfrontiers.shape_vertex_count");
    private static final Component sizeInfoLabel = Component.translatable("mapfrontiers.shape_size_info");
    private static final String verticesKey = "mapfrontiers.vertices";
    private static final String chunksKey = "mapfrontiers.chunks";
    private static final Component createLabel = Component.translatable("mapfrontiers.create");
    private static final Component cancelLabel = Component.translatable("gui.cancel");

    private final IClientAPI jmAPI;

    private OptionButton buttonFrontierType;
    private OptionButton buttonFrontierMode;
    private OptionButton buttonAfterCreate;
    private ShapeVertexButtons shapeVertexButtons;
    private ShapeChunkButtons shapeChunkButtons;
    private StringWidget labelCount;
    private StringWidget labelCountInfo;
    private TextBoxInt textCount;
    private StringWidget labelSize;
    private StringWidget labelSizeInfo;
    private TextBoxInt textSize;
    private BlockPos centerPos;

    public NewFrontier(IClientAPI jmAPI, BlockPos centerPos) {
        super(titleLabel, 344, 295);
        this.jmAPI = jmAPI;
        this.centerPos = centerPos;

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            onClose();
            new NewFrontier(jmAPI, centerPos).display();
        });
    }

    @Override
    public void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(8);
        content.addChild(mainLayout);
        LayoutSettings leftColumnSettings = LayoutSettings.defaults().alignHorizontallyRight();
        LayoutSettings rightColumnSettings = LayoutSettings.defaults().alignHorizontallyLeft();
        LayoutSettings centerColumnSettings = LayoutSettings.defaults().alignHorizontallyCenter();

        mainLayout.addChild(new StringWidget(frontierTypeLabel, font).setColor(ColorConstants.TEXT), 0, 0, leftColumnSettings);
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

        mainLayout.addChild(new StringWidget(frontierModeLabel, font).setColor(ColorConstants.TEXT), 1, 0, leftColumnSettings);
        buttonFrontierMode = new OptionButton(font, 130, (b) -> {
                    ClientConfig.NEW_FRONTIER_MODE.set(FrontierData.Mode.values()[b.getSelected()]);
                    shapeButtonsUpdated();
        });
        buttonFrontierMode.addOption(ClientConfig.getTranslatedEnum(FrontierData.Mode.Vertex));
        buttonFrontierMode.addOption(ClientConfig.getTranslatedEnum(FrontierData.Mode.Chunk));
        buttonFrontierMode.setSelected(ClientConfig.NEW_FRONTIER_MODE.get().ordinal());
        mainLayout.addChild(buttonFrontierMode, 1, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(afterCreatingLabel, font).setColor(ColorConstants.TEXT), 2, 0, leftColumnSettings);
        buttonAfterCreate = new OptionButton(font, 130,
                (b) -> ClientConfig.AFTER_CREATING_FRONTIER.set(ClientConfig.AfterCreatingFrontier.values()[b.getSelected()]));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.Info));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.Edit));
        buttonAfterCreate.addOption(ClientConfig.getTranslatedEnum(ClientConfig.AfterCreatingFrontier.Nothing));
        buttonAfterCreate.setSelected(ClientConfig.AFTER_CREATING_FRONTIER.get().ordinal());
        mainLayout.addChild(buttonAfterCreate, 2, 1, rightColumnSettings);

        shapeVertexButtons = new ShapeVertexButtons(font, ClientConfig.NEW_FRONTIER_SHAPE.get(), (s) -> shapeButtonsUpdated());
        mainLayout.addChild(shapeVertexButtons, 3, 0, 1, 2, centerColumnSettings);
        shapeChunkButtons = new ShapeChunkButtons(font, ClientConfig.NEW_FRONTIER_CHUNK_SHAPE.get(), (s) -> shapeButtonsUpdated());
        mainLayout.addChild(shapeChunkButtons, 3, 0, 1, 2, centerColumnSettings);

        labelCount = mainLayout.addChild(new StringWidget(vertexCountLabel, font).setColor(ColorConstants.WHITE), 4, 0, leftColumnSettings);
        textCount = new TextBoxInt(ClientConfig.NEW_FRONTIER_COUNT.get(), 1, 999, font, 64);
        textCount.setValueChangedCallback(value -> {
            if (ClientConfig.NEW_FRONTIER_COUNT.isInRange(value)) {
                ClientConfig.NEW_FRONTIER_COUNT.set(value);
            }
        });
        mainLayout.addChild(textCount, 4, 1, rightColumnSettings);

        labelCountInfo = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE), 4, 0, 1, 2, centerColumnSettings);

        labelSize = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE), 5, 0, leftColumnSettings);
        textSize = new TextBoxInt(1, 1, 999, font, 64);
        textSize.setValueChangedCallback(value -> {
            if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Vertex) {
                if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
                    if (ClientConfig.NEW_FRONTIER_SHAPE_WIDTH.isInRange(value)) {
                        ClientConfig.NEW_FRONTIER_SHAPE_WIDTH.set(value);
                    }
                } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
                    if (ClientConfig.NEW_FRONTIER_SHAPE_RADIUS.isInRange(value)) {
                        ClientConfig.NEW_FRONTIER_SHAPE_RADIUS.set(value);
                    }
                }
            } else {
                if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Width) {
                    if (ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.isInRange(value)) {
                        ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.set(value);
                        shapeChunkButtons.setSize(value);
                    }
                } else if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Length) {
                    if (ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.isInRange(value)) {
                        ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.set(value);
                        shapeChunkButtons.setSize(value);
                    }
                }
            }
        });
        mainLayout.addChild(textSize, 5, 1, rightColumnSettings);

        labelSizeInfo = mainLayout.addChild(new StringWidget(sizeInfoLabel, font).setColor(ColorConstants.WHITE), 5, 0, 1, 2, centerColumnSettings);

        bottomButtons.addChild(new SimpleButton(font, 100, createLabel, (b) -> {
            boolean personal = buttonFrontierType.getSelected() == 1;
            closeAndReturnToFullscreenMap();
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState != null) {
                MapFrontiersClient.getOperationService().createNewFrontier(personal, uiState.dimension, calculateVertices(), calculateChunks());
            }
        }));
        bottomButtons.addChild(new SimpleButton(font, 100, cancelLabel, b -> onClose()));

        shapeButtonsUpdated();
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, 344, 234);
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private void shapeButtonsUpdated() {
        if (ClientConfig.NEW_FRONTIER_MODE.get() == FrontierData.Mode.Vertex) {
            shapeVertexButtons.visible = true;
            shapeChunkButtons.visible = false;

            int selected = shapeVertexButtons.getSelected();
            ClientConfig.NEW_FRONTIER_SHAPE.set(selected);

            if (selected == 11) {
                labelCount.visible = true;
                textCount.visible = true;
                labelCountInfo.visible = false;
            } else {
                labelCount.visible = false;
                textCount.visible = false;
                labelCountInfo.visible = true;
                setLabelCountInfoMessage(verticesKey, shapeVertexButtons.getVertexCount());
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

            if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
                setLabelSizeMessage("mapfrontiers.shape_width");
                textSize.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_SHAPE_WIDTH.get()));
            } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
                setLabelSizeMessage("mapfrontiers.shape_radius");
                textSize.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_SHAPE_RADIUS.get()));
            }
        } else {
            shapeVertexButtons.visible = false;
            shapeChunkButtons.visible = true;

            int selected = shapeChunkButtons.getSelected();
            ClientConfig.NEW_FRONTIER_CHUNK_SHAPE.set(selected);

            labelCount.visible = false;
            textCount.visible = false;
            labelCountInfo.visible = true;
            setLabelCountInfoMessage(chunksKey, shapeChunkButtons.getChunkCount());

            labelSizeInfo.visible = selected == 7;

            if (selected == 0 || selected == 1 || selected == 7) {
                labelSize.visible = false;
                textSize.visible = false;
                repositionElements();
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Width) {
                setLabelSizeMessage("mapfrontiers.shape_width");
                textSize.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get()));
                shapeChunkButtons.setSize(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get());
            } else if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Length) {
                setLabelSizeMessage("mapfrontiers.shape_length");
                textSize.setValue(String.valueOf(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get()));
                shapeChunkButtons.setSize(ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get());
            }
        }

        repositionElements();
    }

    private void setLabelSizeMessage(String key) {
        labelSize.setMessage(Component.translatable(key));
        labelSize.setWidth(font.width(labelSize.getMessage()));
    }

    private void setLabelCountInfoMessage(String key, int count) {
        labelCountInfo.setMessage(Component.translatable(key, count));
        labelCountInfo.setWidth(font.width(labelCountInfo.getMessage()));
    }

    private List<BlockPos> calculateVertices() {
        if (ClientConfig.NEW_FRONTIER_MODE.get() != FrontierData.Mode.Vertex) {
            return null;
        }

        List<Vec2> shapeVertices;
        if (shapeVertexButtons.getSelected() == 11) {
            shapeVertices = shapeVertexButtons.getVertices(ClientConfig.NEW_FRONTIER_COUNT.get());
        } else {
            shapeVertices = shapeVertexButtons.getVertices();
        }

        if (shapeVertices == null) {
            return new ArrayList<>();
        }

        double radius = 0.0;

        if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
            radius = ClientConfig.NEW_FRONTIER_SHAPE_WIDTH.get();
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
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
        ChunkPos playerChunk = new ChunkPos(centerPos);
        int selected = shapeChunkButtons.getSelected();

        if (selected == 1) {
            chunks.add(playerChunk);
        } else if (selected == 2) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
            }
        } else if (selected == 3) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                if (i < shapeWidth || i >= shapeWidth * (shapeWidth - 1) || (i % shapeWidth) == 0 || (i % shapeWidth) == shapeWidth - 1) {
                    chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
                }
            }
        } else if (selected == 4) {
            int shapeWidth = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_WIDTH.get();
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int z = start.z; z < start.z + shapeWidth; ++z) {
                for (int x = start.x; x < start.x + shapeWidth; ++x) {
                    int deltaX = x - playerChunk.x;
                    int deltaZ = z - playerChunk.z;
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
            int start = playerChunk.x - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(start + i, playerChunk.z));
            }
        } else if (selected == 6) {
            int shapeLength = ClientConfig.NEW_FRONTIER_CHUNK_SHAPE_LENGTH.get();
            int start = playerChunk.z - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(playerChunk.x, start + i));
            }
        } else if (selected == 7) {
            ChunkPos start = new ChunkPos(Math.floorDiv(playerChunk.x, 32) * 32, Math.floorDiv(playerChunk.z, 32) * 32);
            for (int z = 0; z < 32; ++z) {
                for (int x = 0; x < 32; ++x) {
                    chunks.add(new ChunkPos(start.x + x, start.z + z));
                }
            }
        }

        return chunks;
    }
}
