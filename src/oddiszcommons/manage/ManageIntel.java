package oddiszcommons.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * 1.0.2: the Manage pages in the intel screen, for when no Manage button on the bottom bar can show them (a game
 * update broke the bar code, or another mod's button refused the pages). One button per page opens the Manage
 * screen in a dialog ({@link ManageDialog}). Only the game's public API.
 */
public class ManageIntel extends BaseIntelPlugin {

    public static final String TAG = "Manage";
    /** Set once the player was told, so the message comes once per save. */
    private static final String TOLD_KEY = "$oddisz_commons_manage_intel_told";

    /** Adds the entry when {@code on} and it is missing, removes it when not. */
    static void show(boolean on) {
        if (Global.getSector() == null) {
            return;
        }
        IntelInfoPlugin have = Global.getSector().getIntelManager().getFirstIntel(ManageIntel.class);
        if (on && have == null) {
            boolean told = Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate().get(TOLD_KEY));
            Global.getSector().getIntelManager().addIntel(new ManageIntel(), told);
            Global.getSector().getMemoryWithoutUpdate().set(TOLD_KEY, Boolean.TRUE);
        } else if (!on && have != null) {
            removeAll();
        }
    }

    /** Takes every entry out (on load: the bar may work this time). */
    static void removeAll() {
        if (Global.getSector() == null) {
            return;
        }
        for (IntelInfoPlugin i : new ArrayList<>(Global.getSector().getIntelManager().getIntel(ManageIntel.class))) {
            Global.getSector().getIntelManager().removeIntel(i);
        }
    }

    public ManageIntel() {
        setImportant(true);
    }

    @Override
    protected String getName() {
        return "Manage";
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), Misc.getBasePlayerColor(), 0f);
        List<String> names = new ArrayList<>();
        for (PageRegistry.Entry e : PageRegistry.all()) names.add(e.tabName);
        if (!names.isEmpty()) info.addPara(String.join(", ", names), Misc.getGrayColor(), 3f);
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("The Manage button on the bottom bar can't show these pages right now, so they open from here.",
                0f);
        float pad = 10f;
        for (PageRegistry.Entry e : PageRegistry.all()) {
            info.addButton(e.tabName, e.modId, Math.min(width - 10f, 300f), 25f, pad);
            pad = 6f;
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId instanceof String && PageRegistry.get((String) buttonId) != null) {
            ui.showDialog(Global.getSector().getPlayerFleet(), new ManageDialog((String) buttonId));
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    @Override
    public boolean doesButtonHaveConfirmDialog(Object buttonId) {
        if (buttonId instanceof String && PageRegistry.get((String) buttonId) != null) return false;
        return super.doesButtonHaveConfirmDialog(buttonId);
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "monthly_income_report");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(TAG);
        return tags;
    }
}
