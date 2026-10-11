package myau.ui.impl.clickgui.raven;

import myau.module.Module;
import myau.util.KeyBindUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

/**
 * Module keybind row. Click to arm, press a key to bind, Delete/Backspace/Esc
 * to clear. The bind label width is cached and only remeasured when the text
 * changes -- the old components remeasured both labels every frame.
 */
public class RavenBind extends RavenRow {

    private final RavenModule parent;
    private final Module module;
    private boolean binding;
    private String shownText = "";
    private int shownWidth;

    public RavenBind(RavenModule parent, Module module) {
        this.parent = parent;
        this.module = module;
    }

    public boolean isBinding() {
        return this.binding;
    }

    @Override
    public int height() {
        return RavenTheme.SETTING_H;
    }

    private String bindText() {
        if (this.binding) {
            return "...";
        }
        int key = this.module.getKey();
        return key == Keyboard.KEY_NONE ? "None" : KeyBindUtil.getKeyName(key);
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.w = this.parent.rowWidth();
        this.h = RavenTheme.SETTING_H;
        String text = bindText();
        if (!text.equals(this.shownText)) {
            this.shownText = text;
            this.shownWidth = fr.getStringWidth(text);
        }
        int valueColor = this.binding ? accent : RavenTheme.TEXT_FAINT;
        fr.drawString("Bind", (float) (this.x + 4),
                (float) (this.y + (this.h - 8) / 2 + 1), RavenTheme.TEXT_DIM, false);
        int boxW = Math.max(26, this.shownWidth + 10);
        int boxX = this.x + this.w - boxW - 4;
        int boxY = this.y + 1;
        Gui.drawRect(boxX, boxY, boxX + boxW, boxY + this.h - 2, RavenTheme.BOX_BG);
        if (this.binding) {
            Gui.drawRect(boxX, boxY, boxX + boxW, boxY + 1, accent);
            Gui.drawRect(boxX, boxY + this.h - 3, boxX + boxW, boxY + this.h - 2, accent);
            Gui.drawRect(boxX, boxY, boxX + 1, boxY + this.h - 2, accent);
            Gui.drawRect(boxX + boxW - 1, boxY, boxX + boxW, boxY + this.h - 2, accent);
        }
        fr.drawString(this.shownText, (float) (boxX + (boxW - this.shownWidth) / 2),
                (float) (boxY + (this.h - 2 - 8) / 2), valueColor, false);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY)) {
            this.binding = !this.binding;
            return true;
        }
        if (this.binding) {
            this.binding = false;
        }
        return false;
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (!this.binding) {
            return false;
        }
        if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_ESCAPE) {
            this.module.setKey(Keyboard.KEY_NONE);
        } else {
            this.module.setKey(keyCode);
        }
        this.binding = false;
        this.shownText = "";
        return true;
    }

    @Override
    public void closed() {
        this.binding = false;
    }
}
