package oddiszcommons.manage;

/**
 * The tab to open next, kept apart from {@link ManageBar} so the Manage screen and its fallbacks use no game class
 * that a game update can rename (1.0.2).
 */
final class ManageQueue {

    private static volatile String queuedSelect;

    private ManageQueue() {
    }

    static void request(String modId) {
        queuedSelect = modId;
    }

    static String take() {
        String s = queuedSelect;
        queuedSelect = null;
        return s;
    }

    static void clear() {
        queuedSelect = null;
    }
}
