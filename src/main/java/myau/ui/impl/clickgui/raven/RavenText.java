package myau.ui.impl.clickgui.raven;

import myau.property.properties.TextProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

/**
 * Text setting. Click to focus and type directly (committed live, like the
 * old menu); click elsewhere, Enter or Escape to unfocus. Overflow is trimmed
 * with the vanilla font trimmer instead of drawing past the box.
 */
public class RavenText extends RavenRow {

    private final RavenModule parent;
    private final TextProperty property;
    private boolean focused;
    private boolean blink = true;
    private long lastBlink;

    public RavenText(RavenModule parent, TextProperty property) {
        this.parent = parent;
        this.property = property;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void unfocus() {
        this.focused = false;
    }

    @Override
    public int height() {
        return RavenTheme.SETTING_H;
    }

    @Override
    public boolean visible() {
        return this.property.isVisible();
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.w = this.parent.rowWidth();
        this.h = RavenTheme.SETTING_H;
        Gui.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, RavenTheme.BOX_BG);
        if (this.focused) {
            long now = System.currentTimeMillis();
            if (now - this.lastBlink > 500L) {
                this.blink = !this.blink;
                this.lastBlink = now;
            }
            int line = accent;
            Gui.drawRect(this.x, this.y + this.h - 1, this.x + this.w, this.y + this.h, line);
        }
        String value = this.property.getValue();
        if (value == null) {
            value = "";
        }
        String shown;
        int color;
        if (value.isEmpty() && !this.focused) {
            shown = this.property.getName();
            color = RavenTheme.TEXT_FAINT;
        } else {
            shown = value;
            color = RavenTheme.TEXT;
        }
        int maxW = this.w - 10;
        if (fr.getStringWidth(shown) > maxW) {
            shown = fr.trimStringToWidth(shown, maxW);
        }
        fr.drawString(shown, (float) (this.x + 5),
                (float) (this.y + (this.h - 8) / 2 + 1), color, false);
        if (this.focused && this.blink) {
            int cx = this.x + 5 + fr.getStringWidth(shown);
            Gui.drawRect(cx + 1, this.y + 2, cx + 2, this.y + this.h - 2, RavenTheme.TEXT);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY)) {
            this.focused = true;
            this.blink = true;
            this.lastBlink = System.currentTimeMillis();
            return true;
        }
        this.focused = false;
        return false;
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (!this.focused) {
            return false;
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            this.focused = false;
            return true;
        }
        String value = this.property.getValue();
        if (value == null) {
            value = "";
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (!value.isEmpty()) {
                // Delete one code point, not one char, so emoji don't split
                // into lone surrogates.
                int end = value.offsetByCodePoints(value.length(), -1);
                this.property.setValue(value.substring(0, end));
            }
        } else if (typedChar != '\0' && !Character.isISOControl(typedChar)) {
            if (value.length() < 64) {
                this.property.setValue(value + typedChar);
            }
        }
        return true;
    }

    @Override
    public void closed() {
        this.focused = false;
    }
}
