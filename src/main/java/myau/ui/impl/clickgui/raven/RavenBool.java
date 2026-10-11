package myau.ui.impl.clickgui.raven;

import myau.property.properties.BooleanProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * Boolean setting: label on the left, instant on/off pill on the right.
 * No toggle animation (minimal-animations build): the pill snaps.
 */
public class RavenBool extends RavenRow {

    private final RavenModule parent;
    private final BooleanProperty property;

    public RavenBool(RavenModule parent, BooleanProperty property) {
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

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.w = this.parent.rowWidth();
        this.h = RavenTheme.SETTING_H;
        int bg = inside(mouseX, mouseY) ? RavenTheme.SETTING_HOVER : RavenTheme.SETTING_BG;
        Gui.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, bg);

        fr.drawString(this.property.getName(), (float) (this.x + 4),
                (float) (this.y + (this.h - 8) / 2 + 1), RavenTheme.TEXT_DIM, false);

        boolean on = Boolean.TRUE.equals(this.property.getValue());
        int pillW = 16;
        int pillH = 8;
        int pillX = this.x + this.w - pillW - 4;
        int pillY = this.y + (this.h - pillH) / 2;
        Gui.drawRect(pillX, pillY, pillX + pillW, pillY + pillH, on ? accent : RavenTheme.BOX_LINE);
        int knob = 6;
        int knobX = on ? pillX + pillW - knob - 1 : pillX + 1;
        Gui.drawRect(knobX, pillY + 1, knobX + knob, pillY + 1 + knob, RavenTheme.TEXT);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY)) {
            this.property.setValue(!Boolean.TRUE.equals(this.property.getValue()));
            return true;
        }
        return false;
    }
}
