package oddiszcommons;

import com.fs.starfarer.api.BaseModPlugin;

import oddiszcommons.manage.ManageBar;
import oddiszcommons.tips.Tips;

public class OddiszCommonsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        Tips.load();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ManageBar.onGameLoad();
    }
}
