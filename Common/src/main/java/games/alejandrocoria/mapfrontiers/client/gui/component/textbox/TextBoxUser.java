package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class TextBoxUser extends TextBox {
    private static final String AUTOCOMPLETE_HINT_KEY = "mapfrontiers.user_autocomplete_hint";
    private static final int MAX_VISIBLE_SUGGESTIONS = 7;
    private static final int SUGGESTION_WINDOW_OFFSET = 6;
    private static final int POPUP_LINE_HEIGHT = 12;
    private static final int POPUP_PADDING = 4;
    private static final int POPUP_BORDER = 1;

    private final Minecraft mc;
    private final Font font;
    private String partialText;
    private final List<String> suggestions;
    private final List<String> suggestionsToDraw;
    private Component error;
    private int maxSuggestionWidth = 0;
    private int suggestionIndex = 0;

    public TextBoxUser(Minecraft mc, Font font, int width) {
        this(mc, font, width, "");
    }

    public TextBoxUser(Minecraft mc, Font font, int width, String defaultText) {
        super(font, width, defaultText);
        this.mc = mc;
        this.font = font;
        suggestions = new ArrayList<>();
        suggestionsToDraw = new ArrayList<>();

        setError(null);
    }

    public void setError(@Nullable Component error) {
        this.error = error;

        if (this.error != null) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // @Note: Can't use Tab because it's used for accessibility.
        if (event.input() == GLFW.GLFW_KEY_LEFT_ALT) {
            if (suggestions.isEmpty()) {
                suggestionIndex = 0;
                ClientPacketListener handler = mc.getConnection();
                if (!StringUtils.isBlank(getValue()) && handler != null) {
                    partialText = getValue();
                    for (PlayerInfo playerInfo : handler.getOnlinePlayers()) {
                        String name = playerInfo.getProfile().name();
                        if (name != null && name.regionMatches(true, 0, partialText, 0, partialText.length())) {
                            suggestions.add(name);
                        }
                    }
                }
            } else {
                ++suggestionIndex;
                if (suggestionIndex >= suggestions.size()) {
                    suggestionIndex = 0;
                }
            }

            suggestionsToDraw.clear();

            if (!suggestions.isEmpty()) {
                this.setValue(suggestions.get(suggestionIndex));

                if (suggestions.size() == 1) {
                    suggestions.clear();
                } else {
                    maxSuggestionWidth = 0;
                    int size = suggestions.size();

                    if (size > MAX_VISIBLE_SUGGESTIONS) {
                        size = MAX_VISIBLE_SUGGESTIONS;
                    }

                    int firstIndex = 0;
                    if (suggestionIndex > SUGGESTION_WINDOW_OFFSET) {
                        firstIndex = suggestionIndex - SUGGESTION_WINDOW_OFFSET;
                    }

                    for (int i = firstIndex; i < firstIndex + size; ++i) {
                        suggestionsToDraw.add(suggestions.get(i));
                        int textWidth = font.width(suggestions.get(i));
                        if (textWidth > maxSuggestionWidth) {
                            maxSuggestionWidth = textWidth;
                        }
                    }
                }
            }

            return true;
        } else {
            suggestions.clear();
            suggestionsToDraw.clear();
            return super.keyPressed(event);
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (error == null) {
            setTextColor(ColorConstants.TEXTBOX_TEXT);
        } else {
            setTextColor(ColorConstants.TEXT_ERROR_NORMAL);
        }

        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);

        if (error != null) {
            List<FormattedCharSequence> errorList = font.split(error, width - POPUP_PADDING * 2);
            int maxErrorWidth = width - POPUP_PADDING * 2;
            int popupHeight = errorList.size() * POPUP_LINE_HEIGHT;

            drawPopupFrame(graphics, maxErrorWidth, popupHeight);

            int posX = getX() + POPUP_PADDING;
            int posY = getY() - popupHeight;
            for (FormattedCharSequence e : errorList) {
                graphics.text(font, e, posX, posY, ColorConstants.TEXTBOX_POPUP_TEXT);
                posY += POPUP_LINE_HEIGHT;
            }
        } else if (!suggestionsToDraw.isEmpty()) {
            int popupHeight = suggestionsToDraw.size() * POPUP_LINE_HEIGHT;
            drawPopupFrame(graphics, maxSuggestionWidth, popupHeight);

            int posX = getX() + POPUP_PADDING;
            int posY = getY() - POPUP_LINE_HEIGHT;
            for (int i = suggestionsToDraw.size() - 1; i >= 0; --i) {
                String t = suggestionsToDraw.get(i);
                // Strings are compared using == because they are the same objects
                // that are added to suggestions and suggestionsToDraw
                //noinspection StringEquality
                if (suggestionsToDraw.get(i) == suggestions.get(suggestionIndex)) {
                    graphics.text(font, t, posX, posY, ColorConstants.TEXTBOX_POPUP_TEXT);
                } else {
                    String suffix = t.substring(0, partialText.length());
                    String rest = t.substring(partialText.length());
                    graphics.text(font, suffix, posX, posY, ColorConstants.TEXTBOX_POPUP_TEXT);
                    graphics.text(font, rest, posX + font.width(suffix), posY, ColorConstants.TEXT_MEDIUM);
                }

                posY -= POPUP_LINE_HEIGHT;
            }
        } else if (isFocused() && !StringUtils.isBlank(getValue())) {
            Component hint = Component.translatable(AUTOCOMPLETE_HINT_KEY, "Alt");
            String[] hintLines = hint.getString().split("\n");
            int maxHintWidth = 0;
            for (String line : hintLines) {
                maxHintWidth = Math.max(maxHintWidth, font.width(line));
            }

            int popupHeight = hintLines.length * POPUP_LINE_HEIGHT;
            drawPopupFrame(graphics, maxHintWidth, popupHeight);

            int posY = getY() - popupHeight;
            for (String line : hintLines) {
                graphics.text(font, line, getX() + POPUP_PADDING, posY, ColorConstants.TEXT_HIGHLIGHT);
                posY += POPUP_LINE_HEIGHT;
            }
        }
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        super.setFocused(isFocusedIn);

        if (!isFocusedIn) {
            suggestions.clear();
            suggestionsToDraw.clear();
        } else {
            setError(null);
        }
    }

    private void drawPopupFrame(GuiGraphicsExtractor graphics, int contentWidth, int popupHeight) {
        graphics.fill(getX(), getY() - popupHeight - POPUP_PADDING,
                getX() + contentWidth + POPUP_PADDING * 2, getY(),
                ColorConstants.TEXTBOX_POPUP_BORDER);
        graphics.fill(getX() + POPUP_BORDER, getY() - popupHeight - POPUP_PADDING + POPUP_BORDER,
                getX() + contentWidth + POPUP_PADDING * 2 - POPUP_BORDER, getY(),
                ColorConstants.TEXTBOX_POPUP_BG);
    }
}
