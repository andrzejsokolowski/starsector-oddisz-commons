package oddiszcommons.manage;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * One tab on the Manage screen while OddiszCommons is in charge of the button: one of our pages or
 * a tab another mod handed over. Either way the tab is a class name with two static methods.
 */
final class Tenant {

    private static final MethodType BUILD = MethodType.methodType(CustomPanelAPI.class, float.class, float.class);
    private static final MethodType RELEASE = MethodType.methodType(void.class);

    final String modId;
    final String label;
    final String factoryClass;
    final String attentionKey;
    final Color color;
    /** True for a page added through {@link Manage}, false for a tab from another mod. */
    final boolean ours;

    Tenant(String modId, String label, String factoryClass, String attentionKey, Color color, boolean ours) {
        this.modId = modId;
        this.label = label;
        this.factoryClass = factoryClass;
        this.attentionKey = attentionKey;
        this.color = color;
        this.ours = ours;
    }

    CustomPanelAPI build(float width, float height) throws Throwable {
        MethodHandle build = MethodHandles.publicLookup()
                .findStatic(Class.forName(factoryClass), "buildMgmtConsole", BUILD);
        return (CustomPanelAPI) build.invoke(width, height);
    }

    void release() {
        try {
            MethodHandles.publicLookup()
                    .findStatic(Class.forName(factoryClass), "releaseMgmtConsole", RELEASE)
                    .invoke();
        } catch (Throwable ignored) {
            // A tab without a release step is fine.
        }
    }

    boolean wantsAttention() {
        if (attentionKey == null || Global.getSector() == null) {
            return false;
        }
        return Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate().get(attentionKey));
    }

    /** The player opened this tab: clear its news flag, the way the other mods expect. */
    void seen() {
        if (attentionKey != null && Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(attentionKey, Boolean.FALSE);
        }
    }
}
