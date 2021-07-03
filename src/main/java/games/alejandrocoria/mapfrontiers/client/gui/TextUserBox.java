package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextUserBox extends TextBox {
    private final Minecraft mc;
    private String partialText;
    private final List<String> suggestions;
    private final List<String> suggestionsToDraw;
    private ITextComponent error;
    private int maxSuggestionWidth = 0;
    private int suggestionIndex = 0;

    public TextUserBox(Minecraft mc, FontRenderer font, int x, int y, int width, String defaultText) {
        super(font, x, y, width, defaultText);
        this.mc = mc;
        suggestions = new ArrayList<>();
        suggestionsToDraw = new ArrayList<>();

        setError(null);
    }

    public void setError(@Nullable ITextComponent error) {
        this.error = error;

        if (this.error == null) {
            setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        } else {
            setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
            suggestions.clear();
            suggestionsToDraw.clear();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // @Note: Can't use Tab because it's used for accessibility.
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT) {
            if (suggestions.isEmpty()) {
                suggestionIndex = 0;
                ClientPlayNetHandler handler = mc.getConnection();
                if (!StringUtils.isBlank(getValue()) && handler != null) {
                    partialText = getValue();
                    for (NetworkPlayerInfo playerInfo : handler.getOnlinePlayers()) {
                        String name = playerInfo.getProfile().getName();
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

                    if (size > 7) {
                        size = 7;
                    }

                    int firstIndex = 0;
                    if (suggestionIndex > 6) {
                        firstIndex = suggestionIndex - 6;
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
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);

        if (error != null) {
            List<IReorderingProcessor> errorList = font.split(error, width - 8);
            int maxErrorWidth = width - 8;

            fill(matrixStack, x - 1, y - errorList.size() * 12 - 5, x + maxErrorWidth + 9, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            fill(matrixStack, x, y - errorList.size() * 12 - 4, x + maxErrorWidth + 8, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = x + 4;
            int posY = y - errorList.size() * 12;
            for (IReorderingProcessor e : errorList) {
                font.draw(matrixStack, e, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                posY += 12;
            }
        } else if (!suggestionsToDraw.isEmpty()) {
            fill(matrixStack, x - 1, y - suggestionsToDraw.size() * 12 - 5, x + maxSuggestionWidth + 9, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            fill(matrixStack, x, y - suggestionsToDraw.size() * 12 - 4, x + maxSuggestionWidth + 8, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = x + 4;
            int posY = y - 12;
            for (int i = suggestionsToDraw.size() - 1; i >= 0; --i) {
                String t = suggestionsToDraw.get(i);
                if (suggestionsToDraw.get(i) == suggestions.get(suggestionIndex)) {
                    font.draw(matrixStack, t, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                } else {
                    String suffix = t.substring(0, partialText.length());
                    String rest = t.substring(partialText.length());
                    font.draw(matrixStack, suffix, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    font.draw(matrixStack, rest, posX + font.width(suffix), posY,
                            GuiColors.SETTINGS_TEXT_MEDIUM);
                }

                posY -= 12;
            }
        }
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        super.setFocused(isFocusedIn);

        if (!isFocusedIn) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }

        setError(null);
    }

    @Override
    protected void onFocusedChanged(boolean focused) {
        super.onFocusedChanged(focused);

        if (!focused) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }

        setError(null);
    }
}
