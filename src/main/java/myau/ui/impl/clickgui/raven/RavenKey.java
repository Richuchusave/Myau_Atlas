package myau.ui.impl.clickgui.raven;

import myau.property.properties.KeyProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

/**
 * KeyProperty row (hold-keys etc.): same behaviour as {@link RavenBind} but
 * bound to a setting instead of the module.
 */
public class RavenKey extends RavenRow {

    private final RavenModule parent;
    private final KeyProperty property;
    private boolean binding;
    private String shownText = "";
    private int shownWidth;

    public RavenKey(RavenModule parent, KeyProperty property) {
        this.parent = parent;
        this.property = property;
    }

    public boolean isBinding() {
        return this.binding;
    }

    @Override
    public int height() {
        return RavenTheme.SETTING_H;
    }

    @Override
    public boolean visible() {
        return this.property.isVisible();
    }

    private String bindText() {
        return this.binding ? "..." : this.property.getKeyName();
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
        fr.drawString(this.property.getName(), (float) (this.x + 4),
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
            this.property.setValue(Keyboard.KEY_NONE);
        } else {
            this.property.setValue(keyCode);
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
