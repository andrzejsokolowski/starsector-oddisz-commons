package oddiszcommons.manage;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

/**
 * 1.0.2: the parts of the Manage button that work without the bottom bar, so a game update that breaks the bar
 * code ({@link ManageBar}) cannot take them down. Uses only the game's public API.
 * <ul>
 * <li>Mirrors each page's news into the memory flag the Manage screens read.</li>
 * <li>When another mod runs the Manage button (Project Arondight, Captain's Quarters, Liminal, Nomad Flotilla),
 * hands it one tab per page through the agreement's {@code registerTenant}.</li>
 * <li>When no Manage button shows our pages (ours broke, the other one refused them), shows them in the intel
 * screen instead ({@link ManageIntel}).</li>
 * </ul>
 */
public final class ManageJoin implements EveryFrameScript {

    private static final MethodType REGISTER = MethodType.methodType(void.class, String.class, String.class,
            String.class, String.class, int.class, int.class, int.class);
    private static final MethodType NO_ARGS_BOOLEAN = MethodType.methodType(boolean.class);
    private static final MethodType SELECT = MethodType.methodType(void.class, String.class);
    /** How long to wait for some Manage button before showing the pages in the intel screen. */
    private static final float GRACE_SECONDS = 3f;

    private static final Logger LOG = Global.getLogger(ManageJoin.class);

    private static ManageJoin live;

    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
    private String foreignSeam;
    private final Set<String> handedOver = new HashSet<>();
    private boolean foreignFailed;
    private boolean newerContractLogged;
    private boolean foreignDeadLogged;
    private float waited;

    /** Called from the mod plugin on every load, before the first frame, before the bar. */
    public static void onGameLoad() {
        ManageQueue.clear();
        ManageScreen.forgetLastTab();
        // A note left in the save names whoever was in charge last time, maybe a mod that is gone. Every mod in the
        // agreement clears it on load and the first one to find the bar this time writes it again.
        Global.getSector().getMemoryWithoutUpdate().unset(ManageBar.HANDSHAKE_KEY);
        ManageIntel.removeAll();
        if (!Global.getSector().hasTransientScript(ManageJoin.class)) {
            Global.getSector().addTransientScript(new ManageJoin());
        }
    }

    public ManageJoin() {
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
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        try {
            tick(interval.getIntervalDuration());
        } catch (Throwable t) {
            LOG.warn("OddiszCommons: the Manage pages hit an error this frame.", t);
        }
    }

    private void tick(float seconds) {
        if (Global.getSector() == null) {
            return;
        }
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        syncNews(memory);
        Object note = memory.get(ManageBar.HANDSHAKE_KEY);
        if (note instanceof Map && !ManageBar.MOD_ID.equals(((Map<?, ?>) note).get(ManageBar.KEY_HOST))) {
            joinForeign((Map<?, ?>) note);
        }
        if (PageRegistry.isEmpty()) {
            ManageIntel.show(false);
            return;
        }
        if (reachable()) {
            waited = 0f;
            ManageIntel.show(false);
            return;
        }
        waited += seconds;
        if (waited >= GRACE_SECONDS) {
            ManageIntel.show(true);
        }
    }

    /** True when some Manage button on the bottom bar shows every one of our pages. */
    static boolean reachable() {
        try {
            if (ManageBar.isAlive()) {
                return true;
            }
        } catch (Throwable ignored) {
            // The bar code does not fit this game version.
        }
        ManageJoin j = live;
        if (j == null || j.foreignSeam == null || j.foreignFailed) {
            return false;
        }
        for (PageRegistry.Entry e : PageRegistry.all()) {
            if (!j.handedOver.contains(e.modId)) {
                return false;
            }
        }
        return j.foreignAlive();
    }

    /** Asks the other mod's button whether it still works (its isAlive), when it says. */
    private boolean foreignAlive() {
        try {
            boolean alive = (boolean) MethodHandles.publicLookup()
                    .findStatic(Class.forName(foreignSeam), "isAlive", NO_ARGS_BOOLEAN).invoke();
            if (!alive && !foreignDeadLogged) {
                foreignDeadLogged = true;
                LOG.info("OddiszCommons: the Manage button of " + foreignSeam + " is off, so the pages of Oddisz's"
                        + " mods are in the intel screen.");
            }
            return alive;
        } catch (Throwable t) {
            return true;
        }
    }

    /** Mirrors each page's news into the memory flag the Manage screens read. */
    private static void syncNews(MemoryAPI memory) {
        for (PageRegistry.Entry e : PageRegistry.all()) {
            boolean news;
            try {
                news = e.page.hasNews();
            } catch (Throwable t) {
                news = false;
            }
            if (news) {
                memory.set(e.newsKey(), Boolean.TRUE);
            } else if (memory.contains(e.newsKey())) {
                memory.unset(e.newsKey());
            }
        }
    }

    // ---- Another mod is in charge --------------------------------------------------------------------

    private void joinForeign(Map<?, ?> note) {
        Object version = note.get(ManageBar.KEY_VERSION);
        if (!(version instanceof Integer) || (Integer) version > ManageBar.CONTRACT_VERSION) {
            if (!newerContractLogged) {
                newerContractLogged = true;
                LOG.warn("OddiszCommons: another mod runs the Manage button with agreement version " + version
                        + ", this one knows " + ManageBar.CONTRACT_VERSION + ". The pages of Oddisz's mods are in the"
                        + " intel screen this session.");
            }
            foreignFailed = true;
            return;
        }
        Object seam = note.get(ManageBar.KEY_SEAM);
        if (!(seam instanceof String)) {
            return;
        }
        if (!seam.equals(foreignSeam)) {
            foreignSeam = (String) seam;
            handedOver.clear();
            foreignFailed = false;
            foreignDeadLogged = false;
        }
        if (foreignFailed) {
            return;
        }
        List<PageRegistry.Entry> pages = PageRegistry.all();
        for (PageRegistry.Entry e : pages) {
            if (handedOver.contains(e.modId)) {
                continue;
            }
            try {
                handOver(foreignSeam, e);
                handedOver.add(e.modId);
                LOG.info("OddiszCommons: added the page of " + e.modId + " to the Manage button of "
                        + note.get(ManageBar.KEY_HOST) + ".");
            } catch (Throwable t) {
                foreignFailed = true;
                LOG.warn("OddiszCommons: could not add the page of " + e.modId + " to the Manage button of "
                        + note.get(ManageBar.KEY_HOST) + ". The pages of Oddisz's mods are in the intel screen.", t);
                return;
            }
        }
    }

    /** Hands one page to another mod's Manage button, through the agreement's registerTenant. */
    static void handOver(String seamClass, PageRegistry.Entry e) throws Throwable {
        Color c = e.tabColor != null ? e.tabColor : Misc.getBrightPlayerColor();
        MethodHandles.publicLookup().findStatic(Class.forName(seamClass), "registerTenant", REGISTER)
                .invoke(e.modId, e.tabName, e.factoryClass(), e.newsKey(), c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Opens a page on the other mod's button. False when there is none or it refused. */
    static boolean openOnForeign(String modId) {
        ManageJoin j = live;
        if (j == null || j.foreignSeam == null || j.foreignFailed || !j.handedOver.contains(modId)) {
            return false;
        }
        try {
            Class<?> seam = Class.forName(j.foreignSeam);
            MethodHandles.publicLookup().findStatic(seam, "requestTabSelect", SELECT).invoke(modId);
            return (boolean) MethodHandles.publicLookup().findStatic(seam, "openManagementScreen", NO_ARGS_BOOLEAN)
                    .invoke();
        } catch (Throwable t) {
            return false;
        }
    }

    /** For the checks: whether this page went to the other mod's button. */
    static boolean handedOver(String modId) {
        ManageJoin j = live;
        return j != null && j.handedOver.contains(modId);
    }

    /** For the checks: one pass, as a frame would run it. */
    static void tickForCheck(float seconds) {
        ManageJoin j = live;
        if (j != null) {
            j.tick(seconds);
        }
    }

    /** Unused by the game; keeps the intel class reachable for the checks. */
    static Class<? extends IntelInfoPlugin> intelClass() {
        return ManageIntel.class;
    }
}
