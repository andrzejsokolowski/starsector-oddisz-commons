package oddiszcommons;

import com.fs.starfarer.api.BaseModPlugin;

import oddiszcommons.manage.ManageBar;

public class OddiszCommonsModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        ManageBar.onGameLoad();
    }
}
