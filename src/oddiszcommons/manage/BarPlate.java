package oddiszcommons.manage;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;

/**
 * Extends the bottom bar's backing plate under the added button. The game draws the plate as one
 * fixed image ("tripad_button_holder") sized for its own buttons, so this repeats the image's last
 * button cell once and then its rounded right end.
 */
final class BarPlate extends BaseCustomUIPanelPlugin {

    private final SpriteAPI sprite;
    /** Where the game's own buttons end, in plate image pixels. */
    private final float cellEnd;
    /** One button plus the gap before it. */
    private final float cellWidth;
    /** The rounded end of the image, after the last button. */
    private final float edgeWidth;
    private PositionAPI position;

    BarPlate(SpriteAPI sprite, float cellEnd, float cellWidth, float edgeWidth) {
        this.sprite = sprite;
        this.cellEnd = cellEnd;
        this.cellWidth = cellWidth;
        this.edgeWidth = edgeWidth;
    }

    static float width(float cellWidth, float edgeWidth) {
        return cellWidth + edgeWidth;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) {
            return;
        }
        float imageWidth = sprite.getWidth();
        float x = position.getX();
        float y = position.getY();
        sprite.setAlphaMult(alphaMult);
        // renderRegion draws a slice where it would sit if the whole image started at the given x.
        float cellStart = cellEnd - cellWidth;
        sprite.renderRegion(x - cellStart, y, cellStart / imageWidth, 0f, cellWidth / imageWidth, 1f);
        sprite.renderRegion(x + cellWidth - cellEnd, y, cellEnd / imageWidth, 0f, edgeWidth / imageWidth, 1f);
    }
}
