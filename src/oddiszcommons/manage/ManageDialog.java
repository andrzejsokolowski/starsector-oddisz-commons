package oddiszcommons.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * 1.0.2: the Manage screen (our pages' tabs) in a dialog, opened from {@link ManageIntel} or {@link Manage#openPage}
 * when no Manage button on the bottom bar can show the pages. Only the game's public API.
 */
final class ManageDialog implements InteractionDialogPlugin {

    private final String start;
    private InteractionDialogAPI dialog;

    ManageDialog(String start) {
        this.start = start;
    }

    /** Opens the dialog out in the campaign. False when a menu or another dialog is in the way. */
    static boolean open(String modId) {
        if (Global.getSector() == null || PageRegistry.isEmpty()) {
            return false;
        }
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.isShowingMenu() || ui.isShowingDialog()) {
            return false;
        }
        return ui.showInteractionDialog(new ManageDialog(modId), Global.getSector().getPlayerFleet());
    }

    static List<Tenant> tenants() {
        List<Tenant> out = new ArrayList<>();
        for (PageRegistry.Entry e : PageRegistry.all()) {
            out.add(new Tenant(e.modId, e.tabName, e.factoryClass(), e.newsKey(), e.tabColor, true));
        }
        return out;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        float w = Math.min(Global.getSettings().getScreenWidth() - 80f, 1600f);
        float h = Math.min(Global.getSettings().getScreenHeight() - 120f, 960f);
        dialog.showCustomVisualDialog(w, h, new Delegate(w, h));
    }

    private final class Delegate implements CustomVisualDialogDelegate {
        private final float w, h;
        private DialogCallbacks callbacks;
        private ManageScreen screen;

        Delegate(float w, float h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return new BaseCustomUIPanelPlugin() {
                @Override
                public void processInput(List<InputEventAPI> events) {
                    // Always a way out: Escape closes the dialog unless the page used it (typing a name).
                    for (InputEventAPI e : events) {
                        if (e.isConsumed() || !e.isKeyDownEvent() || e.getEventValue() != Keyboard.KEY_ESCAPE) {
                            continue;
                        }
                        e.consume();
                        if (callbacks != null) callbacks.dismissDialog();
                        return;
                    }
                }
            };
        }

        @Override
        public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
            this.callbacks = callbacks;
            screen = new ManageScreen(tenants(), w, h, () -> {
                if (this.callbacks != null) this.callbacks.dismissDialog();
            });
            panel.addComponent(screen.root()).inTL(0f, 0f);
            screen.selectFirst(start != null ? start : ManageQueue.take());
        }

        @Override
        public float getNoiseAlpha() {
            return 0f;
        }

        @Override
        public void advance(float amount) {
        }

        @Override
        public void reportDismissed(int option) {
            if (screen != null) screen.closed();
            if (dialog != null) dialog.dismiss();
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
