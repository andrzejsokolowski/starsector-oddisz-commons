package oddiszcommons.tips;

import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

/**
 * Adds loading screen and main menu tips for the mods that are enabled. The game reads every mod's
 * data/strings/tips.json into one picker before any mod code runs, so a tip there would show even
 * without its mod. These tips live in data/config/oddisz_commons/tips.json instead, each naming its
 * mod, and go into the game's picker only when that mod is enabled.
 */
public final class Tips {

    public static final String FILE = "data/config/oddisz_commons/tips.json";

    private static final Logger log = Logger.getLogger(Tips.class);

    private Tips() {
    }

    /** Called once from onApplicationLoad. Any failure is logged and the tips are skipped. */
    public static void load() {
        try {
            JSONArray list = Global.getSettings().getMergedJSON(FILE).getJSONArray("tips");
            ModManagerAPI mods = Global.getSettings().getModManager();
            // The game's own tip picker (com.fs.starfarer.title.String, 0.98a-RC8). It is filled once,
            // when the game loads its strings, and read by every loading screen and the title screen.
            int added = add(list, mods::isModEnabled, com.fs.starfarer.title.String.picker);
            log.info("OddiszCommons: added " + added + " tips for installed mods");
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError && !(t instanceof StackOverflowError)) throw (VirtualMachineError) t;
            log.warn("OddiszCommons: tips skipped", t);
        }
    }

    /** Adds every tip whose mod is enabled. Returns how many were added. */
    static int add(JSONArray list, Predicate<String> enabled, WeightedRandomPicker<String> picker) {
        int added = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.optJSONObject(i);
            if (entry == null || entry.isNull("mod") || entry.isNull("tip")) continue;
            String mod = entry.optString("mod", "").trim();
            String tip = entry.optString("tip", "").trim();
            float freq = (float) entry.optDouble("freq", 1d);
            if (mod.isEmpty() || tip.isEmpty() || !(freq > 0f) || !enabled.test(mod)) continue;
            picker.add(tip, freq);
            added++;
        }
        return added;
    }
}
