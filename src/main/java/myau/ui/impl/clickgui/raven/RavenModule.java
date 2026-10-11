package myau.ui.impl.clickgui.raven;

import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.KeyProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.TextProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One module: a 14px header row (click toggles, right-click expands) plus its
 * setting rows when open.
 *
 * Children are built once. Heights are summed fresh every frame over the
 * visible children only -- integer addition, no allocation -- so runtime
 * visibility changes (mode cycles, commands, tuners) can never desync the
 * layout. The lower-case name is cached for the search filter.
 */
public class RavenModule extends RavenRow {

    private final RavenCategory category;
    private final Module module;
    private final String lowerName;
    private final List<RavenRow> children = new ArrayList<RavenRow>();

    private boolean open;
    private boolean hovered;

    /** Clip rect assigned by the category during render; clicks outside it are ignored. */
    private int clipTop = Integer.MIN_VALUE;
    private int clipBottom = Integer.MAX_VALUE;

    private String shownLabel = "";
    private int shownLabelWidth = -1;
    /* getSuffix() implementations do real work per call (session lookups,
       time math), so the label refreshes at 2 Hz, not every frame. */
    private long lastSuffixCheck;

    public RavenModule(RavenCategory category, Module module) {
        this.category = category;
        this.module = module;
        this.lowerName = module.getName().toLowerCase(Locale.ROOT);
        buildChildren();
    }

    private void buildChildren() {
        this.children.add(new RavenBind(this, this.module));
        if (Myau.propertyManager == null) {
            return;
        }
        List<Property<?>> properties = Myau.propertyManager.properties.get(this.module.getClass());
        if (properties == null) {
            return;
        }
        for (Property<?> property : properties) {
            if (property instanceof KeyProperty) {
                this.children.add(new RavenKey(this, (KeyProperty) property));
            } else if (property instanceof BooleanProperty) {
                this.children.add(new RavenBool(this, (BooleanProperty) property));
            } else if (property instanceof IntProperty || property instanceof FloatProperty
                    || property instanceof PercentProperty) {
                this.children.add(new RavenSlider(this, property));
            } else if (property instanceof ModeProperty) {
                this.children.add(new RavenMode(this, (ModeProperty) property));
            } else if (property instanceof ColorProperty) {
                this.children.add(new RavenColor(this, (ColorProperty) property));
            } else if (property instanceof TextProperty) {
                this.children.add(new RavenText(this, (TextProperty) property));
            }
        }
    }

    public Module module() {
        return this.module;
    }

    public boolean matches(String needle) {
        return needle.isEmpty() || this.lowerName.contains(needle);
    }

    public boolean isOpen() {
        return this.open;
    }

    /** Width available to children: category width minus side padding. */
    public int rowWidth() {
        return this.category.rowWidth();
    }

    public boolean anyBinding() {
        for (RavenRow row : this.children) {
            if (row instanceof RavenBind && ((RavenBind) row).isBinding()) {
                return true;
            }
            if (row instanceof RavenKey && ((RavenKey) row).isBinding()) {
                return true;
            }
        }
        return false;
    }

    public boolean anyTextFocused() {
        for (RavenRow row : this.children) {
            if (row instanceof RavenText && ((RavenText) row).isFocused()) {
                return true;
            }
        }
        return false;
    }

    public void unfocusText() {
        for (RavenRow row : this.children) {
            if (row instanceof RavenText) {
                ((RavenText) row).unfocus();
            }
        }
    }

    @Override
    public int height() {
        if (!this.open) {
            return RavenTheme.ROW_H;
        }
        // Always summed fresh: visibility predicates can change at runtime
        // (mode cycles, commands, tuners). This is integer addition over the
        // visible children only -- the per-frame cost the old menu paid was
        // never the summation but the allocations and shader binds around it.
        int sum = 0;
        for (RavenRow row : this.children) {
            if (row.visible()) {
                sum += row.height();
            }
        }
        return RavenTheme.ROW_H + sum;
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.h = RavenTheme.ROW_H;
        this.hovered = mouseX >= this.x && mouseX < this.x + this.w
                && mouseY >= this.y && mouseY < this.y + RavenTheme.ROW_H;
        Gui.drawRect(this.x, this.y, this.x + this.w, this.y + RavenTheme.ROW_H,
                this.hovered ? RavenTheme.ROW_HOVER : RavenTheme.ROW_BG);

        long now = System.currentTimeMillis();
        if (now - this.lastSuffixCheck > 500L) {
            this.lastSuffixCheck = now;
            String[] suffix = this.module.getSuffix();
            String label = this.module.getName();
            if (suffix != null && suffix.length > 0) {
                StringBuilder sb = new StringBuilder(label);
                for (String s : suffix) {
                    if (s != null && !s.isEmpty()) {
                        sb.append(' ').append(s);
                    }
                }
                label = sb.toString();
            }
            if (!label.equals(this.shownLabel)) {
                this.shownLabel = label;
                this.shownLabelWidth = fr.getStringWidth(label);
            }
        }
        int color = this.module.isEnabled() ? accent : RavenTheme.TEXT_DIM;
        fr.drawString(this.shownLabel, (float) (this.x + (this.w - this.shownLabelWidth) / 2),
                (float) (this.y + 3), color, false);

        if (this.open) {
            int childY = this.y + RavenTheme.ROW_H;
            for (RavenRow row : this.children) {
                if (!row.visible()) {
                    continue;
                }
                row.x = this.x;
                row.y = childY;
                row.render(fr, mouseX, mouseY, accent);
                childY += row.height();
            }
        }
    }

    /** Renders with vertical culling against the body rect. */
    public void renderCulled(FontRenderer fr, int mouseX, int mouseY, int accent, int clipTop, int clipBottom) {
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
        int full = height();
        if (this.y + full < clipTop || this.y > clipBottom) {
            this.h = RavenTheme.ROW_H;
            return;
        }
        render(fr, mouseX, mouseY, accent);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        // Fully culled modules keep stale children coordinates (their rows
        // were never re-laid-out); they must not steal clicks from the
        // module now occupying that rect.
        if (this.y + height() < this.clipTop || this.y > this.clipBottom) {
            return false;
        }
        if (mouseX >= this.x && mouseX < this.x + this.w
                && mouseY >= this.y && mouseY < this.y + RavenTheme.ROW_H) {
            if (button == 0) {
                this.module.toggle();
                return true;
            }
            if (button == 1) {
                this.open = !this.open;
                return true;
            }
            return false;
        }
        if (this.open) {
            for (RavenRow row : this.children) {
                if (!row.visible()) {
                    continue;
                }
                // Children scissored out of the body must not consume clicks.
                if (row.y + row.h <= this.clipTop || row.y >= this.clipBottom) {
                    continue;
                }
                if (row.click(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Wheel over an open module nudges a hovered slider. */
    public boolean wheel(int mouseX, int mouseY, int amount) {
        if (!this.open) {
            return false;
        }
        for (RavenRow row : this.children) {
            if (row instanceof RavenSlider && row.visible()
                    && ((RavenSlider) row).hovering(mouseX, mouseY)) {
                return ((RavenSlider) row).wheel(amount);
            }
        }
        return false;
    }

    @Override
    public void release(int mouseX, int mouseY, int button) {
        for (RavenRow row : this.children) {
            row.release(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (!this.open) {
            return false;
        }
        for (RavenRow row : this.children) {
            if (row.visible() && row.key(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void closed() {
        for (RavenRow row : this.children) {
            row.closed();
        }
        this.hovered = false;
    }
}
