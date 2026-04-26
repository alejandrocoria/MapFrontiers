package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

@ParametersAreNonnullByDefault
public class CollectionInfoPage extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_collection_info");
    private static final Component NAME_LABEL = Component.translatable("mapfrontiers.name");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Component PERSONAL_LABEL = Component.translatable("mapfrontiers.config.Personal");
    private static final Component GLOBAL_LABEL = Component.translatable("mapfrontiers.config.Global");
    private static final String OWNER_KEY = "mapfrontiers.owner";
    private static final Component R_LABEL = Component.literal("R");
    private static final Component G_LABEL = Component.literal("G");
    private static final Component B_LABEL = Component.literal("B");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;
    private static final int RGB_LABEL_HEIGHT = 8;
    private static final int RGB_TEXTBOX_WIDTH = 33;
    private static final int RGB_ROW_SPACER_WIDTH = 4;
    private static final int RGB_INLINE_SPACING = 3;

    private final UUID collectionId;
    private final CollectionData originalCollection;
    private final CollectionData collection;
    private boolean saveChangesOnClose = true;

    private TextBox textName;
    private TextBoxInt textRed;
    private TextBoxInt textGreen;
    private TextBoxInt textBlue;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private SimpleButton buttonDone;

    public CollectionInfoPage(CollectionData collection) {
        super(TITLE_LABEL);
        this.collectionId = collection.getId();
        this.originalCollection = new CollectionData(collection);
        this.collection = new CollectionData(collection);

        MapFrontiersClient.getCollectionEvents().subscribeDeleted(this, deletedId -> {
            if (collectionId.equals(deletedId)) {
                saveChangesOnClose = false;
                onClose();
            }
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> updateButtons());
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);

        buildOverviewSection(mainLayout);
        buildInfoSection(mainLayout);
        buildColorSection(mainLayout);

        buttonDone = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DONE_LABEL, button -> onClose()));

        updateButtons();
        setInitialFocus(buttonDone);
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout overviewColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 0, 1, 2);

        overviewColumn.addChild(new StringWidget(NAME_LABEL, font).setColor(ColorConstants.WHITE));

        textName = new TextBox(font, NAME_SECTION_WIDTH);
        textName.setMaxLength(CollectionData.MAX_NAME_CHARACTERS);
        textName.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textName.setValue(collection.getName());
        textName.setValueChangedCallback(collection::setName);
        overviewColumn.addChild(textName);
    }

    private void buildInfoSection(GridLayout mainLayout) {
        LinearLayout infoColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        mainLayout.addChild(infoColumn, 0, 2, LayoutSettings.defaults().alignHorizontallyLeft());

        infoColumn.addChild(new StringWidget(Component.translatable(OWNER_KEY, collection.getOwner().toString()), font).setColor(ColorConstants.WHITE));
        infoColumn.addChild(new StringWidget(collection.getPersonal() ? PERSONAL_LABEL : GLOBAL_LABEL, font).setColor(ColorConstants.WHITE));
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(collection.getColor(), (color, dragging) -> {
            collection.setColor(color);
            syncColorWidgets(color);
        });
        mainLayout.addChild(colorPicker, 0, 3, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 0, 4, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(RGB_INLINE_SPACING);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(R_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_R));
        textRed = createRgbTextBox(value -> (collection.getColor() & 0xFF00FFFF) | (value << 16));
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(G_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_G));
        textGreen = createRgbTextBox(value -> (collection.getColor() & 0xFFFF00FF) | (value << 8));
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(B_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_B));
        textBlue = createRgbTextBox(value -> (collection.getColor() & 0xFFFFFF00) | value);
        rgbRow.addChild(textBlue);

        colorPalette = new ColorPaletteWidget(collection.getColor(), color -> {
            colorPicker.setColor(color);
            collection.setColor(color);
            syncColorWidgets(color);
        });
        colorColumn.addChild(colorPalette);

        syncColorWidgets(collection.getColor());
    }

    private TextBoxInt createRgbTextBox(IntUnaryOperator colorComposer) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, RGB_TEXTBOX_WIDTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValueChangedCallback(value -> {
            int color = colorComposer.applyAsInt(value);
            if (color != collection.getColor()) {
                collection.setColor(color);
                colorPicker.setColor(color);
                syncColorWidgets(color);
            }
        });
        return textBox;
    }

    private void syncColorWidgets(int color) {
        textRed.setValue((color & 0xFF0000) >> 16);
        textGreen.setValue((color & 0x00FF00) >> 8);
        textBlue.setValue(color & 0x0000FF);
        colorPalette.setColor(color);
    }

    private void updateButtons() {
        boolean editable = canUpdateCollection();
        textName.setEditable(editable);
        textRed.setEditable(editable);
        textGreen.setEditable(editable);
        textBlue.setEditable(editable);
        colorPicker.active = editable;
        colorPalette.active = editable;
    }

    private boolean canUpdateCollection() {
        if (minecraft.player == null) {
            return false;
        }

        SettingsUser playerUser = new SettingsUser(minecraft.player);
        if (collection.getPersonal()) {
            return collection.getOwner().equals(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && (profile.updateFrontier == SettingsProfile.State.Enabled
                || (profile.updateFrontier == SettingsProfile.State.Owner && collection.getOwner().equals(playerUser)));
    }

    private boolean hasChanges() {
        return !Objects.equals(originalCollection.getName(), collection.getName())
                || originalCollection.getColor() != collection.getColor();
    }

    @Override
    public void onClose() {
        if (saveChangesOnClose && hasChanges() && canUpdateCollection()) {
            MapFrontiersClient.getOperationService().updateCollection(collection);
        }

        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        super.onClose();
    }
}
