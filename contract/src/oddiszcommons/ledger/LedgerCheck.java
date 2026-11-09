package oddiszcommons.ledger;

import java.lang.reflect.Proxy;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;

/** Headless checks of the shared ledger on the game's real stat classes. Never shipped. */
public final class LedgerCheck {

    private static int passed;
    private static int failed;

    /** A colony that only counts how often its buildings are applied again. */
    private static final class FakeColony {
        int reapplied;
        boolean explode;
        final MarketAPI market = (MarketAPI) Proxy.newProxyInstance(LedgerCheck.class.getClassLoader(),
                new Class<?>[] {MarketAPI.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "reapplyIndustries":
                            reapplied++;
                            if (explode) throw new IllegalStateException("a broken building");
                            return null;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "FakeColony";
                        default:
                            return null;
                    }
                });
    }

    public static void main(String[] args) {
        reassertedChangesStay();
        droppedChangesAreUndone();
        colonyIsAppliedOnlyWhenItMoves();
        undoingMarksTheColony();
        collectingLedgerDoesNotApply();
        thresholdIgnoresNoise();
        thresholdCases();
        releaseForSaveUndoesEverythingQuietly();
        releaseAllAppliesColonies();
        multAndFlatWithOneIdAreSeparate();
        statBonusWorks();
        textChangeIsWritten();
        holdRunsItsUndoOnce();
        brokenUndoDoesNotStopTheRest();
        brokenColonyDoesNotCrash();

        System.out.println("Ledger checks: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void reassertedChangesStay() {
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        for (int i = 0; i < 3; i++) {
            ledger.mult(stat, "x", 2f, "doubled");
            ledger.sweep();
        }
        check("re-asserted mult stays", stat.getModifiedValue() == 20f);
        check("ledger holds one change", ledger.size() == 1);
    }

    private static void droppedChangesAreUndone() {
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        ledger.flat(stat, "x", 5f, "plus five");
        ledger.sweep();
        check("flat applied", stat.getModifiedValue() == 15f);
        ledger.sweep();
        check("flat not re-asserted is undone", stat.getModifiedValue() == 10f);
        check("ledger is empty after undo", ledger.isEmpty());
    }

    private static void colonyIsAppliedOnlyWhenItMoves() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(1f);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        check("new value applies the colony once", colony.reapplied == 1);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        check("same value does not apply the colony", colony.reapplied == 1);
        ledger.mult(stat, "x", 3f, "tripled", colony.market);
        ledger.sweep();
        check("changed value applies the colony again", colony.reapplied == 2);
    }

    private static void undoingMarksTheColony() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(1f);
        ledger.flat(stat, "x", 1f, "plus one", colony.market);
        ledger.sweep();
        ledger.sweep();
        check("undo applies the colony", colony.reapplied == 2 && stat.getModifiedValue() == 1f);
    }

    private static void collectingLedgerDoesNotApply() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check", false);
        MutableStat stat = new MutableStat(1f);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        List<MarketAPI> changed = ledger.takeChangedColonies();
        check("collecting ledger reports the colony", changed.size() == 1 && changed.get(0) == colony.market);
        check("collecting ledger does not apply it", colony.reapplied == 0);
        check("taking empties the list", ledger.takeChangedColonies().isEmpty());
        ledger.sweep();
        check("undo is reported too", ledger.takeChangedColonies().size() == 1);
    }

    private static void thresholdIgnoresNoise() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check", false).setChangeThreshold(0.001f);
        MutableStat stat = new MutableStat(1f);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        ledger.takeChangedColonies();
        ledger.mult(stat, "x", 2.0001f, "doubled", colony.market);
        ledger.sweep();
        check("tiny move is not reported", ledger.takeChangedColonies().isEmpty());
        check("tiny move is still written", stat.getMultStatMod("x").getValue() == 2.0001f);
        ledger.mult(stat, "x", 2.5f, "doubled", colony.market);
        ledger.sweep();
        check("real move is reported", ledger.takeChangedColonies().size() == 1);
    }

    /** The cases Toolbox Economy Curves tested before its ledger moved here. */
    private static void thresholdCases() {
        float t = 0.001f;
        check("equal is no change", !Ledger.differs(1.5f, 1.5f, t));
        check("rounding noise is no change", !Ledger.differs(1.5f, 1.5004f, t));
        check("zero to zero is no change", !Ledger.differs(0f, 0f, t));
        check("1 to 1.4 is a change", Ledger.differs(1f, 1.4f, t));
        check("2.74 to 1 is a change", Ledger.differs(2.74f, 1f, t));
        check("0.25 to 0.2 is a change", Ledger.differs(0.25f, 0.2f, t));
        check("no threshold counts any move", Ledger.differs(1.5f, 1.5004f, 0f));
    }

    private static void releaseForSaveUndoesEverythingQuietly() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        int before = colony.reapplied;
        ledger.releaseForSave();
        check("save release undoes everything", stat.getModifiedValue() == 10f && ledger.isEmpty());
        check("save release does not apply colonies", colony.reapplied == before);
        ledger.sweep();
        check("save release forgets changed colonies", colony.reapplied == before);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        check("next pass puts it back", stat.getModifiedValue() == 20f);
    }

    private static void releaseAllAppliesColonies() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        ledger.mult(stat, "x", 2f, "doubled", colony.market);
        ledger.sweep();
        int before = colony.reapplied;
        ledger.releaseAll();
        check("release all undoes and applies the colony", stat.getModifiedValue() == 10f && colony.reapplied == before + 1);
    }

    private static void multAndFlatWithOneIdAreSeparate() {
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        ledger.mult(stat, "x", 2f, "doubled");
        ledger.flat(stat, "x", 5f, "plus five");
        ledger.sweep();
        check("mult and flat under one id both apply", stat.getModifiedValue() == 30f && ledger.size() == 2);
        ledger.flat(stat, "x", 5f, "plus five");
        ledger.sweep();
        check("dropping the mult keeps the flat", stat.getModifiedValue() == 15f);
    }

    private static void statBonusWorks() {
        FakeColony colony = new FakeColony();
        Ledger ledger = new Ledger("Check");
        StatBonus bonus = new StatBonus();
        ledger.mult(bonus, "x", 2f, "doubled");
        ledger.flat(bonus, "y", 3f, "plus three", colony.market);
        ledger.sweep();
        check("stat bonus applies", bonus.computeEffective(10f) == 26f);
        check("stat bonus flat marks the colony", colony.reapplied == 1);
        ledger.sweep();
        check("stat bonus is undone", bonus.computeEffective(10f) == 10f && colony.reapplied == 2);
    }

    private static void textChangeIsWritten() {
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(1f);
        ledger.mult(stat, "x", 2f, "first");
        ledger.mult(stat, "x", 2f, "second");
        check("new text is written", "second".equals(stat.getMultStatMod("x").getDesc()));
    }

    private static void holdRunsItsUndoOnce() {
        Ledger ledger = new Ledger("Check");
        Object target = new Object();
        int[] undone = {0};
        ledger.hold(target, "x", () -> undone[0]++);
        ledger.sweep();
        ledger.hold(target, "x", () -> undone[0] += 100);
        ledger.sweep();
        check("re-asserted hold is not undone", undone[0] == 0);
        ledger.sweep();
        check("dropped hold runs the first undo once", undone[0] == 1);
        ledger.sweep();
        check("undo does not run twice", undone[0] == 1);
    }

    private static void brokenUndoDoesNotStopTheRest() {
        Ledger ledger = new Ledger("Check");
        MutableStat stat = new MutableStat(10f);
        ledger.hold(new Object(), "broken", () -> {
            throw new IllegalStateException("broken undo");
        });
        ledger.mult(stat, "x", 2f, "doubled");
        boolean threw = false;
        try {
            ledger.releaseForSave();
        } catch (Throwable t) {
            threw = true;
        }
        check("broken undo does not throw", !threw);
        check("other changes are still undone", stat.getModifiedValue() == 10f && ledger.isEmpty());
    }

    private static void brokenColonyDoesNotCrash() {
        FakeColony bad = new FakeColony();
        bad.explode = true;
        FakeColony good = new FakeColony();
        Ledger ledger = new Ledger("Check");
        ledger.touch(bad.market);
        ledger.touch(good.market);
        boolean threw = false;
        try {
            ledger.sweep();
        } catch (Throwable t) {
            threw = true;
        }
        check("broken colony does not throw", !threw);
        check("other colony is still applied", good.reapplied == 1);
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + what);
        }
    }
}
