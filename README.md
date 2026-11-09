# OddiszCommons

A Starsector 0.98a library mod by Oddisz: parts that Oddisz's mods share.

## The Manage button

A **Manage (Y)** button on the campaign bottom bar, after Command. It also shows on that bar while
docked. Clicking it (or pressing Y) opens a full screen like Intel: one tab per mod along the top, and
that mod's page below. Tabs 1 to 9 also open with the number keys. The button only appears when at
least one installed mod has a page, and it pulses while a page has news.

Project Arondight, Captain's Quarters and Liminal carry their own Manage button. OddiszCommons shares
that button with them, so with any of them installed there is still one Manage button, with their
tabs and the tabs of Oddisz's mods side by side.

### Adding a page from another mod

Add `oddisz_commons` to the mod's `dependencies` in `mod_info.json`, put `OddiszCommons.jar` on the
compile path, and add the page once when the game starts:

```java
@Override
public void onApplicationLoad() {
    Manage.addPage("missions_renewed", "Missions", new MissionsPage());
}
```

A page builds its own panel each time its tab opens:

```java
public class MissionsPage implements ManagePage {
    @Override
    public CustomPanelAPI createPanel(float width, float height) {
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, new MyPlugin());
        TooltipMakerAPI text = panel.createUIElement(width, height, true);
        text.addPara("...", 0f);
        panel.addUIElement(text).inTL(0f, 0f);
        return panel;
    }

    @Override
    public boolean hasNews() {   // optional: the button and the tab pulse while true
        return false;
    }

    @Override
    public void panelClosed() {  // optional: the tab closed
    }
}
```

`Manage.addPage(modId, tabName, tabColor, page)` gives the tab a colour. `Manage.openPage(modId)` opens
the screen on that page. Up to 12 pages fit. A page removed with `Manage.removePage` stays on another
mod's button until the next load.

## Build

`./gradlew releaseZip` compiles against the game (`gradle.properties` names the game folder), runs the
checks and writes `OddiszCommons.zip` in this folder. `./gradlew testPageZip` writes
`OddiszCommonsTestPage.zip`, a separate test mod that adds one page. It is for testing only and is
never released.
