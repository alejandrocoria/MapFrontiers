package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeChunkButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeVertexButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
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
    private static final Component createLabel = Component.translatable("mapfrontiers.create");
    private static final Component cancelLabel = Component.translatable("gui.cancel");

    private final IClientAPI jmAPI;

    private OptionButton buttonFrontierType;
    private OptionButton buttonFrontierMode;
    private OptionButton buttonAfterCreate;
    private ShapeVertexButtons shapeVertexButtons;
    private ShapeChunkButtons shapeChunkButtons;
    private StringWidget labelSize;
    private TextBoxInt textSize;

    public NewFrontier(IClientAPI jmAPI) {
        super(titleLabel, 344, 295);
        this.jmAPI = jmAPI;

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            onClose();
            new NewFrontier(jmAPI).display();
        });
    }

    @Override
    public void initScreen() {
        GridLayout mainLayout = new GridLayout(2, 5).spacing(8);
        content.addChild(mainLayout);
        LayoutSettings leftColumnSettings = LayoutSettings.defaults().alignHorizontallyRight();
        LayoutSettings rightColumnSettings = LayoutSettings.defaults().alignHorizontallyLeft();
        LayoutSettings centerColumnSettings = LayoutSettings.defaults().alignHorizontallyCenter();

        mainLayout.addChild(new StringWidget(frontierTypeLabel, font).setColor(ColorConstants.TEXT), 0, 0, leftColumnSettings);
        buttonFrontierType = new OptionButton(font, 0, 0, 130, OptionButton.DO_NOTHING);
        buttonFrontierType.addOption(Config.getTranslatedEnum(Config.FilterFrontierType.Global));
        buttonFrontierType.addOption(Config.getTranslatedEnum(Config.FilterFrontierType.Personal));
        buttonFrontierType.setSelected(0);
        if (!MapFrontiersClient.isModOnServer() || MapFrontiersClient.getSettingsProfile().createFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }
        mainLayout.addChild(buttonFrontierType, 0, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(frontierModeLabel, font).setColor(ColorConstants.TEXT), 1, 0, leftColumnSettings);
        buttonFrontierMode = new OptionButton(font, 130, (b) -> {
                    Config.newFrontierMode = FrontierData.Mode.values()[b.getSelected()];
                    shapeButtonsUpdated();
        });
        buttonFrontierMode.addOption(Config.getTranslatedEnum(FrontierData.Mode.Vertex));
        buttonFrontierMode.addOption(Config.getTranslatedEnum(FrontierData.Mode.Chunk));
        buttonFrontierMode.setSelected(Config.newFrontierMode.ordinal());
        mainLayout.addChild(buttonFrontierMode, 1, 1, rightColumnSettings);

        mainLayout.addChild(new StringWidget(afterCreatingLabel, font).setColor(ColorConstants.TEXT), 2, 0, leftColumnSettings);
        buttonAfterCreate = new OptionButton(font, 130,
                (b) -> Config.afterCreatingFrontier = Config.AfterCreatingFrontier.values()[b.getSelected()]);
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Info));
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Edit));
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Nothing));
        buttonAfterCreate.setSelected(Config.afterCreatingFrontier.ordinal());
        mainLayout.addChild(buttonAfterCreate, 2, 1, rightColumnSettings);

        shapeVertexButtons = new ShapeVertexButtons(font, Config.newFrontierShape, (s) -> shapeButtonsUpdated());
        mainLayout.addChild(shapeVertexButtons, 3, 0, 1, 2, centerColumnSettings);
        shapeChunkButtons = new ShapeChunkButtons(font, Config.newFrontierChunkShape, (s) -> shapeButtonsUpdated());
        mainLayout.addChild(shapeChunkButtons, 3, 0, 1, 2, centerColumnSettings);

        labelSize = mainLayout.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE), 4, 0, leftColumnSettings);
        textSize = new TextBoxInt(1, 1, 999, font, 0, 0, 64);
        textSize.setValueChangedCallback(value -> {
            if (Config.newFrontierMode == FrontierData.Mode.Vertex) {
                if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
                    if (Config.isInRange("newFrontierShapeWidth", value)) {
                        Config.newFrontierShapeWidth = value;
                    }
                } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
                    if (Config.isInRange("newFrontierShapeRadius", value)) {
                        Config.newFrontierShapeRadius = value;
                    }
                }
            } else {
                if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Width) {
                    if (Config.isInRange("newFrontierChunkShapeWidth", value)) {
                        Config.newFrontierChunkShapeWidth = value;
                        shapeChunkButtons.setSize(value);
                    }
                } else if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Length) {
                    if (Config.isInRange("newFrontierChunkShapeLength", value)) {
                        Config.newFrontierChunkShapeLength = value;
                        shapeChunkButtons.setSize(value);
                    }
                }
            }
        });
        mainLayout.addChild(textSize, 4, 1, rightColumnSettings);

        bottomButtons.addChild(new SimpleButton(font, 100, createLabel, (b) -> {
            boolean personal = buttonFrontierType.getSelected() == 1;
            closeAndReturnToFullscreenMap();
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState != null) {
                MapFrontiersClient.getFrontiersOverlayManager(personal).clientCreateNewfrontier(uiState.dimension, calculateVertices(), calculateChunks());
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
    public void removed() {
        ClientEventHandler.unsuscribeAllEvents(this);
        ClientEventHandler.postUpdatedConfigEvent();
    }

    private void shapeButtonsUpdated() {
        if (Config.newFrontierMode == FrontierData.Mode.Vertex) {
            shapeVertexButtons.visible = true;
            shapeChunkButtons.visible = false;

            int selected = shapeVertexButtons.getSelected();
            Config.newFrontierShape = selected;

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
                textSize.setValue(String.valueOf(Config.newFrontierShapeWidth));
            } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
                setLabelSizeMessage("mapfrontiers.shape_radius");
                textSize.setValue(String.valueOf(Config.newFrontierShapeRadius));
            }
        } else {
            shapeVertexButtons.visible = false;
            shapeChunkButtons.visible = true;

            int selected = shapeChunkButtons.getSelected();
            Config.newFrontierChunkShape = selected;

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
                textSize.setValue(String.valueOf(Config.newFrontierChunkShapeWidth));
                shapeChunkButtons.setSize(Config.newFrontierChunkShapeWidth);
            } else if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Length) {
                setLabelSizeMessage("mapfrontiers.shape_length");
                textSize.setValue(String.valueOf(Config.newFrontierChunkShapeLength));
                shapeChunkButtons.setSize(Config.newFrontierChunkShapeLength);
            }
        }

        repositionElements();
    }

    private void setLabelSizeMessage(String key) {
        labelSize.setMessage(Component.translatable(key));
        labelSize.setWidth(font.width(labelSize.getMessage()));
    }

    private List<BlockPos> calculateVertices() {
        if (minecraft.player == null || Config.newFrontierMode != FrontierData.Mode.Vertex) {
            return null;
        }

        List<Vec2> shapeVertices = shapeVertexButtons.getVertices();
        if (shapeVertices == null) {
            return new ArrayList<>();
        }

        double radius = 0.0;

        if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
            radius = Config.newFrontierShapeWidth;
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
            radius = Config.newFrontierShapeRadius;
            if (radius < 1) {
                radius = 1;
            }
        }

        Set<BlockPos> polygonVertices = new LinkedHashSet<>();
        BlockPos playerPos = minecraft.player.blockPosition();

        for (Vec2 vertex : shapeVertices) {
            int x = (int) Math.round(vertex.x * radius) + playerPos.getX();
            int z = (int) Math.round(vertex.y * radius) + playerPos.getZ();
            polygonVertices.add(new BlockPos(x, 70, z));
        }

        return new ArrayList<>(polygonVertices);
    }

    private List<ChunkPos> calculateChunks() {
        if (minecraft.player == null || Config.newFrontierMode != FrontierData.Mode.Chunk) {
            return null;
        }

        List<ChunkPos> chunks = new ArrayList<>();
        ChunkPos playerChunk = new ChunkPos(minecraft.player.blockPosition());
        int selected = shapeChunkButtons.getSelected();

        if (selected == 1) {
            chunks.add(playerChunk);
        } else if (selected == 2) {
            int shapeWidth = Config.newFrontierChunkShapeWidth;
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
            }
        } else if (selected == 3) {
            int shapeWidth = Config.newFrontierChunkShapeWidth;
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                if (i < shapeWidth || i >= shapeWidth * (shapeWidth - 1) || (i % shapeWidth) == 0 || (i % shapeWidth) == shapeWidth - 1) {
                    chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
                }
            }
        } else if (selected == 4) {
            int shapeWidth = Config.newFrontierChunkShapeWidth;
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
            int shapeLength = Config.newFrontierChunkShapeLength;
            int start = playerChunk.x - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(start + i, playerChunk.z));
            }
        } else if (selected == 6) {
            int shapeLength = Config.newFrontierChunkShapeLength;
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
