package myau.ui.impl.clickgui.raven;

import myau.Myau;
import myau.module.Category;
import myau.module.Module;
import myau.module.ModuleCategories;
import myau.module.modules.ClickGUIModule;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kclient-inspired ClickGUI for Myau: one draggable column per category, plain
 * quad rendering, instant expand/collapse and scrolling, plus a search field
 * (this client has over a hundred modules; remembering which category
 * something is filed under is the actual obstacle).
 *
 * Optimizations over both previous menus and over Kclient itself:
 * <ul>
 *   <li>No shaders at all -- no shadow pass, no rounded-rect shader, no blur.
 *       Every background is one {@code Gui.drawRect} quad.</li>
 *   <li>No per-frame allocations in the hot path: colours are precomputed
 *       ints, the accent is resolved only when its index changes, value texts
 *       and label widths are cached and refreshed only on change, and no
 *       {@code ScaledResolution} is built per frame ({@code width}/{@code height}
 *       are already scaled).</li>
 *   <li>No animation timers ticking on every row: expand/collapse, hover and
 *       scroll apply instantly. There is nothing to ease per frame.</li>
 *   <li>Heights are plain integer sums over the visible rows -- no animation
 *       state, no per-row easing timers -- rows outside the visible body are
 *       culled, and a category that is closed (or has no search match) draws
 *       one header and returns.</li>
 *   <li>The search field is a vanilla {@code GuiTextField}, so typing,
 *       cursor and focus come from the game, not custom text code.</li>
 * </ul>
 */
public class RavenClickGui extends GuiScreen {

    private static RavenClickGui instance;
    /** Session-persistent columns: drag positions, open states and scroll survive reopening. */
    private static List<RavenCategory> categories;
    /** Module registry size when the columns were built; scripts can add modules at runtime. */
    private static int builtModuleCount = -1;

    private GuiTextField search;
    private String needle = "";
    private int accent = RavenTheme.HEADER_BG;
    private int lastAccentIdx = Integer.MIN_VALUE;
    private Module invWalkModule;

    private static KeyBinding[] invWalkKeys;

    private RavenClickGui() {
    }

    public static RavenClickGui getInstance() {
        if (instance == null) {
            instance = new RavenClickGui();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
        categories = null;
        builtModuleCount = -1;
    }

    private static void ensureBuilt() {
        int count = Myau.moduleManager.modules.size();
        if (categories != null && builtModuleCount == count) {
            return;
        }
        List<RavenCategory> fresh = new ArrayList<RavenCategory>();
        for (Category category : Category.values()) {
            List<Module> modules = ModuleCategories.modulesIn(category, Myau.moduleManager.modules);
            if (modules.isEmpty()) {
                continue;
            }
            fresh.add(new RavenCategory(category, modules));
        }
        // Keep the player's layout across rebuilds (a script module appearing
        // must not throw every column back to the top-left).
        if (categories != null) {
            for (RavenCategory next : fresh) {
                for (RavenCategory prev : categories) {
                    if (prev.name().equals(next.name())) {
                        next.x = prev.x;
                        next.y = prev.y;
                        next.open = prev.open;
                        next.scroll = prev.scroll;
                        next.placed = prev.placed;
                        break;
                    }
                }
            }
        }
        categories = fresh;
        builtModuleCount = count;
    }

    private void flowLayout() {
        int cx = 5;
        int cy = RavenTheme.TOP_PAD;
        int rowBottom = cy;
        for (RavenCategory category : categories) {
            if (!category.placed) {
                if (cx + RavenTheme.CAT_W > this.width - 5) {
                    cx = 5;
                    cy = rowBottom + RavenTheme.CAT_GAP;
                }
                category.x = cx;
                category.y = cy;
                category.placed = true;
                cx += RavenTheme.CAT_W + RavenTheme.CAT_GAP;
                if (cy + RavenTheme.HEADER_H + 40 > rowBottom) {
                    rowBottom = cy + RavenTheme.HEADER_H + 40;
                }
            }
            if (category.x > this.width - RavenTheme.CAT_W) {
                category.x = Math.max(0, this.width - RavenTheme.CAT_W);
            }
            if (category.y > this.height - RavenTheme.HEADER_H) {
                category.y = Math.max(0, this.height - RavenTheme.HEADER_H);
            }
            if (category.x < 0) {
                category.x = 0;
            }
            if (category.y < 0) {
                category.y = 0;
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        ensureBuilt();
        flowLayout();
        FontRenderer fr = this.fontRendererObj;
        this.search = new GuiTextField(0, fr, 5, 5, RavenTheme.SEARCH_W, RavenTheme.SEARCH_H);
        this.search.setMaxStringLength(24);
        this.search.setEnableBackgroundDrawing(false);
        this.search.setTextColor(RavenTheme.TEXT);
        this.needle = "";
        this.invWalkModule = Myau.moduleManager.getModule("InvWalk");
    }

    private int resolveAccent() {
        ClickGUIModule gui = (ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");
        int idx = gui == null ? 0 : gui.accentColor.getValue();
        if (idx != this.lastAccentIdx) {
            this.lastAccentIdx = idx;
            this.accent = gui == null ? RavenTheme.HEADER_BG : gui.getAccentColor().getRGB();
        }
        return this.accent;
    }

    private String currentNeedle() {
        if (this.search == null) {
            return "";
        }
        return this.search.getText().toLowerCase(Locale.ROOT).trim();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        String next = currentNeedle();
        if (!next.equals(this.needle)) {
            this.needle = next;
        }
        int accentNow = resolveAccent();

        Gui.drawRect(5 - 2, 5 - 2, 5 + RavenTheme.SEARCH_W + 2, 5 + RavenTheme.SEARCH_H + 2,
                RavenTheme.SEARCH_BG);
        this.search.drawTextBox();
        if (this.search.getText().isEmpty() && !this.search.isFocused()) {
            this.fontRendererObj.drawString("Search...", 9.0F, 8.0F, RavenTheme.TEXT_FAINT, false);
        }

        // HUD Editor button: Atlas's header held the only entry point to the
        // HUD editor, so Raven keeps one next to the search box (Kclient does
        // the same with its HUD editor link).
        int hudX = 5 + RavenTheme.SEARCH_W + 6;
        boolean hudHover = mouseX >= hudX && mouseX < hudX + RavenTheme.HUD_BTN_W
                && mouseY >= 5 && mouseY < 5 + RavenTheme.SEARCH_H;
        Gui.drawRect(hudX, 5, hudX + RavenTheme.HUD_BTN_W, 5 + RavenTheme.SEARCH_H,
                hudHover ? RavenTheme.SETTING_HOVER : RavenTheme.SEARCH_BG);
        Gui.drawRect(hudX, 5 + RavenTheme.SEARCH_H - 1, hudX + RavenTheme.HUD_BTN_W,
                5 + RavenTheme.SEARCH_H, accentNow);
        String hudLabel = "HUD";
        int hudW = this.fontRendererObj.getStringWidth(hudLabel);
        this.fontRendererObj.drawString(hudLabel, (float) (hudX + (RavenTheme.HUD_BTN_W - hudW) / 2),
                8.0F, RavenTheme.TEXT_DIM, false);

        for (RavenCategory category : categories) {
            if (!this.needle.isEmpty() && category.filtered(this.needle).isEmpty()) {
                continue;
            }
            if (category.x + RavenTheme.CAT_W < 0 || category.x > this.width
                    || category.y > this.height) {
                continue;
            }
            category.render(this.fontRendererObj, mouseX, mouseY, accentNow, this.needle,
                    this.width, this.height);
        }

        if (this.invWalkModule != null && this.invWalkModule.isEnabled()) {
            handleInvWalk();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** KeyBinding references are stable; caching the array avoids one allocation per frame. */
    private void handleInvWalk() {
        if (invWalkKeys == null) {
            invWalkKeys = new KeyBinding[] {
                    mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
                    mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight,
                    mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint,
                    mc.gameSettings.keyBindSneak
            };
        }
        for (KeyBinding key : invWalkKeys) {
            KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getX() * this.width / mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / mc.displayHeight - 1;
            for (int i = categories.size() - 1; i >= 0; i--) {
                if (categories.get(i).wheel(mouseX, mouseY, wheel, this.needle, this.height)) {
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.search != null) {
            this.search.mouseClicked(mouseX, mouseY, mouseButton);
            if (this.search.isFocused()) {
                // Typing must go to the search box, not to a setting's text row.
                unfocusRows();
            }
        }
        if (mouseButton == 0) {
            int hudX = 5 + RavenTheme.SEARCH_W + 6;
            if (mouseX >= hudX && mouseX < hudX + RavenTheme.HUD_BTN_W
                    && mouseY >= 5 && mouseY < 5 + RavenTheme.SEARCH_H) {
                mc.displayGuiScreen(new myau.ui.hud.HudEditorScreen(this));
                return;
            }
        }
        for (int i = categories.size() - 1; i >= 0; i--) {
            RavenCategory category = categories.get(i);
            if (category.click(mouseX, mouseY, mouseButton, this.needle, this.height)) {
                if (category.overHeader(mouseX, mouseY) && mouseButton == 0) {
                    categories.remove(i);
                    categories.add(category);
                }
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        for (RavenCategory category : categories) {
            category.release();
        }
    }

    private void unfocusRows() {
        for (RavenCategory category : categories) {
            category.unfocusText();
        }
    }

    private boolean anyBinding() {
        for (RavenCategory category : categories) {
            if (category.anyBinding()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyTextFocused() {
        for (RavenCategory category : categories) {
            if (category.anyTextFocused()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.search != null && this.search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.search.setFocused(false);
                return;
            }
            this.search.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (anyBinding() || anyTextFocused()) {
            for (RavenCategory category : categories) {
                if (category.key(typedChar, keyCode, this.needle)) {
                    return;
                }
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                unfocusRows();
                for (RavenCategory category : categories) {
                    category.closed();
                }
            }
            return;
        }
        Module clickGui = Myau.moduleManager.getModule("ClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGui != null && keyCode == clickGui.getKey())) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        for (RavenCategory category : categories) {
            category.closed();
        }
        if (this.search != null) {
            this.search.setFocused(false);
        }
        Module guiModule = Myau.moduleManager.getModule("ClickGUI");
        if (guiModule instanceof ClickGUIModule && ((ClickGUIModule) guiModule).isSwitchingGuiStyle()) {
            return;
        }
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }
}
