package oddiszcommons.manage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.ui.newui.S;
import com.fs.starfarer.ui.newui.voidsuper;

/**
 * Puts the Manage button on the campaign bottom bar and shares it with other mods.
 *
 * <p>Project Arondight, Captain's Quarters and Liminal each carry their own Manage button and agree
 * on a single one through a note in sector memory ({@link #HANDSHAKE_KEY}): the first mod that finds
 * the bottom bar adds the button and writes its class name there, and the others hand that class
 * their tabs. OddiszCommons follows the same agreement (version {@link #CONTRACT_VERSION}). When it
 * is in charge, the public static methods below are the ones the other mods call. When another mod
 * is in charge, {@link ManageJoin} (1.0.2: kept apart, so it works even when this class breaks) hands that mod one
 * tab per page.
 *
 * <p>The game classes used here ({@code CampaignState}, the core screen {@code S}, the bar
 * {@code voidsuper}) are the game's own, compiled against 0.98.5a-RC6. The button's own class is
 * never named: it is held as a ButtonAPI or through {@code var}, so a rename of it only needs a
 * rebuild. Any error
 * turns the button off for the session instead of breaking the game.
 */
public final class ManageBar implements EveryFrameScript {

    static final String HANDSHAKE_KEY = "$mgmt_bar";
    static final int CONTRACT_VERSION = 2;
    static final String KEY_HOST = "host";
    static final String KEY_VERSION = "version";
    static final String KEY_SEAM = "seam";

    static final String MOD_ID = "oddisz_commons";
    private static final String BUTTON_LABEL = "Manage (Y)";
    private static final Object BUTTON_ID = "oddisz_commons_manage";
    private static final int SHORTCUT = Keyboard.KEY_Y;
    /** The added plate sits under one button; anything outside this range means the bar changed. */
    private static final float MAX_EDGE = 40f;

    private static final Logger LOG = Global.getLogger(ManageBar.class);

    /** Every tab while OddiszCommons is in charge: our pages and the tabs other mods handed over. */
    private static final Map<String, Tenant> TENANTS = new LinkedHashMap<>();

    private static ManageBar live;
    private static ManageShell openShell;

    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
    private boolean dead;
    private boolean hosting;
    private ButtonAPI flashButton;
    private boolean flashOn;
    private boolean plateSkippedLogged;

    // ---- The agreement: called by other mods while OddiszCommons is in charge -------------------

    /** Another mod hands over a tab. The first registration of a mod id wins. */
    public static synchronized void registerTenant(String modId, String label, String factoryClass,
            String attentionKey, int r, int g, int b) {
        if (modId == null || label == null || factoryClass == null || TENANTS.containsKey(modId)) {
            return;
        }
        TENANTS.put(modId, new Tenant(modId, label, factoryClass, attentionKey, new Color(r, g, b), false));
    }

    /** True while OddiszCommons is in charge of the button and working. */
    public static boolean isAlive() {
        ManageBar l = live;
        return l != null && !l.dead && l.hosting;
    }

    /** Opens this tab the next time the screen shows, or right away if it is open. */
    public static void requestTabSelect(String modId) {
        ManageQueue.request(modId);
    }

    /** Opens the Manage screen, as a click on the button would. */
    public static boolean openManagementScreen() {
        ManageBar l = live;
        return l != null && !l.dead && l.hosting && l.openScreen();
    }

    // ---- For Manage and the screen -----------------------------------------------------------------

    static String takeQueuedSelect() {
        return ManageQueue.take();
    }

    /** Opens our own Manage screen on this page while OddiszCommons is in charge. */
    static boolean openOn(String modId) {
        ManageBar l = live;
        if (l == null || l.dead || !l.hosting) {
            return false;
        }
        requestTabSelect(modId);
        return l.openScreen();
    }

    static synchronized void shellClosed(ManageShell shell) {
        if (openShell == shell) {
            openShell = null;
        }
    }

    static synchronized List<Tenant> tenantsSnapshot() {
        return new ArrayList<>(TENANTS.values());
    }

    /**
     * Called from the mod plugin on every load, before the first frame, after {@link ManageJoin#onGameLoad} (which
     * clears the agreement's note and the queued tab).
     */
    public static void onGameLoad() {
        synchronized (ManageBar.class) {
            TENANTS.clear();
            openShell = null;
        }
        if (!Global.getSector().hasTransientScript(ManageBar.class)) {
            Global.getSector().addTransientScript(new ManageBar());
        }
    }

    // ---- The script --------------------------------------------------------------------------------

    public ManageBar() {
        live = this;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (dead) {
            return;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        try {
            tick();
        } catch (Throwable t) {
            dead = true;
            LOG.warn("OddiszCommons: the Manage button hit an error and is off for this session."
                    + " A game update may have changed the bottom bar.", t);
            hideOwnButton();
        }
    }

    private void tick() {
        if (Global.getSector() == null) {
            return;
        }
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        Object note = memory.get(HANDSHAKE_KEY);
        if (note instanceof Map && !MOD_ID.equals(((Map<?, ?>) note).get(KEY_HOST))) {
            // Another mod runs the button: ManageJoin hands it our pages.
            hosting = false;
            return;
        }
        if (!hosting && PageRegistry.isEmpty()) {
            return;
        }
        host(memory);
    }

    // ---- OddiszCommons is in charge -----------------------------------------------------------------

    private void host(MemoryAPI memory) {
        S core = activeCore();
        if (core == null) {
            return;
        }
        voidsuper bar = core.getButtons();
        if (bar == null) {
            return;
        }
        syncOwnTenants();
        var button = bar.getButton(BUTTON_ID);
        if (button == null) {
            addButton(bar);
            button = bar.getButton(BUTTON_ID);
        }
        if (!hosting || !(memory.get(HANDSHAKE_KEY) instanceof Map)) {
            hosting = true;
            Map<String, Object> note = new HashMap<>();
            note.put(KEY_HOST, MOD_ID);
            note.put(KEY_VERSION, CONTRACT_VERSION);
            note.put(KEY_SEAM, ManageBar.class.getName());
            memory.set(HANDSHAKE_KEY, note);
        }
        updateFlash(button, button.getFlashFader());
    }

    /** Our pages as tabs, kept in step with pages added or removed during the session. */
    private static synchronized void syncOwnTenants() {
        List<PageRegistry.Entry> pages = PageRegistry.all();
        Set<String> current = new HashSet<>();
        for (PageRegistry.Entry e : pages) {
            current.add(e.modId);
            Tenant old = TENANTS.get(e.modId);
            if (old != null && !old.ours) {
                continue; // another mod took this id first
            }
            if (old == null || !old.label.equals(e.tabName) || !old.factoryClass.equals(e.factoryClass())) {
                TENANTS.put(e.modId, new Tenant(e.modId, e.tabName, e.factoryClass(), e.newsKey(), e.tabColor, true));
            }
        }
        for (Iterator<Tenant> it = TENANTS.values().iterator(); it.hasNext(); ) {
            Tenant t = it.next();
            if (t.ours && !current.contains(t.modId)) {
                it.remove();
            }
        }
    }

    /** The core screen whose bar is on screen: the campaign's own, or the one opened while docked. */
    private static S activeCore() {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!(ui instanceof CampaignState)) {
            return null;
        }
        CampaignState state = (CampaignState) ui;
        if (ui.getCurrentInteractionDialog() != null) {
            var dialog = state.getEncounterDialog();
            return dialog == null ? null : dialog.getCoreUI();
        }
        return state.getCore();
    }

    private void addButton(voidsuper bar) {
        float ownEnd = gameButtonsEnd(bar);
        addPlate(bar, ownEnd);
        // The bar places a new button right of its last one and grows to fit it.
        bar.addButton(BUTTON_LABEL, BUTTON_ID, null, (input, source) -> onButtonClicked());
        ButtonAPI button = bar.getButton(BUTTON_ID);
        if (button == null) {
            throw new IllegalStateException("the bottom bar did not keep the Manage button");
        }
        button.setShortcut(SHORTCUT, false);
    }

    /** Where the bar's buttons end, measured from the bar's left edge. */
    private static float gameButtonsEnd(voidsuper bar) {
        PositionAPI barPos = bar.getPosition();
        float end = 0f;
        for (ButtonAPI b : bar.getButtons().values()) {
            PositionAPI p = b.getPosition();
            end = Math.max(end, p.getX() + p.getWidth() - barPos.getX());
        }
        return end;
    }

    private void addPlate(voidsuper bar, float ownEnd) {
        SpriteAPI plate = Global.getSettings().getSprite("ui", "tripad_button_holder");
        float cell = bar.BUTTON_WIDTH + bar.BUTTON_PAD;
        float edge = plate == null ? -1f : plate.getWidth() - ownEnd;
        if (plate == null || ownEnd < cell || edge < 0f || edge > MAX_EDGE) {
            if (!plateSkippedLogged) {
                plateSkippedLogged = true;
                LOG.info("OddiszCommons: the bottom bar is not the expected size (buttons end at " + ownEnd
                        + "), so the Manage button goes on without its backing plate.");
            }
            return;
        }
        CustomPanelAPI panel = Global.getSettings().createCustom(BarPlate.width(cell, edge), plate.getHeight(),
                new BarPlate(plate, ownEnd, cell, edge));
        bar.addComponent(panel).inBL(ownEnd, 0f);
    }

    private void onButtonClicked() {
        try {
            S core = activeCore();
            ButtonAPI button = core == null || core.getButtons() == null ? null : core.getButtons().getButton(BUTTON_ID);
            if (button != null) {
                button.setChecked(false);
            }
            if (core != null && openShell != null && core.getCurrentTab() == openShell) {
                core.dismiss(0);
                return;
            }
            openScreen();
        } catch (Throwable t) {
            LOG.warn("OddiszCommons: the Manage screen could not open.", t);
        }
    }

    private boolean openScreen() {
        try {
            CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
            if (ui == null || ui.isShowingMenu()) {
                return false;
            }
            boolean docked = ui.getCurrentInteractionDialog() != null;
            S core = activeCore();
            if (core == null || core.getButtons() == null) {
                return false;
            }
            var button = core.getButtons().getButton(BUTTON_ID);
            if (button == null) {
                return false;
            }
            if (openShell != null && core.getCurrentTab() == openShell) {
                String pick = takeQueuedSelect();
                if (pick != null) {
                    openShell.screen().select(pick);
                }
                return true;
            }
            // Out in space with some other dialog up: leave it alone.
            if (!docked && ui.isShowingDialog() && ui.getCurrentCoreTab() == null) {
                return false;
            }
            List<Tenant> tenants = tenantsSnapshot();
            if (tenants.isEmpty()) {
                return false;
            }
            float width = Math.min(core.computePanelWidth(true), S.MAX_INTEL_WIDTH);
            float height = Math.min(core.computePanelHeight(true), S.MAX_INTEL_HEIGHT);
            ManageShell shell = new ManageShell(tenants, width, height);
            core.showPanelAsDialog(button, shell, true, false);
            if (core.getCurrentTab() != shell) {
                return false;
            }
            synchronized (ManageBar.class) {
                openShell = shell;
            }
            shell.screen().selectFirst(takeQueuedSelect());
            return true;
        } catch (Throwable t) {
            LOG.warn("OddiszCommons: the Manage screen could not open.", t);
            return false;
        }
    }

    /** The button pulses while any tab has news, like the game's own "Character +". */
    private void updateFlash(ButtonAPI button, Fader fader) {
        if (button != flashButton) {
            flashButton = button;
            flashOn = false;
        }
        boolean want = false;
        for (Tenant t : tenantsSnapshot()) {
            if (t.wantsAttention()) {
                want = true;
                break;
            }
        }
        if (want && !flashOn) {
            button.flash(false, 0.25f, 0.25f);
            if (fader != null) {
                fader.setBounceUp(true);
            }
            flashOn = true;
        } else if (!want && flashOn) {
            if (fader != null) {
                fader.setBounceUp(false);
            }
            flashOn = false;
        }
    }

    private void hideOwnButton() {
        try {
            S core = activeCore();
            ButtonAPI button = core == null || core.getButtons() == null ? null : core.getButtons().getButton(BUTTON_ID);
            if (button != null) {
                button.setOpacity(0f);
                button.setEnabled(false);
            }
        } catch (Throwable ignored) {
            // Already broken: nothing more to do.
        }
    }
}
