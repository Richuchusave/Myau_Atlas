package myau.ui.impl.clickgui.raven;

import net.minecraft.client.gui.FontRenderer;

/**
 * One rendered line inside a {@link RavenCategory}: either a module header
 * ({@link RavenModule}) or one of its settings.
 *
 * Coordinates ({@link #x}/{@link #y}) are the last rendered position in screen
 * pixels and are assigned top-down during render, Kclient-style. Clicks between
 * frames hit-test against those stored coordinates, so there is exactly one
 * description of where each control is.
 */
public abstract class RavenRow {

    /** Last rendered rect, screen pixels. Assigned by render, read by click. */
    protected int x;
    protected int y;
    protected int w;
    protected int h;

    /** Full height of this row including expanded children. Constant for settings. */
    public abstract int height();

    /** Hidden by a visibility predicate or by the search filter. */
    public boolean visible() {
        return true;
    }

    public abstract void render(FontRenderer fr, int mouseX, int mouseY, int accent);

    /**
     * @return true when the click was consumed.
     */
    public boolean click(int mouseX, int mouseY, int button) {
        return false;
    }

    public void release(int mouseX, int mouseY, int button) {
    }

    /**
     * @return true when the key was consumed (binding rows, text rows).
     */
    public boolean key(char typedChar, int keyCode) {
        return false;
    }

    public void closed() {
    }

    protected boolean inside(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
    }
}
