package oddiszcommonstest;

import java.awt.Color;

import com.fs.starfarer.api.BaseModPlugin;

import oddiszcommons.manage.Manage;

public class TestPagePlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        Manage.addPage("oddisz_commons_testpage", "Test page", new Color(120, 200, 255), new TestPage());
    }
}
