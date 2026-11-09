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

## The colony ledger

`oddiszcommons.ledger.Ledger` keeps every change a mod makes to the game, with the way to undo it.
The mod puts every change that should exist right now through the ledger on each pass (usually each
frame), then calls `sweep()`. Whatever was not put through since the last sweep is undone: the core
was removed, the building was lost, a setting was switched off.

```java
private final Ledger ledger = new Ledger("My Mod");

@Override
public void advance(float amount) {
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
        if (wanted(market)) ledger.mult(market.getIncomeMult(), "mymod_income", 2f, "My Mod", market);
    }
    ledger.sweep();
}
```

- `mult` and `flat` work on `MutableStat` and `StatBonus`. `hold(target, id, undo)` keeps any other
  change, already made by the caller.
- A change that names its colony marks it when its value moves or is undone. By default the sweep
  then applies that colony's buildings again. `new Ledger(name, false)` only collects the colonies
  for `takeChangedColonies()`. `setChangeThreshold(0.001f)` ignores tiny moves.
- Call `releaseForSave()` from `beforeGameSave` so a save holds nothing of the mod. The next pass puts
  everything back. It does not apply colonies again, because that crashes during a save.
- An undo or a colony that throws is logged once in starsector.log and skipped.

## Build

`./gradlew releaseZip` compiles against the game (`gradle.properties` names the game folder), runs the
checks and writes `OddiszCommons.zip` in this folder. `./gradlew testPageZip` writes
`OddiszCommonsTestPage.zip`, a separate test mod that adds one page. It is for testing only and is
never released.
