package myau.ui.impl.clickgui.raven;

import myau.property.properties.ModeProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * Mode setting: left-click steps forward, right-click steps back through the
 * <i>visible</i> modes. No expanding dropdown, so no per-frame
 * {@code getValuePrompt().split(...)} and no scissored overlay list -- the old
 * dropdown rebuilt its mode list twice per frame per setting.
 */
public class RavenMode extends RavenRow {

    private final RavenModule parent;
    private final ModeProperty property;
    private int shownValue = Integer.MIN_VALUE;
    private String label = "";
    private int valueWidth;

    public RavenMode(RavenModule parent, ModeProperty property) {
        this.parent = parent;
        this.property = property;
    }

    @Override
    public int height() {
        return RavenTheme.SETTING_H;
    }

    @Override
    public boolean visible() {
        return this.property.isVisible();
    }

    private void refresh(FontRenderer fr) {
        int value = this.property.getValue();
        if (value != this.shownValue) {
            this.shownValue = value;
            this.label = this.property.getName() + ": " + this.property.getModeString();
            this.valueWidth = fr.getStringWidth(this.property.getModeString());
        }
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.w = this.parent.rowWidth();
        this.h = RavenTheme.SETTING_H;
        refresh(fr);
        int bg = inside(mouseX, mouseY) ? RavenTheme.SETTING_HOVER : RavenTheme.SETTING_BG;
        Gui.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, bg);
        fr.drawString(this.label, (float) (this.x + 4),
                (float) (this.y + (this.h - 8) / 2 + 1), RavenTheme.TEXT_DIM, false);
        fr.drawString(this.property.getModeString(), (float) (this.x + this.w - 4 - this.valueWidth),
                (float) (this.y + (this.h - 8) / 2 + 1), accent, false);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        int[] order = this.property.visibleIndices();
        if (order.length == 0) {
            return true;
        }
        int current = this.property.getValue();
        int at = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == current) {
                at = i;
                break;
            }
        }
        if (button == 0) {
            this.property.setValue(order[(at + 1) % order.length]);
        } else if (button == 1) {
            this.property.setValue(order[(at + order.length - 1) % order.length]);
        } else {
            return false;
        }
        // Force a label refresh on the next frame (refresh needs the font).
        this.shownValue = Integer.MIN_VALUE;
        return true;
    }
}
