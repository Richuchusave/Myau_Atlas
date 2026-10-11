package myau.ui.impl.clickgui.raven;

import myau.module.Category;
import myau.module.Module;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import myau.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * One draggable category column, Kclient-style: accent header, dark body,
 * per-column instant scrolling, no open/close or hover animations.
 */
public class RavenCategory {

    private final Category category;
    private final List<RavenModule> modules = new ArrayList<RavenModule>();

    public int x = 5;
    public int y = 5;
    public boolean open = true;
    public int scroll;
    /** True once flow-placed or dragged, so reopening never re-flows it. */
    public boolean placed;

    private boolean dragging;
    private int dragOffX;
    private int dragOffY;

    private int nameWidth = -1;

    public RavenCategory(Category category, List<Module> modules) {
        this.category = category;
        for (Module module : modules) {
            this.modules.add(new RavenModule(this, module));
        }
    }

    public String name() {
        return this.category.displayName();
    }

    public List<RavenModule> modules() {
        return this.modules;
    }

    /** Width available to module rows: full column width. */
    public int rowWidth() {
        return RavenTheme.CAT_W;
    }

    /** Modules passing the search filter. */
    public List<RavenModule> filtered(String needle) {
        if (needle.isEmpty()) {
            return this.modules;
        }
        List<RavenModule> out = null;
        for (RavenModule module : this.modules) {
            if (module.matches(needle)) {
                if (out == null) {
                    out = new ArrayList<RavenModule>();
                }
                out.add(module);
            }
        }
        return out == null ? this.modules.subList(0, 0) : out;
    }

    /** Full body content height for the current filter. */
    public int contentHeight(String needle) {
        int sum = 0;
        for (RavenModule module : filtered(needle)) {
            sum += module.height();
        }
        return sum;
    }

    public boolean anyBinding() {
        for (RavenModule module : this.modules) {
            if (module.anyBinding()) {
                return true;
            }
        }
        return false;
    }

    public boolean anyTextFocused() {
        for (RavenModule module : this.modules) {
            if (module.anyTextFocused()) {
                return true;
            }
        }
        return false;
    }

    public void unfocusText() {
        for (RavenModule module : this.modules) {
            module.unfocusText();
        }
    }

    public boolean overHeader(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + RavenTheme.CAT_W
                && mouseY >= this.y && mouseY < this.y + RavenTheme.HEADER_H;
    }

    public boolean overBody(int mouseX, int mouseY, int bodyBottom) {
        return mouseX >= this.x && mouseX < this.x + RavenTheme.CAT_W
                && mouseY >= this.y && mouseY < bodyBottom;
    }

    public void render(FontRenderer fr, int mouseX, int mouseY, int accent, String needle,
                       int screenW, int screenH) {
        if (this.nameWidth < 0) {
            this.nameWidth = fr.getStringWidth(name());
        }
        if (this.dragging) {
            this.x = mouseX - this.dragOffX;
            this.y = mouseY - this.dragOffY;
            if (this.x < 0) {
                this.x = 0;
            }
            if (this.y < 0) {
                this.y = 0;
            }
            if (this.x > screenW - RavenTheme.CAT_W) {
                this.x = screenW - RavenTheme.CAT_W;
            }
            if (this.y > screenH - RavenTheme.HEADER_H) {
                this.y = screenH - RavenTheme.HEADER_H;
            }
        }

        boolean searching = !needle.isEmpty();
        boolean showBody = this.open || searching;
        List<RavenModule> list = showBody ? filtered(needle) : null;

        Gui.drawRect(this.x, this.y, this.x + RavenTheme.CAT_W, this.y + RavenTheme.HEADER_H, accent);
        fr.drawString(name(), (float) (this.x + (RavenTheme.CAT_W - this.nameWidth) / 2),
                (float) (this.y + (RavenTheme.HEADER_H - 8) / 2 + 1), RavenTheme.TEXT, false);

        if (list == null || list.isEmpty()) {
            return;
        }

        int content = contentHeight(needle);
        int maxBody = screenH - this.y - RavenTheme.HEADER_H - 4;
        if (maxBody < 40) {
            maxBody = 40;
        }
        int bodyH = Math.min(content, maxBody);
        if (bodyH <= 0) {
            return;
        }
        int maxScroll = Math.max(0, content - bodyH);
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        } else if (this.scroll < 0) {
            this.scroll = 0;
        }

        Gui.drawRect(this.x, this.y + RavenTheme.HEADER_H,
                this.x + RavenTheme.CAT_W, this.y + RavenTheme.HEADER_H + bodyH, RavenTheme.BODY_BG);

        int clipTop = this.y + RavenTheme.HEADER_H;
        int clipBottom = clipTop + bodyH;
        boolean needScissor = this.scroll != 0 || content > bodyH;
        if (needScissor) {
            RenderUtil.scissor(this.x, clipTop, RavenTheme.CAT_W, bodyH);
        }
        int rowY = this.y + RavenTheme.HEADER_H - this.scroll;
        for (RavenModule module : list) {
            module.x = this.x;
            module.y = rowY;
            module.w = RavenTheme.CAT_W;
            module.renderCulled(fr, mouseX, mouseY, accent, clipTop, clipBottom);
            rowY += module.height();
            if (rowY > clipBottom) {
                break;
            }
        }
        if (needScissor) {
            RenderUtil.releaseScissor();
        }
    }

    public int bodyBottom(String needle, int screenH) {
        boolean searching = !needle.isEmpty();
        if (!this.open && !searching) {
            return this.y + RavenTheme.HEADER_H;
        }
        int content = contentHeight(needle);
        int maxBody = screenH - this.y - RavenTheme.HEADER_H - 4;
        if (maxBody < 40) {
            maxBody = 40;
        }
        return this.y + RavenTheme.HEADER_H + Math.min(content, maxBody);
    }

    public boolean click(int mouseX, int mouseY, int button, String needle, int screenH) {
        if (overHeader(mouseX, mouseY)) {
            if (button == 0) {
                this.dragging = true;
                this.placed = true;
                this.dragOffX = mouseX - this.x;
                this.dragOffY = mouseY - this.y;
                return true;
            }
            if (button == 1 && needle.isEmpty()) {
                this.open = !this.open;
                return true;
            }
            return false;
        }
        boolean searching = !needle.isEmpty();
        if (!this.open && !searching) {
            return false;
        }
        if (mouseY < this.y + RavenTheme.HEADER_H || mouseY >= bodyBottom(needle, screenH)) {
            return false;
        }
        for (RavenModule module : filtered(needle)) {
            if (module.click(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /** Wheel over this column: nudge a hovered slider, else scroll the column. */
    public boolean wheel(int mouseX, int mouseY, int amount, String needle, int screenH) {
        if (!overBody(mouseX, mouseY, bodyBottom(needle, screenH))) {
            return false;
        }
        boolean searching = !needle.isEmpty();
        if (this.open || searching) {
            for (RavenModule module : filtered(needle)) {
                if (module.wheel(mouseX, mouseY, amount)) {
                    return true;
                }
            }
        }
        if (this.open || searching) {
            this.scroll -= amount > 0 ? 14 : -14;
            int content = contentHeight(needle);
            int maxBody = screenH - this.y - RavenTheme.HEADER_H - 4;
            if (maxBody < 40) {
                maxBody = 40;
            }
            int maxScroll = Math.max(0, content - Math.min(content, maxBody));
            if (this.scroll > maxScroll) {
                this.scroll = maxScroll;
            } else if (this.scroll < 0) {
                this.scroll = 0;
            }
            return true;
        }
        return false;
    }

    public boolean key(char typedChar, int keyCode, String needle) {
        boolean searching = !needle.isEmpty();
        if (!this.open && !searching) {
            return false;
        }
        for (RavenModule module : filtered(needle)) {
            if (module.key(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    public void release() {
        this.dragging = false;
        for (RavenModule module : this.modules) {
            module.release(0, 0, 0);
        }
    }

    public void closed() {
        this.dragging = false;
        for (RavenModule module : this.modules) {
            module.closed();
        }
    }
}
