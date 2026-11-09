package oddiszcommons.manage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** The Manage screen: a row of tabs, one per page, and the chosen page below them. */
final class ManageScreen {

    static final float TAB_WIDTH = 130f;
    static final float TAB_HEIGHT = 18f;
    static final float TAB_GAP = 6f;
    static final float CONTENT_TOP = TAB_HEIGHT + 8f;
    private static final float FLASH_EVERY = 0.6f;
    private static final String TAB_PREFIX = "oddisz_commons_tab:";

    /** The tab the player last looked at, so the screen reopens on it. */
    private static String lastTab;

    private final List<Tenant> tenants;
    private final Map<String, ButtonAPI> tabButtons = new LinkedHashMap<>();
    private final float width;
    private final float height;
    private final CustomPanelAPI root;
    private CustomPanelAPI content;
    private Tenant selected;
    private float flashTimer;

    ManageScreen(List<Tenant> tenants, float width, float height) {
        this.tenants = tenants;
        this.width = width;
        this.height = height;
        this.root = Global.getSettings().createCustom(width, height, new Plugin());
        buildTabs();
    }

    static void forgetLastTab() {
        lastTab = null;
    }

    CustomPanelAPI root() {
        return root;
    }

    private void buildTabs() {
        TooltipMakerAPI row = root.createUIElement(width, TAB_HEIGHT + 4f, false);
        int i = 0;
        for (Tenant t : tenants) {
            boolean numbered = i < 9;
            String label = numbered ? "[" + (i + 1) + "] " + t.label : t.label;
            ButtonAPI tab = row.addButton(label, TAB_PREFIX + t.modId,
                    t.color != null ? t.color : Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                    Alignment.MID, CutStyle.TOP, TAB_WIDTH, TAB_HEIGHT, 0f);
            if (numbered) {
                tab.setShortcut(Keyboard.KEY_1 + i, false);
            }
            tab.getPosition().inTL(i * (TAB_WIDTH + TAB_GAP), 2f);
            tabButtons.put(t.modId, tab);
            i++;
        }
        root.addUIElement(row).inTL(0f, 0f);
    }

    /** Opens the requested tab, or the last one the player used, or the first. */
    void selectFirst(String requested) {
        String pick = requested;
        if (pick == null || !tabButtons.containsKey(pick)) {
            pick = lastTab;
        }
        if ((pick == null || !tabButtons.containsKey(pick)) && !tenants.isEmpty()) {
            pick = tenants.get(0).modId;
        }
        select(pick);
    }

    void select(String modId) {
        if (modId == null || !tabButtons.containsKey(modId)
                || (selected != null && selected.modId.equals(modId))) {
            return;
        }
        Tenant next = null;
        for (Tenant t : tenants) {
            if (t.modId.equals(modId)) {
                next = t;
            }
        }
        if (next == null) {
            return;
        }
        dropContent();
        try {
            content = next.build(width, height - CONTENT_TOP);
            if (content != null) {
                root.addComponent(content).inTL(0f, CONTENT_TOP);
            }
        } catch (Throwable t) {
            Global.getLogger(ManageScreen.class).warn("OddiszCommons: the Manage page of " + modId
                    + " could not be built", t);
            content = null;
        }
        selected = next;
        lastTab = modId;
        next.seen();
        for (Map.Entry<String, ButtonAPI> e : tabButtons.entrySet()) {
            boolean on = e.getKey().equals(modId);
            e.getValue().setChecked(on);
            if (on) {
                e.getValue().highlight();
            } else {
                e.getValue().unhighlight();
            }
        }
    }

    /** The screen closed: let the open page let go of its panel. */
    void closed() {
        dropContent();
        selected = null;
    }

    private void dropContent() {
        if (content != null) {
            root.removeComponent(content);
            content = null;
        }
        if (selected != null) {
            selected.release();
        }
    }

    private final class Plugin extends BaseCustomUIPanelPlugin {

        @Override
        public void buttonPressed(Object buttonId) {
            if (buttonId instanceof String && ((String) buttonId).startsWith(TAB_PREFIX)) {
                select(((String) buttonId).substring(TAB_PREFIX.length()));
            }
        }

        @Override
        public void advance(float amount) {
            String queued = ManageBar.takeQueuedSelect();
            if (queued != null) {
                select(queued);
            }
            flashTimer += amount;
            if (flashTimer < FLASH_EVERY) {
                return;
            }
            flashTimer = 0f;
            for (Tenant t : tenants) {
                ButtonAPI tab = tabButtons.get(t.modId);
                if (tab != null && t != selected && t.wantsAttention()) {
                    tab.flash(false, 0.25f, 0.25f);
                }
            }
        }
    }
}
