package myau.ui.impl.clickgui.raven;

import myau.property.properties.ColorProperty;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Colour setting. Collapsed it is one row (name + swatch + hex); clicking it
 * opens a hue strip plus a saturation/value box underneath.
 *
 * Unlike the old inline picker this renders nothing but the row while closed,
 * and the open picker is two flat gradient quads -- no per-pixel loops, no
 * per-frame HSB recomputation (synced only when the value actually changes).
 */
public class RavenColor extends RavenRow {

    private static final int HUE_H = 10;
    private static final int SV_H = 48;

    private final RavenModule parent;
    private final ColorProperty property;
    private boolean open;

    private float hue;
    private float saturation = 1.0F;
    private float brightness = 1.0F;
    private int synced = Integer.MIN_VALUE;

    private boolean draggingHue;
    private boolean draggingSv;

    private int hueX;
    private int hueY;
    private int hueW;
    private int svX;
    private int svY;
    private int svW;
    private int svH;

    private String shownHex = "#000000";
    private int hexSynced = Integer.MIN_VALUE;
    private int shownWidth;

    public RavenColor(RavenModule parent, ColorProperty property) {
        this.parent = parent;
        this.property = property;
        sync();
    }

    @Override
    public int height() {
        return RavenTheme.SETTING_H + (this.open ? 2 + HUE_H + 2 + SV_H + 2 : 0);
    }

    @Override
    public boolean visible() {
        return this.property.isVisible();
    }

    private void sync() {
        int rgb = this.property.getValue();
        if (rgb != this.synced) {
            this.synced = rgb;
            float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
        }
    }

    private void commit() {
        int rgb = java.awt.Color.HSBtoRGB(this.hue, this.saturation, this.brightness) | 0xFF000000;
        this.property.setValue(rgb);
        this.synced = rgb;
    }

    @Override
    public void render(FontRenderer fr, int mouseX, int mouseY, int accent) {
        sync();
        this.w = this.parent.rowWidth();
        this.h = height();

        if ((this.draggingHue || this.draggingSv) && !Mouse.isButtonDown(0)) {
            this.draggingHue = false;
            this.draggingSv = false;
        }

        fr.drawString(this.property.getName(), (float) (this.x + 4),
                (float) (this.y + (RavenTheme.SETTING_H - 8) / 2 + 1), RavenTheme.TEXT_DIM, false);

        if (this.synced != this.hexSynced) {
            this.hexSynced = this.synced;
            this.shownHex = "#" + String.format("%06X", this.synced & 0xFFFFFF);
            this.shownWidth = fr.getStringWidth(this.shownHex);
        }
        int sw = 10;
        int swX = this.x + this.w - 4 - this.shownWidth - 4 - sw;
        int swY = this.y + 2;
        Gui.drawRect(swX, swY, swX + sw, swY + RavenTheme.SETTING_H - 4, 0xFF000000 | (this.synced & 0xFFFFFF));
        Gui.drawRect(swX, swY, swX + sw, swY + 1, RavenTheme.BOX_LINE);
        Gui.drawRect(swX, swY + RavenTheme.SETTING_H - 5, swX + sw, swY + RavenTheme.SETTING_H - 4, RavenTheme.BOX_LINE);
        Gui.drawRect(swX, swY, swX + 1, swY + RavenTheme.SETTING_H - 4, RavenTheme.BOX_LINE);
        Gui.drawRect(swX + sw - 1, swY, swX + sw, swY + RavenTheme.SETTING_H - 4, RavenTheme.BOX_LINE);
        fr.drawString(this.shownHex, (float) (swX + sw + 4),
                (float) (this.y + (RavenTheme.SETTING_H - 8) / 2 + 1), RavenTheme.TEXT_FAINT, false);

        if (!this.open) {
            return;
        }

        if (this.draggingHue) {
            this.hue = clamp01((float) (mouseX - this.hueX) / (float) this.hueW);
            commit();
        } else if (this.draggingSv) {
            this.saturation = clamp01((float) (mouseX - this.svX) / (float) this.svW);
            this.brightness = clamp01(1.0F - (float) (mouseY - this.svY) / (float) this.svH);
            commit();
        }

        this.hueX = this.x + 4;
        this.hueY = this.y + RavenTheme.SETTING_H + 2;
        this.hueW = this.w - 8;
        drawHueStrip(this.hueX, this.hueY, this.hueW, HUE_H);
        int knobX = this.hueX + Math.round(this.hueW * this.hue);
        Gui.drawRect(knobX - 1, this.hueY - 1, knobX + 1, this.hueY + HUE_H + 1, RavenTheme.TEXT);

        this.svX = this.x + 4;
        this.svY = this.hueY + HUE_H + 2;
        this.svW = this.w - 8;
        this.svH = SV_H;
        int hueRgb = java.awt.Color.HSBtoRGB(this.hue, 1.0F, 1.0F) | 0xFF000000;
        hGradient(this.svX, this.svY, this.svW, this.svH, 0xFFFFFFFF, hueRgb);
        vGradient(this.svX, this.svY, this.svW, this.svH, 0x00000000, 0xFF000000);
        int dotX = this.svX + Math.round(this.svW * this.saturation);
        int dotY = this.svY + Math.round(this.svH * (1.0F - this.brightness));
        Gui.drawRect(dotX - 1, dotY - 1, dotX + 2, dotY + 2, RavenTheme.TEXT);
        Gui.drawRect(dotX - 1, dotY - 1, dotX + 2, dotY, RavenTheme.BOX_LINE);
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (mouseX >= this.x && mouseX < this.x + this.w
                && mouseY >= this.y && mouseY < this.y + RavenTheme.SETTING_H) {
            this.open = !this.open;
            this.draggingHue = false;
            this.draggingSv = false;
            return true;
        }
        if (this.open) {
            if (mouseX >= this.hueX && mouseX < this.hueX + this.hueW
                    && mouseY >= this.hueY - 1 && mouseY < this.hueY + HUE_H + 1) {
                this.draggingHue = true;
                this.draggingSv = false;
                this.hue = clamp01((float) (mouseX - this.hueX) / (float) this.hueW);
                commit();
                return true;
            }
            if (mouseX >= this.svX && mouseX < this.svX + this.svW
                    && mouseY >= this.svY && mouseY < this.svY + this.svH) {
                this.draggingSv = true;
                this.draggingHue = false;
                this.saturation = clamp01((float) (mouseX - this.svX) / (float) this.svW);
                this.brightness = clamp01(1.0F - (float) (mouseY - this.svY) / (float) this.svH);
                commit();
                return true;
            }
        }
        return false;
    }

    @Override
    public void release(int mouseX, int mouseY, int button) {
        this.draggingHue = false;
        this.draggingSv = false;
    }

    @Override
    public void closed() {
        this.draggingHue = false;
        this.draggingSv = false;
    }

    private static final int[] HUE_STOPS = new int[] {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private void drawHueStrip(int x, int y, int w, int h) {
        int segs = HUE_STOPS.length - 1;
        for (int i = 0; i < segs; i++) {
            int sx = x + (w * i) / segs;
            int ex = x + (w * (i + 1)) / segs;
            hGradient(sx, y, ex - sx, h, HUE_STOPS[i], HUE_STOPS[i + 1]);
        }
    }

    private static void hGradient(int x, int y, int w, int h, int left, int right) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float lr = ((left >> 16) & 0xFF) / 255.0F;
        float lg = ((left >> 8) & 0xFF) / 255.0F;
        float lb = (left & 0xFF) / 255.0F;
        float la = ((left >> 24) & 0xFF) / 255.0F;
        float rr = ((right >> 16) & 0xFF) / 255.0F;
        float rg = ((right >> 8) & 0xFF) / 255.0F;
        float rb = (right & 0xFF) / 255.0F;
        float ra = ((right >> 24) & 0xFF) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + w, y, 0.0D).color(rr, rg, rb, ra).endVertex();
        wr.pos(x, y, 0.0D).color(lr, lg, lb, la).endVertex();
        wr.pos(x, y + h, 0.0D).color(lr, lg, lb, la).endVertex();
        wr.pos(x + w, y + h, 0.0D).color(rr, rg, rb, ra).endVertex();
        tess.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void vGradient(int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float tr = ((top >> 16) & 0xFF) / 255.0F;
        float tg = ((top >> 8) & 0xFF) / 255.0F;
        float tb = (top & 0xFF) / 255.0F;
        float ta = ((top >> 24) & 0xFF) / 255.0F;
        float br = ((bottom >> 16) & 0xFF) / 255.0F;
        float bg = ((bottom >> 8) & 0xFF) / 255.0F;
        float bb = (bottom & 0xFF) / 255.0F;
        float ba = ((bottom >> 24) & 0xFF) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + w, y, 0.0D).color(tr, tg, tb, ta).endVertex();
        wr.pos(x, y, 0.0D).color(tr, tg, tb, ta).endVertex();
        wr.pos(x, y + h, 0.0D).color(br, bg, bb, ba).endVertex();
        wr.pos(x + w, y + h, 0.0D).color(br, bg, bb, ba).endVertex();
        tess.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}
