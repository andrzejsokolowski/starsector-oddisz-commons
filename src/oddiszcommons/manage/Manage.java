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
 * save loads. The button appears as soon as one page exists. When no Manage button on the bottom bar
 * can show the pages, they appear in the intel screen instead (1.0.2).
 *
 * <p>A small mod that needs only a little room joins the shared Others tab instead (1.1.0), so it
 * does not use up one of the {@link #MAX_PAGES} tabs:
 *
 * <pre>
 * Manage.addOtherPage("starterpack", "StarterPack", new SnapshotPage());
 * </pre>
 */
public final class Manage {

    /** The most pages the screen holds. Each one needs its own entry point the host can call. */
    public static final int MAX_PAGES = Slots.COUNT;

    /** The page id of the shared Others tab (1.1.0). */
    public static final String OTHERS_ID = "oddisz_commons_others";

    /** The Others tab's name on the tab strip. */
    public static final String OTHERS_TAB_NAME = "Others";

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

    /**
     * Adds a page to the shared Others tab (1.1.0): one tab for every small mod, with the mods listed
     * down its left side and the picked mod's page beside the list. The page is built at the size left
     * of the list. A second call with the same mod id replaces the page. The Others tab appears with the
     * first page added here. Returns false when the tab could not be added (all {@link #MAX_PAGES} places
     * are taken by other pages).
     */
    public static boolean addOtherPage(String modId, String name, ManagePage page) {
        if (modId == null || modId.isEmpty() || name == null || page == null) {
            throw new IllegalArgumentException("OddiszCommons: an Others page needs a mod id, a name and a page");
        }
        if (PageRegistry.get(OTHERS_ID) == null && !PageRegistry.add(OTHERS_ID, OTHERS_TAB_NAME, null, OthersPage.INSTANCE)) {
            Global.getLogger(Manage.class).warn("OddiszCommons: the Manage screen already holds " + MAX_PAGES
                    + " pages, so there is no room for the Others tab and the page of " + modId + " was not added.");
            return false;
        }
        OthersPage.INSTANCE.add(modId, name, page);
        return true;
    }

    /** Removes a page from the Others tab. The tab goes away with its last page. */
    public static void removeOtherPage(String modId) {
        OthersPage.INSTANCE.remove(modId);
        if (OthersPage.INSTANCE.isEmpty()) {
            PageRegistry.remove(OTHERS_ID);
        }
    }

    /** True when this mod has a page in the Others tab. */
    public static boolean hasOtherPage(String modId) {
        return OthersPage.INSTANCE.has(modId);
    }

    /**
     * Opens the Manage screen on the Others tab with this mod picked. Returns false when the screen
     * cannot open right now, or the mod has no page there.
     */
    public static boolean openOtherPage(String modId) {
        if (!OthersPage.INSTANCE.has(modId)) {
            return false;
        }
        OthersPage.INSTANCE.select(modId);
        return openPage(OTHERS_ID);
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
        try {
            if (ManageBar.openOn(modId)) {
                return true;
            }
        } catch (Throwable ignored) {
            // The bar code no longer fits the game: the fallbacks below still work.
        }
        if (ManageJoin.openOnForeign(modId)) {
            return true;
        }
        return ManageDialog.open(modId);
    }
}
