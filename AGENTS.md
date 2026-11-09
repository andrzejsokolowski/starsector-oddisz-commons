# OddiszCommons

These instructions apply to this repository, including when it is opened without the parent mod workspace.
The workspace `AGENTS.md` one folder up still governs releases, packaging, Git identity and communication.

## Notion

Tasks, history and test lists: the **OddiszCommons board** in Andrzej's Notion, under **Starsector mods**
(`collection://1e31cca2-e672-4c4e-8ec8-04a20d76ddd3`). Ticket ids start with ODC. Read the board with
`notion-query-data-sources` mode `view` on `view://41320ca7-5d20-8197-b4ad-000c2d09e9b4`.
Working rules and game API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`),
starting with "Start here: working on Starsector mods through Notion". The page "Shared Manage button" there
describes the agreement with the other Manage button mods. Update the board during the work.

## What it is

A library mod (id `oddisz_commons`, package `oddiszcommons`, staging folder `OddiszCommons`) for parts
Andrzej's mods share. Plain Java 17, no Kotlin, no LunaLib. Andrzej chose the name on 2026-11-09 (ODC-1).
Other mods depend on it, so keep the public API (`oddiszcommons.manage.Manage`, `ManagePage`) stable.

## Build

`./gradlew releaseZip` compiles against the game, runs the checks and writes `OddiszCommons.zip`.
`./gradlew testPageZip` writes `OddiszCommonsTestPage.zip`, a test-only mod with one page (never released).
`checkContract` runs `contract/src/.../ContractCheck` on the game's own Java with bytecode checks off
(the game's scrambled class names need that). It plays the other mods' side of the agreement.

## The Manage button

- `ManageBar` is the script and the agreement's entry point. Sector memory `$mgmt_bar` holds
  `{host, version 2, seam}`. Every mod in the agreement unsets it on load. The first to find the bottom bar
  adds the button and writes its seam class there. The others call that class's
  `registerTenant(modId, label, factoryClass, attentionKey, r, g, b)`.
- A tab is only a class name with static `buildMgmtConsole(float, float)` and `releaseMgmtConsole()`, with no
  page id passed. So each page owns one of the 12 classes in `Slots` (`Slots$S0` to `Slots$S11`).
- Game classes called directly (compiled against 0.98a-RC8): `CampaignState.getCore()`,
  `getEncounterDialog().getCoreUI()` when docked, core screen `ui.newui.L` (`getButtons`,
  `showPanelAsDialog`, `computePanelWidth`, `MAX_INTEL_WIDTH`, `getCurrentTab`, `dismiss`), the bar
  `ui.newui.thissuper` (`addButton(label, id, null, listener)`, `getButton`, `BUTTON_WIDTH`, `BUTTON_PAD`),
  the button `ui.n`, the listener `ui.U` and the tab panel `ui.interfacenew` with `ui.newui.Y.tabClosed`.
  A game update can rename these: `checkContract` then fails. Any error at run time turns the button off
  for the session.
- `BarPlate` extends the bar's backing image (`ui/tripad_button_holder`, 914 px, game buttons end at 903)
  under the added button.
- Project Arondight, Captain's Quarters and Liminal (Mamick, GPL 3) carry the other side. This code is
  written from scratch to match the agreement. Their source is not in this repository and must not be.
