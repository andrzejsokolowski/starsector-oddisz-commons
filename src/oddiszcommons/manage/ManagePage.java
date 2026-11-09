package oddiszcommons.manage;

import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * One mod's page on the Manage screen. Register it once with {@link Manage#addPage}.
 *
 * <p>The page draws itself: {@link #createPanel} hands back a fresh panel each time its tab opens
 * (make it with {@code Global.getSettings().createCustom(width, height, yourPlugin)}).
 */
public interface ManagePage {

    /** Builds the page for a tab of this size. Called every time the tab is opened. */
    CustomPanelAPI createPanel(float width, float height);

    /** The tab was closed or the player switched to another tab. Drop what the panel holds. */
    default void panelClosed() {
    }

    /**
     * True while the page has something the player has not seen yet. The Manage button and the
     * page's tab then blink. Return false again once the player has looked.
     */
    default boolean hasNews() {
        return false;
    }
}
