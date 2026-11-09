package oddiszcommons.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.StatBonus;

/**
 * Every change a mod makes to the game, with the way to undo it.
 *
 * <p>A mod re-asserts, on each pass (usually each frame), every change that should exist right now.
 * {@link #sweep()} then undoes whatever was not re-asserted since the previous sweep: the core was
 * pulled, the building was lost, the colony changed hands, a setting was switched off. Nothing has to
 * remember to take its own change off.
 *
 * <p>A change that names its colony marks that colony when its value really moves (or when it is
 * undone). A colony's buildings copy colony-wide values when they are applied, so a marked colony
 * needs its buildings applied again. By default {@link #sweep()} does that once the pass is over.
 * A ledger made with {@code reapplyColonies = false} only collects them for
 * {@link #takeChangedColonies()}, for a mod that hands them to someone else.
 *
 * <p>{@link #releaseForSave()} undoes everything, so a save never holds a trace of the mod. The next
 * pass puts it all back.
 *
 * <p>Undoing a change and applying a colony again never throw: an error is written to starsector.log
 * once per kind of failure, and the ledger goes on.
 *
 * <p>Not thread-safe. Use one ledger from the game thread.
 */
public final class Ledger {

    private static final Logger log = Logger.getLogger(Ledger.class);
    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private static final class Hold {
        final Runnable release;
        final MarketAPI market;
        int stamp;

        Hold(Runnable release, MarketAPI market) {
            this.release = release;
            this.market = market;
        }
    }

    private final String owner;
    private final boolean reapplyColonies;
    private final Map<Object, Map<String, Hold>> holds = new IdentityHashMap<>();
    private final Map<MarketAPI, Boolean> changed = new IdentityHashMap<>();
    private float threshold;
    private int stamp = 1;

    /**
     * A ledger that applies changed colonies again after each sweep.
     *
     * @param owner the mod's name, used in starsector.log
     */
    public Ledger(String owner) {
        this(owner, true);
    }

    /**
     * @param owner the mod's name, used in starsector.log
     * @param reapplyColonies true to apply changed colonies again after each sweep; false to only
     *        collect them for {@link #takeChangedColonies()}
     */
    public Ledger(String owner, boolean reapplyColonies) {
        this.owner = owner == null ? "A mod" : owner;
        this.reapplyColonies = reapplyColonies;
    }

    /**
     * How far (relative) a value must move before its colony counts as changed. 0, the default, counts
     * every move. A small value such as 0.001 ignores rounding noise from one pass to the next.
     *
     * @return this ledger
     */
    public Ledger setChangeThreshold(float relative) {
        threshold = Math.max(0f, relative);
        return this;
    }

    // ---- Stat modifiers ----

    public void mult(MutableStat stat, String id, float value, String desc) {
        mult(stat, id, value, desc, null);
    }

    /** Multiplies {@code stat} by {@code value} under {@code id}. A move marks {@code market} (may be null). */
    public void mult(MutableStat stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getMultStatMod(id);
        if (needsWrite(before, value, desc)) {
            if (moved(before, 1f, value)) touch(market);
            stat.modifyMult(id, value, desc);
        }
        keep(stat, "mult:" + id, market, () -> stat.unmodifyMult(id));
    }

    public void flat(MutableStat stat, String id, float value, String desc) {
        flat(stat, id, value, desc, null);
    }

    /** Adds {@code value} to {@code stat} under {@code id}. A move marks {@code market} (may be null). */
    public void flat(MutableStat stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getFlatStatMod(id);
        if (needsWrite(before, value, desc)) {
            if (moved(before, 0f, value)) touch(market);
            stat.modifyFlat(id, value, desc);
        }
        keep(stat, "flat:" + id, market, () -> stat.unmodifyFlat(id));
    }

    public void mult(StatBonus stat, String id, float value, String desc) {
        mult(stat, id, value, desc, null);
    }

    public void mult(StatBonus stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getMultBonus(id);
        if (needsWrite(before, value, desc)) {
            if (moved(before, 1f, value)) touch(market);
            stat.modifyMult(id, value, desc);
        }
        keep(stat, "mult:" + id, market, () -> stat.unmodifyMult(id));
    }

    public void flat(StatBonus stat, String id, float value, String desc) {
        flat(stat, id, value, desc, null);
    }

    public void flat(StatBonus stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getFlatBonus(id);
        if (needsWrite(before, value, desc)) {
            if (moved(before, 0f, value)) touch(market);
            stat.modifyFlat(id, value, desc);
        }
        keep(stat, "flat:" + id, market, () -> stat.unmodifyFlat(id));
    }

    // ---- Anything else ----

    /**
     * A change that is not a stat modifier, already made by the caller. {@code release} undoes it.
     * Only the first {@code release} given for a {@code target} and {@code id} is kept.
     */
    public void hold(Object target, String id, Runnable release) {
        keep(target, "hold:" + id, null, release);
    }

    /** Like {@link #hold(Object, String, Runnable)}; undoing it also marks {@code market}. */
    public void hold(Object target, String id, MarketAPI market, Runnable release) {
        keep(target, "hold:" + id, market, release);
    }

    /** Marks a colony as changed by something the ledger cannot see. Null is ignored. */
    public void touch(MarketAPI market) {
        if (market != null) changed.put(market, Boolean.TRUE);
    }

    // ---- Passes ----

    /** Undoes whatever was not re-asserted since the last sweep, then deals with changed colonies. */
    public void sweep() {
        release(false);
        stamp++;
        if (reapplyColonies) reapplyChanged();
    }

    /** Undoes everything, then deals with changed colonies like {@link #sweep()}. */
    public void releaseAll() {
        release(true);
        stamp++;
        if (reapplyColonies) reapplyChanged();
    }

    /**
     * Undoes everything before a save, and forgets the changed colonies. Colonies are not applied
     * again: by the time a mod's beforeGameSave runs, the game has already emptied every building's
     * income, supply and demand, and applying a colony then crashes the save. The game applies every
     * colony itself once the save is written.
     */
    public void releaseForSave() {
        release(true);
        stamp++;
        changed.clear();
    }

    /** Colonies changed since the last call, in no set order. The list is emptied by the call. */
    public List<MarketAPI> takeChangedColonies() {
        List<MarketAPI> out = new ArrayList<>(changed.keySet());
        changed.clear();
        return out;
    }

    /** How many changes the ledger holds right now. */
    public int size() {
        int n = 0;
        for (Map<String, Hold> byId : holds.values()) n += byId.size();
        return n;
    }

    public boolean isEmpty() {
        return holds.isEmpty();
    }

    // ---- Inside ----

    /**
     * Rewrites an entry only when its value or text differs. A rewrite of an equal entry gains nothing,
     * and skipping it keeps the stat's maps still while other threads (Theory of Toolbox's workers)
     * may be reading them.
     */
    private static boolean needsWrite(StatMod before, float value, String desc) {
        return before == null || before.getValue() != value || !Objects.equals(desc, before.getDesc());
    }

    private boolean moved(StatMod before, float neutral, float after) {
        return differs(before == null ? neutral : before.getValue(), after, threshold);
    }

    /** Whether {@code after} is more than {@code threshold} (relative) away from {@code before}. */
    static boolean differs(float before, float after, float threshold) {
        if (before == after) return false;
        if (threshold <= 0f) return true;
        float scale = Math.max(Math.abs(before), Math.abs(after));
        if (scale == 0f) return false;
        return Math.abs(after - before) / scale > threshold;
    }

    private void keep(Object target, String key, MarketAPI market, Runnable release) {
        Map<String, Hold> byId = holds.get(target);
        if (byId == null) {
            byId = new HashMap<>();
            holds.put(target, byId);
        }
        Hold hold = byId.get(key);
        if (hold == null) {
            hold = new Hold(release, market);
            byId.put(key, hold);
        }
        hold.stamp = stamp;
    }

    private void release(boolean all) {
        List<Hold> stale = new ArrayList<>();
        Iterator<Map<String, Hold>> targets = holds.values().iterator();
        while (targets.hasNext()) {
            Map<String, Hold> byId = targets.next();
            Iterator<Hold> it = byId.values().iterator();
            while (it.hasNext()) {
                Hold hold = it.next();
                if (all || hold.stamp != stamp) {
                    stale.add(hold);
                    it.remove();
                }
            }
            if (byId.isEmpty()) targets.remove();
        }
        for (Hold hold : stale) {
            try {
                if (hold.release != null) hold.release.run();
            } catch (Throwable t) {
                failed("undoing a change", t);
            }
            touch(hold.market);
        }
    }

    private void reapplyChanged() {
        if (changed.isEmpty()) return;
        List<MarketAPI> markets = takeChangedColonies();
        // Another mod's building can fail while its colony is applied: that must not crash here either.
        for (MarketAPI market : markets) {
            try {
                market.reapplyIndustries();
            } catch (Throwable t) {
                failed("applying a colony's buildings again", t);
            }
        }
    }

    /** Logs once per mod and place. Passes on only errors the Java machine cannot survive. */
    private void failed(String where, Throwable t) {
        if (t instanceof VirtualMachineError && !(t instanceof StackOverflowError)) throw (VirtualMachineError) t;
        if (reported.add(owner + "|" + where)) {
            log.error(owner + ": " + where + " failed; the ledger skipped it instead of crashing the game", t);
        }
    }
}
