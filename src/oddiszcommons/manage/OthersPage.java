package oddiszcommons.manage;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The Others tab (1.1.0): one tab on the Manage screen that small mods share, so they do not each take
 * one of its {@link Manage#MAX_PAGES} places. A list of the mods runs down the left and the picked mod's
 * page fills the rest. Mods join it with {@link Manage#addOtherPage}; their pages are ordinary
 * {@link ManagePage}s, built at the size left of the list.
 *
 * <p>The tab itself is an ordinary page of the registry under {@link Manage#OTHERS_ID}, added with the
 * first mod that joins and removed with the last one that leaves. It names no hidden game class.
 */
final class OthersPage implements ManagePage {

    static final OthersPage INSTANCE = new OthersPage();

    private static final float LIST_W = 240f;
    private static final float GAP = 12f;
    private static final float ROW_H = 30f;
    private static final String BUTTON_PREFIX = "oddisz_commons_others:";

    static final class Member {
        final String modId;
        final String name;
        final ManagePage page;

        Member(String modId, String name, ManagePage page) {
            this.modId = modId;
            this.name = name;
            this.page = page;
        }
    }

    private final List<Member> members = new ArrayList<>();

    /** The mod picked in the list, kept while the game runs so the tab reopens where it was left. */
    private String selected;

    /** The page on screen right now, told when it goes away. */
    private Member open;

    private OthersPage() {
    }

    // ---- Members --------------------------------------------------------------------------------

    synchronized void add(String modId, String name, ManagePage page) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).modId.equals(modId)) {
                members.set(i, new Member(modId, name, page));
                return;
            }
        }
        members.add(new Member(modId, name, page));
    }

    synchronized void remove(String modId) {
        members.removeIf(m -> m.modId.equals(modId));
        if (modId.equals(selected)) {
            selected = null;
        }
    }

    synchronized boolean has(String modId) {
        return find(modId) != null;
    }

    synchronized boolean isEmpty() {
        return members.isEmpty();
    }

    synchronized List<Member> snapshot() {
        return new ArrayList<>(members);
    }

    synchronized void select(String modId) {
        if (find(modId) != null) {
            selected = modId;
        }
    }

    synchronized String selected() {
        if (find(selected) == null) {
            selected = members.isEmpty() ? null : members.get(0).modId;
        }
        return selected;
    }

    private Member find(String modId) {
        if (modId == null) return null;
        for (Member m : members) {
            if (m.modId.equals(modId)) return m;
        }
        return null;
    }

    private static boolean newsOf(Member m) {
        try {
            return m.page.hasNews();
        } catch (Throwable t) {
            return false;
        }
    }

    // ---- ManagePage -------------------------------------------------------------------------------

    @Override
    public boolean hasNews() {
        for (Member m : snapshot()) {
            if (newsOf(m)) return true;
        }
        return false;
    }

    @Override
    public void panelClosed() {
        closeOpen();
    }

    private void closeOpen() {
        Member was = open;
        open = null;
        if (was != null) {
            try {
                was.page.panelClosed();
            } catch (Throwable t) {
                Global.getLogger(OthersPage.class).warn("OddiszCommons: the Others page of " + was.modId
                        + " failed to close", t);
            }
        }
    }

    @Override
    public CustomPanelAPI createPanel(float width, float height) {
        closeOpen();
        View view = new View(width, height);
        CustomPanelAPI root = Global.getSettings().createCustom(width, height, view);
        view.root = root;
        view.build(selected());
        return root;
    }

    /** The tab on screen: the list and the picked page. Changes wait for the next frame. */
    private final class View extends BaseCustomUIPanelPlugin {
        private final float width;
        private final float height;
        CustomPanelAPI root;
        private CustomPanelAPI list;
        private CustomPanelAPI content;
        private String pending;

        View(float width, float height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void advance(float amount) {
            if (pending != null) {
                String next = pending;
                pending = null;
                select(next);
                build(selected());
            }
        }

        void build(String pick) {
            if (root == null) return;
            if (list != null) root.removeComponent(list);
            if (content != null) root.removeComponent(content);
            list = null;
            content = null;
            closeOpen();

            List<Member> all = snapshot();
            list = Global.getSettings().createCustom(LIST_W, height, new ListPlugin(this));
            TooltipMakerAPI rows = list.createUIElement(LIST_W, height, true);
            rows.addSectionHeading("Others", Alignment.MID, 0f);
            for (Member m : all) {
                boolean news = newsOf(m);
                ButtonAPI row = rows.addAreaCheckbox(m.name, BUTTON_PREFIX + m.modId,
                        news ? Misc.getHighlightColor() : Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), LIST_W - 10f, ROW_H, 6f);
                row.setChecked(m.modId.equals(pick));
            }
            // The list covers its whole panel from the top left corner, or its buttons take no clicks.
            list.addUIElement(rows).inTL(0f, 0f);
            root.addComponent(list).inTL(0f, 0f);

            Member chosen = find(pick);
            float contentW = Math.max(100f, width - LIST_W - GAP);
            CustomPanelAPI built = null;
            if (chosen != null) {
                try {
                    built = chosen.page.createPanel(contentW, height);
                    open = chosen;
                } catch (Throwable t) {
                    Global.getLogger(OthersPage.class).error("OddiszCommons: the Others page of " + chosen.modId
                            + " could not be built", t);
                }
            }
            if (built == null) {
                built = Global.getSettings().createCustom(contentW, height, null);
            }
            content = built;
            root.addComponent(content).inTL(LIST_W + GAP, 0f);
        }

        void picked(String modId) {
            pending = modId;
        }
    }

    /** Takes the list's clicks and hands the pick to the view for the next frame. */
    private static final class ListPlugin extends BaseCustomUIPanelPlugin {
        private final View view;

        ListPlugin(View view) {
            this.view = view;
        }

        @Override
        public void buttonPressed(Object buttonId) {
            if (buttonId instanceof String && ((String) buttonId).startsWith(BUTTON_PREFIX)) {
                view.picked(((String) buttonId).substring(BUTTON_PREFIX.length()));
            }
        }
    }
}
