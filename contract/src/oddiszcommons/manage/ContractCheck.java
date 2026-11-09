package oddiszcommons.manage;

import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * Plays the other mods' side of the Manage button agreement without the game. They find our methods
 * the way the agreement's existing mods do: by name and number of arguments, then call them with
 * boxed values. Never shipped (this source set is not in the mod jar), so plain reflection is fine.
 */
public final class ContractCheck {

    private static int failures;
    private static int checks;

    public static void main(String[] args) throws Throwable {
        seamAsOtherModsSeeIt();
        pagesThroughTheirEntryPoints();
        handingPagesToAnotherButton();
        linksAgainstTheGame();
        System.out.println("Manage button agreement: " + (checks - failures) + " of " + checks + " checks passed");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** OddiszCommons in charge: the calls Project Arondight, Captain's Quarters and Liminal make. */
    private static void seamAsOtherModsSeeIt() throws Throwable {
        Class<?> seam = Class.forName(ManageBar.class.getName());
        Method register = staticMethod(seam, "registerTenant", 7);
        register.invoke(null, "foreign_mod", "Foreign", FakeTenant.class.getName(), "$foreign_news", 10, 20, 30);
        register.invoke(null, "foreign_mod", "Second try", "nope", null, 1, 1, 1);
        List<Tenant> tenants = ManageBar.tenantsSnapshot();
        check(tenants.size() == 1 && tenants.get(0).label.equals("Foreign"), "registerTenant keeps the first tab of a mod");
        check(tenants.get(0).color.equals(new Color(10, 20, 30)), "registerTenant keeps the tab colour");
        check(tenants.get(0).build(300f, 200f) == FakeTenant.PANEL, "another mod's tab builds through its own class");
        check(FakeTenant.lastWidth == 300f && FakeTenant.lastHeight == 200f, "the tab gets the screen size");
        tenants.get(0).release();
        check(FakeTenant.released == 1, "the tab is told when it closes");

        Object alive = staticMethod(seam, "isAlive", 0).invoke(null);
        check(Boolean.FALSE.equals(alive), "isAlive answers false while nothing runs");
        staticMethod(seam, "requestTabSelect", 1).invoke(null, "foreign_mod");
        check("foreign_mod".equals(ManageBar.takeQueuedSelect()), "requestTabSelect queues the tab");
        Object opened = staticMethod(seam, "openManagementScreen", 0).invoke(null);
        check(Boolean.FALSE.equals(opened), "openManagementScreen answers false outside a campaign");
    }

    /** Our pages, reached the way any Manage screen reaches a tab: one class per page. */
    private static void pagesThroughTheirEntryPoints() throws Throwable {
        for (int i = 0; i < Slots.COUNT; i++) {
            Class<?> slot = Class.forName(Slots.className(i));
            check(Modifier.isPublic(slot.getModifiers()), "entry point " + i + " is public");
            Method build = staticMethod(slot, "buildMgmtConsole", 2);
            check(build.getReturnType() == CustomPanelAPI.class
                    && build.getParameterTypes()[0] == float.class && build.getParameterTypes()[1] == float.class,
                    "entry point " + i + " builds a panel from two sizes");
            staticMethod(slot, "releaseMgmtConsole", 0);
        }

        StubPage a = new StubPage();
        StubPage b = new StubPage();
        check(Manage.addPage("mod_a", "A", new Color(1, 2, 3), a), "the first page is added");
        check(Manage.addPage("mod_b", "B", new Color(4, 5, 6), b), "the second page is added");
        PageRegistry.Entry ea = PageRegistry.get("mod_a");
        PageRegistry.Entry eb = PageRegistry.get("mod_b");
        check(ea.slot == 0 && eb.slot == 1, "each page gets its own entry point");

        Object panel = staticMethod(Class.forName(ea.factoryClass()), "buildMgmtConsole", 2).invoke(null, 640f, 480f);
        check(panel == a.panel && a.lastWidth == 640f && b.built == 0, "a page's entry point builds that page only");
        staticMethod(Class.forName(eb.factoryClass()), "releaseMgmtConsole", 0).invoke(null);
        check(b.closed == 1 && a.closed == 0, "a page's entry point closes that page only");

        Tenant ours = new Tenant(eb.modId, eb.tabName, eb.factoryClass(), eb.newsKey(), eb.tabColor, true);
        check(ours.build(100f, 100f) == b.panel, "our own screen builds our pages the same way");

        StubPage a2 = new StubPage();
        Manage.addPage("mod_a", "A again", null, a2);
        check(PageRegistry.get("mod_a").slot == 0 && PageRegistry.get("mod_a").tabName.equals("A again"),
                "adding a page again replaces it in place");
        Manage.removePage("mod_a");
        check(!Manage.hasPage("mod_a"), "a removed page is gone");
        Manage.addPage("mod_c", "C", null, new StubPage());
        check(PageRegistry.get("mod_c").slot == 0, "a removed page frees its entry point");

        for (int i = 0; i < Slots.COUNT - 2; i++) {
            Manage.addPage("fill_" + i, "Fill", null, new StubPage());
        }
        check(PageRegistry.all().size() == Slots.COUNT, "the screen holds " + Slots.COUNT + " pages");
        check(!Manage.addPage("one_too_many", "X", null, new StubPage()), "a page past the limit is refused");
        for (int i = 0; i < Slots.COUNT - 2; i++) {
            Manage.removePage("fill_" + i);
        }
    }

    /** Another mod in charge: OddiszCommons hands it one tab per page. */
    private static void handingPagesToAnotherButton() throws Throwable {
        PageRegistry.Entry eb = PageRegistry.get("mod_b");
        ManageBar.handOver(FakeHost.class.getName(), eb);
        check(FakeHost.calls.size() == 1, "one tab is handed over per page");
        Object[] call = FakeHost.calls.get(0);
        check("mod_b".equals(call[0]) && "B".equals(call[1]) && eb.factoryClass().equals(call[2]),
                "the handed tab names the page and its entry point");
        check("$oddisz_commons_news_mod_b".equals(call[3]), "the handed tab carries the news flag");
        check(Integer.valueOf(4).equals(call[4]) && Integer.valueOf(5).equals(call[5]) && Integer.valueOf(6).equals(call[6]),
                "the handed tab keeps its colour");
    }

    /** The classes built on the game's own screen classes load against this game version. */
    private static void linksAgainstTheGame() throws Throwable {
        Class<?> shell = Class.forName(ManageShell.class.getName(), false, ContractCheck.class.getClassLoader());
        check(shell.getSuperclass().getName().equals("com.fs.starfarer.ui.interfacenew"),
                "the screen is one of the game's own panels");
        check(Class.forName("com.fs.starfarer.ui.newui.Y").isAssignableFrom(shell),
                "the screen hears when its tab closes");
        Class<?> bar = Class.forName("com.fs.starfarer.ui.newui.thissuper");
        Method addButton = null;
        for (Method m : bar.getMethods()) {
            if (m.getName().equals("addButton") && m.getParameterCount() == 4) {
                addButton = m;
            }
        }
        check(addButton != null && addButton.getParameterTypes()[3].getName().equals("com.fs.starfarer.ui.U"),
                "the bottom bar still takes a button with a click listener");
        Class<?> core = Class.forName("com.fs.starfarer.ui.newui.L");
        boolean showPanel = false;
        for (Method m : core.getMethods()) {
            showPanel |= m.getName().equals("showPanelAsDialog") && m.getParameterCount() == 4
                    && m.getParameterTypes()[1].isAssignableFrom(shell);
        }
        check(showPanel, "the core screen still opens a panel as a tab");
        check(core.getField("MAX_INTEL_WIDTH") != null && core.getMethod("getButtons") != null
                && core.getMethod("getCurrentTab") != null && core.getMethod("dismiss", int.class) != null,
                "the core screen still has its size limits, bar, current tab and close");
        check(Class.forName("com.fs.starfarer.campaign.CampaignState").getMethod("getCore") != null,
                "the campaign still hands out its core screen");
    }

    private static Method staticMethod(Class<?> cls, String name, int args) {
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args) {
                check(Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()),
                        cls.getSimpleName() + "." + name + " is public and static");
                return m;
            }
        }
        check(false, cls.getName() + " has " + name + " with " + args + " arguments");
        throw new IllegalStateException(name);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAILED: " + what);
        }
    }

    static CustomPanelAPI fakePanel() {
        return (CustomPanelAPI) Proxy.newProxyInstance(ContractCheck.class.getClassLoader(),
                new Class<?>[] {CustomPanelAPI.class}, (proxy, method, args) -> {
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    return null;
                });
    }

    static final class StubPage implements ManagePage {
        final CustomPanelAPI panel = fakePanel();
        int built;
        int closed;
        float lastWidth;

        @Override
        public CustomPanelAPI createPanel(float width, float height) {
            built++;
            lastWidth = width;
            return panel;
        }

        @Override
        public void panelClosed() {
            closed++;
        }
    }

    /** Stands in for another mod's tab class. */
    public static final class FakeTenant {
        static final CustomPanelAPI PANEL = fakePanel();
        static float lastWidth;
        static float lastHeight;
        static int released;

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            lastWidth = width;
            lastHeight = height;
            return PANEL;
        }

        public static void releaseMgmtConsole() {
            released++;
        }
    }

    /** Stands in for another mod's Manage button. */
    public static final class FakeHost {
        static final List<Object[]> calls = new ArrayList<>();

        public static void registerTenant(String modId, String label, String factoryClass, String attentionKey,
                int r, int g, int b) {
            calls.add(new Object[] {modId, label, factoryClass, attentionKey, r, g, b});
        }
    }
}
