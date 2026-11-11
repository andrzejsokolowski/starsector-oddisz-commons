# OddiszCommons

A Starsector 0.98a library mod by Oddisz: parts that Oddisz's mods share.
Forum topic: https://fractalsoftworks.com/forum/index.php?topic=36282

Needed by Omega Core Everywhere 1.2.3, Astral Ascension Renewed 1.0.2, Toolbox Economy Curves 1.0.3
(and newer) and Landlordsector.

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

## Loading screen tips

The game reads every mod's `data/strings/tips.json` before any mod code runs, so a tip there shows
even when its mod is not installed. OddiszCommons keeps its tips in
`data/config/oddisz_commons/tips.json` instead, each naming its mod, and adds a tip to the game's
tip list only when that mod is enabled:

```
{
	"tips":[
		{"mod":"missions_renewed", "tip":"[Missions - Renewed] - Check the deadline before you sign."},
		{"mod":"my_mod", "tip":"[My Mod] - A rarer tip.", "freq":0.5},
	],
}
```

Another mod can add its own tips by shipping a file at the same path. `freq` is optional (default 1,
the same as every base game tip). The very first loading screen of a launch comes before mod code
runs, so these tips show from the main menu on.

## Build

`./gradlew releaseZip` compiles against the game (`gradle.properties` names the game folder), runs the
checks and writes `OddiszCommons.zip` in this folder. `./gradlew testPageZip` writes
`OddiszCommonsTestPage.zip`, a separate test mod that adds one page. It is for testing only and is
never released.
