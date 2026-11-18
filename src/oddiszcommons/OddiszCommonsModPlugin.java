package oddiszcommons;

import com.fs.starfarer.api.BaseModPlugin;

import oddiszcommons.manage.ManageBar;
import oddiszcommons.manage.ManageJoin;
import oddiszcommons.tips.Tips;

public class OddiszCommonsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        Tips.load();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        // 1.0.2: the parts that need no game class names first, so a game update that breaks the bar code
        // still hands the pages to another mod's button or puts them in the intel screen.
        ManageJoin.onGameLoad();
        try {
            ManageBar.onGameLoad();
        } catch (Throwable t) {
            com.fs.starfarer.api.Global.getLogger(OddiszCommonsModPlugin.class).warn("OddiszCommons: the Manage"
                    + " button does not fit this game version and is off. The pages are in the intel screen.", t);
        }
    }
}
