package oddiszcommonstest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import oddiszcommons.manage.ManagePage;

/** One page with a line of text and a button that makes the Manage button blink. */
public class TestPage implements ManagePage {

    private static final String BLINK = "oddisz_commons_testpage_blink";

    /** Starts true so the Manage button blinks once on the first load. */
    private boolean news = true;
    private int timesOpened;

    @Override
    public CustomPanelAPI createPanel(float width, float height) {
        news = false;
        timesOpened++;
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, new BaseCustomUIPanelPlugin() {
            @Override
            public void buttonPressed(Object buttonId) {
                if (BLINK.equals(buttonId)) {
                    news = true;
                }
            }
        });
        TooltipMakerAPI text = panel.createUIElement(width, height, false);
        text.addSectionHeading("OddiszCommons test page", Alignment.MID, 0f);
        text.addPara("This page comes from the OddiszCommons Test Page mod. If you can read it, the Manage"
                + " button and its pages work.", 10f);
        text.addPara("Opened %s times this session.", 10f, Misc.getHighlightColor(), "" + timesOpened);
        text.addPara("The button below marks this page as having news. Close the screen: the Manage button"
                + " should pulse until you open this page again.", 10f);
        text.addButton("Mark as news", BLINK, 200f, 25f, 10f);
        panel.addUIElement(text).inTL(0f, 0f);
        return panel;
    }

    @Override
    public boolean hasNews() {
        return news;
    }
}
