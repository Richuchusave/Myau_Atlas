package myau.ui.impl.clickgui.raven;

/**
 * Shared constants for the Raven ClickGUI.
 *
 * Kclient renders everything with plain {@code Gui.drawRect} quads instead of
 * shader-backed rounded rects; this theme follows that. Every colour here is a
 * precomputed packed int -- drawing a row must never allocate
 * ({@code new Color(...).getRGB()} per row per frame was one of the old
 * menu's hot spots).
 */
public final class RavenTheme {

    // ---- layout (scaled pixels) ----
    public static final int CAT_W = 100;
    public static final int CAT_GAP = 8;
    public static final int HEADER_H = 16;
    public static final int ROW_H = 14;
    public static final int SETTING_H = 12;
    public static final int SLIDER_H = 14;
    public static final int SEARCH_W = 130;
    public static final int SEARCH_H = 14;
    public static final int HUD_BTN_W = 46;
    public static final int TOP_PAD = 24;

    // ---- colours (opaque packed ints) ----
    public static final int HEADER_BG = 0xFF008CFF; // replaced per-frame by the accent
    public static final int BODY_BG = 0xFA0F0F0F;
    public static final int ROW_BG = 0xFF0F0F0F;
    public static final int ROW_HOVER = 0xFF282828;
    public static final int SETTING_BG = 0xFF232323;
    public static final int SETTING_HOVER = 0xFF373737;
    public static final int TRACK_BG = 0xFF1E1E1E;
    public static final int BOX_BG = 0xFF141414;
    public static final int BOX_LINE = 0xFF3C3C3C;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFC0C0C0;
    public static final int TEXT_FAINT = 0xFF808080;
    public static final int SEARCH_BG = 0xFF141414;
    public static final int SEARCH_LINE = 0xFF3C3C3C;

    private RavenTheme() {
    }
}
