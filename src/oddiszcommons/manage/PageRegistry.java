package oddiszcommons.manage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;

/** The pages other mods added. Each page owns one of the fixed entry points in {@link Slots}. */
final class PageRegistry {

    static final String NEWS_KEY_PREFIX = "$oddisz_commons_news_";

    static final class Entry {
        final String modId;
        final int slot;
        final String tabName;
        final Color tabColor;
        final ManagePage page;

        Entry(String modId, int slot, String tabName, Color tabColor, ManagePage page) {
            this.modId = modId;
            this.slot = slot;
            this.tabName = tabName;
            this.tabColor = tabColor;
            this.page = page;
        }

        String factoryClass() {
            return Slots.className(slot);
        }

        String newsKey() {
            return NEWS_KEY_PREFIX + modId;
        }
    }

    private static final Entry[] SLOTS = new Entry[Slots.COUNT];

    private PageRegistry() {
    }

    static synchronized boolean add(String modId, String tabName, Color tabColor, ManagePage page) {
        int free = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != null && SLOTS[i].modId.equals(modId)) {
                SLOTS[i] = new Entry(modId, i, tabName, tabColor, page);
                return true;
            }
            if (SLOTS[i] == null && free < 0) {
                free = i;
            }
        }
        if (free < 0) {
            return false;
        }
        SLOTS[free] = new Entry(modId, free, tabName, tabColor, page);
        return true;
    }

    static synchronized void remove(String modId) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != null && SLOTS[i].modId.equals(modId)) {
                SLOTS[i] = null;
            }
        }
    }

    static synchronized Entry get(String modId) {
        for (Entry e : SLOTS) {
            if (e != null && e.modId.equals(modId)) {
                return e;
            }
        }
        return null;
    }

    static synchronized List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : SLOTS) {
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    static synchronized boolean isEmpty() {
        for (Entry e : SLOTS) {
            if (e != null) {
                return false;
            }
        }
        return true;
    }

    private static synchronized Entry inSlot(int slot) {
        return slot >= 0 && slot < SLOTS.length ? SLOTS[slot] : null;
    }

    /** Called by whichever Manage screen is in charge when the tab of this slot opens. */
    static CustomPanelAPI buildSlot(int slot, float width, float height) {
        Entry e = inSlot(slot);
        if (e == null) {
            // The page was removed after its tab was handed to another mod's screen: show nothing.
            return Global.getSettings().createCustom(width, height, null);
        }
        CustomPanelAPI panel = e.page.createPanel(width, height);
        return panel != null ? panel : Global.getSettings().createCustom(width, height, null);
    }

    static void releaseSlot(int slot) {
        Entry e = inSlot(slot);
        if (e != null) {
            e.page.panelClosed();
        }
    }
}
