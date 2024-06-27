package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeChunkButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ShapeVertexButtons;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
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
public class NewFrontier extends StackeableScreen {
    private final IClientAPI jmAPI;

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

    private OptionButton buttonFrontierType;
    private OptionButton buttonFrontierMode;
    private OptionButton buttonAfterCreate;
    private ShapeVertexButtons shapeVertexButtons;
    private ShapeChunkButtons shapeChunkButtons;
    private SimpleLabel labelSize;
    private TextBoxInt textSize;
    private SimpleButton buttonCreateFrontier;
    private SimpleButton buttonCancel;

    private final List<SimpleLabel> labels;

    public NewFrontier(IClientAPI jmAPI, Screen returnScreen) {
        super(Component.translatable("mapfrontiers.title_new_frontier"), returnScreen);
        this.jmAPI = jmAPI;
        labels = new ArrayList<>();

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            StackeableScreen.popAndOpen(new NewFrontier(jmAPI, returnScreen));
        });
    }

    @Override
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, 344, 279);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        labels.clear();

        labels.add(new SimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 105, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.frontier_type"), ColorConstants.TEXT));
        buttonFrontierType = new OptionButton(font, actualWidth / 2, actualHeight / 2 - 107, 130, this::buttonPressed);
        buttonFrontierType.addOption(Config.getTranslatedEnum(Config.FilterFrontierType.Global));
        buttonFrontierType.addOption(Config.getTranslatedEnum(Config.FilterFrontierType.Personal));
        buttonFrontierType.setSelected(0);

        if (!MapFrontiersClient.isModOnServer() || MapFrontiersClient.getSettingsProfile().createFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }

        labels.add(new SimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 89, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.frontier_mode"), ColorConstants.TEXT));
        buttonFrontierMode = new OptionButton(font, actualWidth / 2, actualHeight / 2 - 91, 130, this::buttonPressed);
        buttonFrontierMode.addOption(Config.getTranslatedEnum(FrontierData.Mode.Vertex));
        buttonFrontierMode.addOption(Config.getTranslatedEnum(FrontierData.Mode.Chunk));
        buttonFrontierMode.setSelected(Config.newFrontierMode.ordinal());

        labels.add(new SimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 73, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.after_creating"), ColorConstants.TEXT));
        buttonAfterCreate = new OptionButton(font, actualWidth / 2, actualHeight / 2 - 75, 130, this::buttonPressed);
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Info));
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Edit));
        buttonAfterCreate.addOption(Config.getTranslatedEnum(Config.AfterCreatingFrontier.Nothing));
        buttonAfterCreate.setSelected(Config.afterCreatingFrontier.ordinal());

        shapeVertexButtons = new ShapeVertexButtons(font, actualWidth / 2 - 162, actualHeight / 2 - 45, Config.newFrontierShape, (s) -> shapeButtonsUpdated());
        shapeChunkButtons = new ShapeChunkButtons(font, actualWidth / 2 - 107, actualHeight / 2 - 45, Config.newFrontierChunkShape, (s) -> shapeButtonsUpdated());

        labelSize = new SimpleLabel(font, actualWidth / 2 - 80, actualHeight / 2 + 98, SimpleLabel.Align.Left, Component.literal(""), ColorConstants.WHITE);
        labels.add(labelSize);
        textSize = new TextBoxInt(1, 1, 999, font, actualWidth / 2 + 16, actualHeight / 2 + 96, 64);
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

        buttonCreateFrontier = new SimpleButton(font, actualWidth / 2 - 110, actualHeight / 2 + 123, 100,
                Component.translatable("mapfrontiers.create"), this::buttonPressed);
        buttonCancel = new SimpleButton(font, actualWidth / 2 + 10, actualHeight / 2 + 123, 100,
                Component.translatable("gui.cancel"), this::buttonPressed);

        addRenderableWidget(buttonFrontierType);
        addRenderableWidget(buttonFrontierMode);
        addRenderableWidget(buttonAfterCreate);
        addRenderableWidget(shapeVertexButtons);
        addRenderableWidget(shapeChunkButtons);
        addRenderableWidget(textSize);
        addRenderableWidget(buttonCreateFrontier);
        addRenderableWidget(buttonCancel);

        shapeButtonsUpdated();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            graphics.pose().pushPose();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        int x1 = actualWidth / 2 - 172;
        int x2 = actualWidth / 2 + 172;
        int y1 = actualHeight / 2 - 117;
        int y2 = actualHeight / 2 + 117;
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_BG);
        graphics.hLine(x1, x2, y1, ColorConstants.TAB_BORDER);
        graphics.hLine(x1, x2, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x1, y1, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x2, y1, y2, ColorConstants.TAB_BORDER);

        // Rendering manually so the background is not scaled.
        for(GuiEventListener child : children()) {
            if (child instanceof Renderable renderable)
                renderable.render(graphics, mouseX, mouseY, partialTicks);
        }

        graphics.drawCenteredString(font, title, this.actualWidth / 2, 8, ColorConstants.WHITE);

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        if (scaleFactor != 1.f) {
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (mouseButton == 0) {
            textSize.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX * scaleFactor, mouseY * scaleFactor, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, hDelta, vDelta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonFrontierMode) {
            Config.newFrontierMode = FrontierData.Mode.values()[buttonFrontierMode.getSelected()];
            shapeButtonsUpdated();
        } else if (button == buttonAfterCreate) {
            Config.afterCreatingFrontier = Config.AfterCreatingFrontier.values()[buttonAfterCreate.getSelected()];
        } else if (button == buttonCreateFrontier) {
            boolean personal = buttonFrontierType.getSelected() == 1;
            closeAndReturnUntil(IFullscreen.class);
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState != null) {
                MapFrontiersClient.getFrontiersOverlayManager(personal).clientCreateNewfrontier(uiState.dimension, calculateVertices(), calculateChunks());
            }
        } else if (button == buttonCancel) {
            closeAndReturn();
        }
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
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Width) {
                labelSize.setText(Component.translatable("mapfrontiers.shape_width"));
                textSize.setValue(String.valueOf(Config.newFrontierShapeWidth));
            } else if (shapeVertexButtons.getShapeMeasure() == ShapeVertexButtons.ShapeMeasure.Radius) {
                labelSize.setText(Component.translatable("mapfrontiers.shape_radius"));
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
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Width) {
                labelSize.setText(Component.translatable("mapfrontiers.shape_width"));
                textSize.setValue(String.valueOf(Config.newFrontierChunkShapeWidth));
                shapeChunkButtons.setSize(Config.newFrontierChunkShapeWidth);
            } else if (shapeChunkButtons.getShapeMeasure() == ShapeChunkButtons.ShapeMeasure.Length) {
                labelSize.setText(Component.translatable("mapfrontiers.shape_length"));
                textSize.setValue(String.valueOf(Config.newFrontierChunkShapeLength));
                shapeChunkButtons.setSize(Config.newFrontierChunkShapeLength);
            }
        }
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
