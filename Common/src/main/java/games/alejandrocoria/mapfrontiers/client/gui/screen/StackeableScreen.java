package games.alejandrocoria.mapfrontiers.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Stack;

public class StackeableScreen extends Screen {
    private static final Stack<Screen> returnScreenStack = new Stack<>();

    public StackeableScreen(Component title, Screen returnScreen) {
        super(title);
        returnScreenStack.push(returnScreen);
    }

    protected void closeAndReturn() {
        if (minecraft == null) {
            return;
        }

        if (returnScreenStack.peek() == null) {
            minecraft.setScreen(null);
        } else {
            minecraft.setScreen(returnScreenStack.pop());
        }
    }

    protected void closeAndReturnUntil(Class<?> screenClass) {
        if (minecraft == null) {
            return;
        }

        while (returnScreenStack.peek() != null && !screenClass.isAssignableFrom(returnScreenStack.peek().getClass())) {
            closeAndReturn();
        }

        closeAndReturn();
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.closeAndReturn();
            return true;
        }
        return super.keyPressed(key, value, modifier);
    }

    public static void open(StackeableScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof StackeableScreen stackeableScreen) {
            stackeableScreen.close();
        }

        minecraft.setScreen(screen);
    }

    public static void popAndOpen(StackeableScreen screen) {
        if (returnScreenStack.peek() != null) {
            returnScreenStack.pop();
        }
        open(screen);
    }

    protected void close() {

    }
}
