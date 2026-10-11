package myau.ui.impl.clickgui.normal;

import java.awt.*;

public class MaterialTheme {
    public static final Color PRIMARY_COLOR = new Color(110, 80, 255);
    public static final Color SURFACE_CONTAINER_HIGH = new Color(45, 45, 50, 220);
    public static final Color TEXT_COLOR = new Color(240, 240, 240);
    public static final Color TEXT_COLOR_SECONDARY = new Color(160, 160, 170);
    public static final Color OUTLINE_COLOR = new Color(80, 80, 90, 100);
    public static final float CORNER_RADIUS_FRAME = 8.0f;

    /* Packed ints, precomputed so the render path never allocates colour
       objects (2026-10-10): new Color(...).getRGB() per row per frame was
       one of the menu's hot spots. */
    public static final int PRIMARY = 0xFF6E50FF;
    public static final int TEXT = 0xFFF0F0F0;
    public static final int TEXT_SECONDARY = 0xFFA0A0AA;
    public static final int FRAME_BG = 0xFF0F0F0F;
    public static final int LIST_BG = 0xFF1C1C1C;
    public static final int HOVER_BG = 0xDC2D2D32;
    public static final int TRACK_BG = 0xFF28282D;
    public static final int SWITCH_OFF = 0xFF3C3C41;
    public static final int ROW_BG = 0x640A0A0C;
    public static final int FIELD_BG = 0xFF1E1E23;
    public static final int FIELD_LINE = 0xFF3C3C41;
    public static final int PLACEHOLDER = 0xFF787878;
    public static final int OUTLINE = 0x6450505A;
    public static final int DROPDOWN_BG = 0xF0141418;

    public static int getRGB(Color color) {
        return color.getRGB();
    }

    public static int getRGBWithAlpha(Color color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }

    /** Re-alpha a packed colour with bit ops only -- no allocation. */
    public static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
