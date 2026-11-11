package oddiszcommons.tips;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.util.WeightedRandomPicker;

/**
 * Checks the tips file and the tip filter without the game: every tip names a real mod and starts
 * with its tag, only enabled mods get their tips, and the game's tip picker is where Tips expects it.
 * Never shipped, so file access and reflection are fine here.
 */
public final class TipsCheck {

    private static int failures;
    private static int checks;

    public static void main(String[] args) throws Exception {
        JSONArray tips = readTips(Path.of(Tips.FILE));
        Set<String> knownMods = modIdsNextTo(Path.of("").toAbsolutePath().getParent());
        check(tips.length() > 0, "the tips file has tips");
        for (int i = 0; i < tips.length(); i++) {
            JSONObject entry = tips.getJSONObject(i);
            String mod = entry.getString("mod");
            String tip = entry.getString("tip");
            check(knownMods.contains(mod), "tip " + (i + 1) + " names a real mod id (" + mod + ")");
            check(tip.matches("\\[[^\\]]+\\] - \\S.*"), "tip " + (i + 1) + " starts with a [Mod] - tag");
            check(tip.chars().allMatch(c -> c >= 32 && c < 127), "tip " + (i + 1) + " is plain ASCII");
            check(tip.length() <= 220, "tip " + (i + 1) + " is short enough for the loading screen");
        }

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        int added = Tips.add(tips, mod -> mod.equals("sol_renewed"), picker);
        check(added == 1 && picker.getItems().size() == 1 && picker.getItems().get(0).startsWith("[Sol Renewed]"),
                "only the enabled mod's tips are added");
        check(Tips.add(tips, mod -> false, new WeightedRandomPicker<>()) == 0, "no mods enabled, no tips");

        JSONArray odd = new JSONArray("[{\"mod\":null,\"tip\":\"x\"},{\"mod\":\"a\",\"tip\":null},{\"mod\":\"a\",\"tip\":\"  \"},"
                + "{\"mod\":\"a\",\"tip\":\"rare\",\"freq\":0.25},{\"mod\":\"a\",\"tip\":\"never\",\"freq\":0},\"plain text\"]");
        WeightedRandomPicker<String> oddPicker = new WeightedRandomPicker<>();
        check(Tips.add(odd, mod -> true, oddPicker) == 1 && oddPicker.getItems().get(0).equals("rare")
                && Math.abs(oddPicker.getWeight(0) - 0.25f) < 1e-6f, "null, blank and freq 0 entries are skipped, freq is kept");

        // Do not initialise the game class: its UI parent class needs a running game.
        Class<?> tipScreen = Class.forName("com.fs.starfarer.title.String", false, TipsCheck.class.getClassLoader());
        Field field = tipScreen.getField("picker");
        check(Modifier.isStatic(field.getModifiers()) && field.getType() == WeightedRandomPicker.class,
                "the game keeps its tips in the public static picker Tips adds to");

        System.out.println("Tips: " + (checks - failures) + " of " + checks + " checks passed");
        if (failures > 0) System.exit(1);
    }

    /** The game's JSON reader allows # comments; org.json does not. */
    private static JSONArray readTips(Path file) throws Exception {
        StringBuilder text = new StringBuilder();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().startsWith("#")) text.append(line).append('\n');
        }
        return new JSONObject(text.toString()).getJSONArray("tips");
    }

    private static Set<String> modIdsNextTo(Path workspace) throws Exception {
        Set<String> ids = new HashSet<>();
        Pattern id = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        try (Stream<Path> folders = Files.list(workspace)) {
            for (Path folder : (Iterable<Path>) folders::iterator) {
                Path info = folder.resolve("mod_info.json");
                if (!Files.isRegularFile(info)) continue;
                Matcher m = id.matcher(Files.readString(info, StandardCharsets.UTF_8));
                if (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }
}
