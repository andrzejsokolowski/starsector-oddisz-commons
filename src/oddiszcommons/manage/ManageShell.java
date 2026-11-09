package oddiszcommons.manage;

import java.util.List;

/**
 * The game shows a bottom bar tab as one of its own panels, and tells that panel when the tab
 * closes. This is that panel. It holds the {@link ManageScreen} and nothing else.
 */
public class ManageShell extends com.fs.starfarer.ui.interfacenew implements com.fs.starfarer.ui.newui.Y {

    private final ManageScreen screen;

    ManageShell(List<Tenant> tenants, float width, float height) {
        super(width, height);
        screen = new ManageScreen(tenants, width, height);
        addComponent(screen.root()).inTL(0f, 0f);
    }

    ManageScreen screen() {
        return screen;
    }

    @Override
    public void tabClosed() {
        screen.closed();
        ManageBar.shellClosed(this);
    }
}
