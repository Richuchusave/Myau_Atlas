package myau.ui.impl.clickgui.raven;

import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Mouse;

/**
 * Numeric setting (Int / Float / Percent): label + cached value text on top,
 * plain track + fill + knob below.
 *
 * The value text is only reformatted when the value actually changes -- the
 * old menu built a {@code BigDecimal} per slider per frame just to draw it.
 * Dragging updates from the pointer position passed to render; the
 * {@code Mouse.isButtonDown} poll runs only while a drag is active.
 */
public class RavenSlider extends RavenRow {

    private final RavenModule parent;
    private final Property<?> property;
    private final float min;
    private final float max;
    private final boolean integral;
    private final String suffix;

    private boolean dragging;
    private float shownValue = Float.NaN;
    private String shownText = "";
    private int shownWidth;

    public RavenSlider(RavenModule parent, Property<?> property) {
        this.parent = parent;
        this.property = property;
        if (property instanceof IntProperty) {
            this.min = ((IntProperty) property).getMinimum();
            this.max = ((IntProperty) property).getMaximum();
            this.integral = true;
            this.suffix = "";
        } else if (property instanceof PercentProperty) {
            this.min = ((PercentProperty) property).getMinimum();
            this.max = ((PercentProperty) property).getMaximum();
            this.integral = true;
            this.suffix = "%";
        } else {
            this.min = ((FloatProperty) property).getMinimum();
            this.max = ((FloatProperty) property).getMaximum();
            this.integral = false;
            this.suffix = "";
        }
    }

    @Override
    public int height() {
        return RavenTheme.SLIDER_H;
    }

    @Override
    public boolean visible() {
        return this.property.isVisible();
    }

    private float current() {
        Object v = this.property.getValue();
        return v instanceof Number ? ((Number) v).floatValue() : this.min;
    }

    private void refreshText(FontRenderer fr, float value) {
        if (value != this.shownValue) {
            this.shownValue = value;
            this.shownText = (this.integral ? Integer.toString(Math.round(value)) : trimFloat(value)) + this.suffix;
            this.shownWidth = fr.getStringWidth(this.shownText);
        }
    }

    /** Two decimals without a BigDecimal allocation. Sign-safe for (-1, 0). */
    private static String trimFloat(float value) {
        int rounded = Math.round(value * 100.0F);
        if (rounded == 0) {
            return "0";
        }
        String sign = rounded < 0 ? "-" : "";
        int abs = Math.abs(rounded);
        int whole = abs / 100;
        int frac = abs % 100;
        if (frac == 0) {
            return sign + whole;
        }
        if (frac % 10 == 0) {
            return sign + whole + "." + (frac / 10);
        }
        return sign + whole + "." + (frac < 10 ? "0" + frac : Integer.toString(frac));
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        this.w = this.parent.rowWidth();
        this.h = RavenTheme.SLIDER_H;

        if (this.dragging) {
            if (Mouse.isButtonDown(0)) {
                dragTo(mouseX);
            } else {
                this.dragging = false;
            }
        }

        float value = current();
        refreshText(fr, value);
        float range = this.max - this.min;
        float fill = range <= 0.0F ? 0.0F : (value - this.min) / range;
        if (fill < 0.0F) {
            fill = 0.0F;
        } else if (fill > 1.0F) {
            fill = 1.0F;
        }

        fr.drawString(this.property.getName(), (float) (this.x + 4),
                (float) (this.y + 1), RavenTheme.TEXT_DIM, false);
        fr.drawString(this.shownText, (float) (this.x + this.w - 4 - this.shownWidth),
                (float) (this.y + 1), accent, false);

        int trackY = this.y + this.h - 5;
        int trackX = this.x + 4;
        int trackW = this.w - 8;
        Gui.drawRect(trackX, trackY, trackX + trackW, trackY + 3, RavenTheme.TRACK_BG);
        int fillW = Math.round(trackW * fill);
        if (fillW > 0) {
            Gui.drawRect(trackX, trackY, trackX + fillW, trackY + 3, accent);
        }
        int knobX = trackX + fillW - 1;
        Gui.drawRect(knobX, trackY - 1, knobX + 3, trackY + 4, RavenTheme.TEXT);
    }

    private void dragTo(int mouseX) {
        int trackX = this.x + 4;
        int trackW = this.w - 8;
        if (trackW <= 0) {
            return;
        }
        float f = (float) (mouseX - trackX) / (float) trackW;
        if (f < 0.0F) {
            f = 0.0F;
        } else if (f > 1.0F) {
            f = 1.0F;
        }
        float value = this.min + (this.max - this.min) * f;
        if (this.integral) {
            this.property.setValue(Integer.valueOf(Math.round(value)));
        } else {
            this.property.setValue(Float.valueOf(Math.round(value * 100.0F) / 100.0F));
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY)) {
            this.dragging = true;
            dragTo(mouseX);
            return true;
        }
        return false;
    }

    public boolean wheel(int amount) {
        if (this.integral) {
            int v = Math.round(current());
            this.property.setValue(Integer.valueOf((int) Math.max(this.min, Math.min(this.max, v + (amount > 0 ? 1 : -1)))));
        } else {
            float v = current();
            float next = Math.round((v + (amount > 0 ? 0.05F : -0.05F)) * 100.0F) / 100.0F;
            this.property.setValue(Float.valueOf(Math.max(this.min, Math.min(this.max, next))));
        }
        return true;
    }

    public boolean hovering(int mouseX, int mouseY) {
        return inside(mouseX, mouseY);
    }

    @Override
    public void release(int mouseX, int mouseY, int button) {
        this.dragging = false;
    }

    @Override
    public void closed() {
        this.dragging = false;
    }
}
