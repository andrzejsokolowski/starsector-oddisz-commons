package oddiszcommons.manage;

import java.awt.Color;

import com.fs.starfarer.api.Global;

/**
 * The Manage button on the campaign bottom bar. Other mods add one page each:
 *
 * <pre>
 * public void onApplicationLoad() {
 *     Manage.addPage("missions_renewed", "Missions", new MissionsPage());
 * }
 * </pre>
 *
 * Add the page once, in {@code onApplicationLoad}. It stays for the whole game session, across
 * save loads. The button appears as soon as one page exists.
 */
public final class Manage {

    /** The most pages the screen holds. Each one needs its own entry point the host can call. */
    public static final int MAX_PAGES = Slots.COUNT;

    private Manage() {
    }

    /** Adds a page in the player's own interface colour. Returns false when the page was not added. */
    public static boolean addPage(String modId, String tabName, ManagePage page) {
        return addPage(modId, tabName, null, page);
    }

    /**
     * Adds a page with a coloured tab. A second call with the same mod id replaces the page.
     * Returns false when the page was not added (all {@link #MAX_PAGES} places are taken).
     */
    public static boolean addPage(String modId, String tabName, Color tabColor, ManagePage page) {
        if (modId == null || modId.isEmpty() || tabName == null || page == null) {
            throw new IllegalArgumentException("OddiszCommons: a page needs a mod id, a tab name and a page");
        }
        boolean added = PageRegistry.add(modId, tabName, tabColor, page);
        if (!added) {
            Global.getLogger(Manage.class).warn("OddiszCommons: the Manage screen already holds "
                    + MAX_PAGES + " pages, so the page of " + modId + " was not added.");
        }
        return added;
    }

    /**
     * Removes a page. When another mod's Manage button is in charge this session, its tab stays
     * there until the next load.
     */
    public static void removePage(String modId) {
        PageRegistry.remove(modId);
    }

    /** True when a page with this mod id is registered. */
    public static boolean hasPage(String modId) {
        return PageRegistry.get(modId) != null;
    }

    /**
     * Opens the Manage screen on this mod's page, as if the player clicked the button.
     * Returns false when the screen cannot open right now (no campaign, a menu or dialog in the way).
     */
    public static boolean openPage(String modId) {
        return ManageBar.openOn(modId);
    }
}
