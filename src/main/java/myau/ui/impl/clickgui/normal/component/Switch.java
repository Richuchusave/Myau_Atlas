package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.BooleanProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

public class Switch extends Component {
    private final BooleanProperty booleanProperty;
    private float toggleAnim;

    public Switch(BooleanProperty booleanProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.booleanProperty = booleanProperty;
        this.toggleAnim = booleanProperty.getValue() ? 1.0f : 0.0f;
    }

    public BooleanProperty getProperty() {
        return this.booleanProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!booleanProperty.isVisible()) {
            return;
        }

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        float target = booleanProperty.getValue() ? 1.0f : 0.0f;
        this.toggleAnim = AnimationUtil.animateSmooth(target, this.toggleAnim, 15.0f, deltaTime);
        int textColor = MaterialTheme.withAlpha(MaterialTheme.TEXT, alpha);

        float textY = scrolledY + (height - 8) / 2f;
        if (FontManager.productSans16 != null) {
            FontManager.productSans16.drawString(booleanProperty.getName(), x + 2, textY, textColor);
        } else {
            mc.fontRendererObj.drawStringWithShadow(booleanProperty.getName(), x + 2, scrolledY + 6, textColor);
        }

        int switchW = 22;
        int switchH = 12;
        int switchX = x + width - switchW - 2;
        int switchY = scrolledY + (height - switchH) / 2;

        int switchColor = AnimationUtil.interpolateColor(MaterialTheme.SWITCH_OFF, MaterialTheme.PRIMARY, toggleAnim);

        if (alpha < 255) {
            switchColor = MaterialTheme.withAlpha(switchColor, alpha);
        }

        int knobX = switchX + (int) (toggleAnim * (switchW - switchH));
        RenderUtil.drawRoundedRect(switchX, switchY, switchW, switchH, switchH / 2f, switchColor, true, true, true, true);
        RenderUtil.drawRoundedRect(knobX, switchY + 1, switchH - 2, switchH - 2, (switchH - 2) / 2f, -1, true, true, true, true);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height) {
            booleanProperty.setValue(!booleanProperty.getValue());
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}
