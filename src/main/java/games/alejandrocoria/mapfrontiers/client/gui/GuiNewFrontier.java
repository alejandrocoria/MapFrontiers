package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiNewFrontier extends Screen implements TextIntBox.TextIntBoxResponder {
    private final IClientAPI jmAPI;

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

    private GuiOptionButton buttonFrontierType;
    private GuiOptionButton buttonFrontierMode;
    private GuiOptionButton buttonAfterCreate;
    private GuiVertexShapeButtons vertexShapeButtons;
    private GuiChunkShapeButtons chunkShapeButtons;
    private GuiSimpleLabel labelSize;
    private TextIntBox textSize;
    private GuiSettingsButton buttonCreateFrontier;
    private GuiSettingsButton buttonCancel;

    public GuiNewFrontier(IClientAPI jmAPI) {
        super(TextComponent.EMPTY);
        this.jmAPI = jmAPI;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 338, 338);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        Component title = new TranslatableComponent("mapfrontiers.title_new_frontier");
        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 112, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.frontier_type"), GuiColors.SETTINGS_TEXT));
        buttonFrontierType = new GuiOptionButton(font, actualWidth / 2, actualHeight / 2 - 114, 130, this::buttonPressed);
        buttonFrontierType.addOption(ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Global));
        buttonFrontierType.addOption(ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Personal));
        buttonFrontierType.setSelected(0);

        SettingsProfile profile = ClientProxy.getSettingsProfile();

        if (profile.personalFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(0);
            buttonFrontierType.active = false;
        } else if (profile.createFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }

        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 96, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.frontier_mode"), GuiColors.SETTINGS_TEXT));
        buttonFrontierMode = new GuiOptionButton(font, actualWidth / 2, actualHeight / 2 - 98, 130, this::buttonPressed);
        buttonFrontierMode.addOption(ConfigData.getTranslatedEnum(FrontierData.Mode.Vertex));
        buttonFrontierMode.addOption(ConfigData.getTranslatedEnum(FrontierData.Mode.Chunk));
        buttonFrontierMode.setSelected(ConfigData.newFrontierMode.ordinal());

        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 - 130, actualHeight / 2 - 80, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.after_creating"), GuiColors.SETTINGS_TEXT));
        buttonAfterCreate = new GuiOptionButton(font, actualWidth / 2, actualHeight / 2 - 82, 130, this::buttonPressed);
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Info));
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Edit));
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Nothing));
        buttonAfterCreate.setSelected(ConfigData.afterCreatingFrontier.ordinal());

        vertexShapeButtons = new GuiVertexShapeButtons(font, actualWidth / 2 - 162, actualHeight / 2 - 52, ConfigData.newFrontierShape, (s) -> shapeButtonsUpdated());
        chunkShapeButtons = new GuiChunkShapeButtons(font, actualWidth / 2 - 107, actualHeight / 2 - 52, ConfigData.newFrontierChunkShape, (s) -> shapeButtonsUpdated());

        labelSize = new GuiSimpleLabel(font, actualWidth / 2 - 80, actualHeight / 2 + 90, GuiSimpleLabel.Align.Left, new TextComponent(""), GuiColors.WHITE);
        addRenderableOnly(labelSize);
        textSize = new TextIntBox(1, 1, 999, font, actualWidth / 2 + 16, actualHeight / 2 + 88, 64);
        textSize.setResponder(this);

        buttonCreateFrontier = new GuiSettingsButton(font, actualWidth / 2 - 110, actualHeight / 2 + 146, 100,
                new TranslatableComponent("mapfrontiers.create"), this::buttonPressed);
        buttonCancel = new GuiSettingsButton(font, actualWidth / 2 + 10, actualHeight / 2 + 146, 100,
                new TranslatableComponent("gui.cancel"), this::buttonPressed);

        addRenderableWidget(buttonFrontierType);
        addRenderableWidget(buttonFrontierMode);
        addRenderableWidget(buttonAfterCreate);
        addRenderableWidget(vertexShapeButtons);
        addRenderableWidget(chunkShapeButtons);
        addRenderableWidget(textSize);
        addRenderableWidget(buttonCreateFrontier);
        addRenderableWidget(buttonCancel);

        shapeButtonsUpdated();
    }

    @Override
    public void tick() {
        textSize.tick();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (scaleFactor != 1.f) {
            matrixStack.popPose();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, delta);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonFrontierMode) {
            ConfigData.newFrontierMode = FrontierData.Mode.values()[buttonFrontierMode.getSelected()];
            shapeButtonsUpdated();
        } else if (button == buttonAfterCreate) {
            ConfigData.afterCreatingFrontier = ConfigData.AfterCreatingFrontier.values()[buttonAfterCreate.getSelected()];
        } else if (button == buttonCreateFrontier) {
            boolean personal = buttonFrontierType.getSelected() == 1;
            ClientProxy.getFrontiersOverlayManager(personal).clientCreateNewfrontier(jmAPI.getUIState(Context.UI.Fullscreen).dimension, calculateVertices(), calculateChunks());
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonCancel) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void updatedValue(TextIntBox textIntBox, int value) {
        if (textSize == textIntBox) {
            if (ConfigData.newFrontierMode == FrontierData.Mode.Vertex) {
                if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Width) {
                    if (ConfigData.isInRange("newFrontierShapeWidth", value)) {
                        ConfigData.newFrontierShapeWidth = value;
                    }
                } else if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Radius) {
                    if (ConfigData.isInRange("newFrontierShapeRadius", value)) {
                        ConfigData.newFrontierShapeRadius = value;
                    }
                }
            } else {
                if (chunkShapeButtons.getShapeMeasure() == GuiChunkShapeButtons.ShapeMeasure.Width) {
                    if (ConfigData.isInRange("newFrontierChunkShapeWidth", value)) {
                        ConfigData.newFrontierChunkShapeWidth = value;
                        chunkShapeButtons.setSize(value);
                    }
                } else if (chunkShapeButtons.getShapeMeasure() == GuiChunkShapeButtons.ShapeMeasure.Length) {
                    if (ConfigData.isInRange("newFrontierChunkShapeLength", value)) {
                        ConfigData.newFrontierChunkShapeLength = value;
                        chunkShapeButtons.setSize(value);
                    }
                }
            }
        }
    }

    @Override
    public void removed() {
        MinecraftForge.EVENT_BUS.unregister(this);
        ClientProxy.configUpdated();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        ForgeHooksClient.popGuiLayer(minecraft);
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiNewFrontier(jmAPI));
    }

    private void shapeButtonsUpdated() {
        if (ConfigData.newFrontierMode == FrontierData.Mode.Vertex) {
            vertexShapeButtons.visible = true;
            chunkShapeButtons.visible = false;

            int selected = vertexShapeButtons.getSelected();
            ConfigData.newFrontierShape = selected;

            if (selected == 0 || selected == 1) {
                labelSize.visible = false;
                textSize.visible = false;
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Width) {
                labelSize.setText(new TranslatableComponent("mapfrontiers.shape_width"));
                textSize.setValue(String.valueOf(ConfigData.newFrontierShapeWidth));
            } else if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Radius) {
                labelSize.setText(new TranslatableComponent("mapfrontiers.shape_radius"));
                textSize.setValue(String.valueOf(ConfigData.newFrontierShapeRadius));
            }
        } else {
            vertexShapeButtons.visible = false;
            chunkShapeButtons.visible = true;

            int selected = chunkShapeButtons.getSelected();
            ConfigData.newFrontierChunkShape = selected;

            if (selected == 0 || selected == 1 || selected == 7) {
                labelSize.visible = false;
                textSize.visible = false;
                return;
            }

            labelSize.visible = true;
            textSize.visible = true;

            if (chunkShapeButtons.getShapeMeasure() == GuiChunkShapeButtons.ShapeMeasure.Width) {
                labelSize.setText(new TranslatableComponent("mapfrontiers.shape_width"));
                textSize.setValue(String.valueOf(ConfigData.newFrontierChunkShapeWidth));
                chunkShapeButtons.setSize(ConfigData.newFrontierChunkShapeWidth);
            } else if (chunkShapeButtons.getShapeMeasure() == GuiChunkShapeButtons.ShapeMeasure.Length) {
                labelSize.setText(new TranslatableComponent("mapfrontiers.shape_length"));
                textSize.setValue(String.valueOf(ConfigData.newFrontierChunkShapeLength));
                chunkShapeButtons.setSize(ConfigData.newFrontierChunkShapeLength);
            }
        }
    }

    private List<BlockPos> calculateVertices() {
        if (ConfigData.newFrontierMode != FrontierData.Mode.Vertex) {
            return null;
        }

        List<Vec2> shapeVertices = vertexShapeButtons.getVertices();
        if (shapeVertices == null) {
            return new ArrayList<>();
        }

        double radius = 0.0;

        if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Width) {
            radius = ConfigData.newFrontierShapeWidth;
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (vertexShapeButtons.getShapeMeasure() == GuiVertexShapeButtons.ShapeMeasure.Radius) {
            radius = ConfigData.newFrontierShapeRadius;
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
        if (ConfigData.newFrontierMode != FrontierData.Mode.Chunk) {
            return null;
        }

        List<ChunkPos> chunks = new ArrayList<>();
        ChunkPos playerChunk = new ChunkPos(minecraft.player.blockPosition());
        int selected = chunkShapeButtons.getSelected();

        if (selected == 1) {
            chunks.add(playerChunk);
        } else if (selected == 2) {
            int shapeWidth = ConfigData.newFrontierChunkShapeWidth;
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
            }
        } else if (selected == 3) {
            int shapeWidth = ConfigData.newFrontierChunkShapeWidth;
            ChunkPos start = new ChunkPos(playerChunk.x - shapeWidth / 2, playerChunk.z - shapeWidth / 2);
            for (int i = 0; i < shapeWidth * shapeWidth; ++i) {
                if (i < shapeWidth || i >= shapeWidth * (shapeWidth - 1) || (i % shapeWidth) == 0 || (i % shapeWidth) == shapeWidth - 1) {
                    chunks.add(new ChunkPos(start.x + (i % shapeWidth), start.z + i / shapeWidth));
                }
            }
        } else if (selected == 4) {
            int shapeWidth = ConfigData.newFrontierChunkShapeWidth;
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
            int shapeLength = ConfigData.newFrontierChunkShapeLength;
            int start = playerChunk.x - shapeLength / 2;
            for (int i = 0; i < shapeLength; ++i) {
                chunks.add(new ChunkPos(start + i, playerChunk.z));
            }
        } else if (selected == 6) {
            int shapeLength = ConfigData.newFrontierChunkShapeLength;
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
