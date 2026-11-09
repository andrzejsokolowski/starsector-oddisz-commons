package oddiszcommons.manage;

import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * Fixed entry points, one per page. A Manage screen (ours or another mod's) knows a tab only by a
 * class name with two static methods, {@code buildMgmtConsole(float, float)} and
 * {@code releaseMgmtConsole()}, and passes no page id. So every page gets a class of its own.
 * Keep {@link #COUNT} in step with the classes below.
 */
public final class Slots {

    public static final int COUNT = 12;

    private Slots() {
    }

    static String className(int slot) {
        return Slots.class.getName() + "$S" + slot;
    }

    public static final class S0 {
        private S0() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(0, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(0);
        }
    }

    public static final class S1 {
        private S1() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(1, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(1);
        }
    }

    public static final class S2 {
        private S2() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(2, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(2);
        }
    }

    public static final class S3 {
        private S3() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(3, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(3);
        }
    }

    public static final class S4 {
        private S4() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(4, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(4);
        }
    }

    public static final class S5 {
        private S5() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(5, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(5);
        }
    }

    public static final class S6 {
        private S6() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(6, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(6);
        }
    }

    public static final class S7 {
        private S7() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(7, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(7);
        }
    }

    public static final class S8 {
        private S8() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(8, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(8);
        }
    }

    public static final class S9 {
        private S9() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(9, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(9);
        }
    }

    public static final class S10 {
        private S10() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(10, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(10);
        }
    }

    public static final class S11 {
        private S11() {
        }

        public static CustomPanelAPI buildMgmtConsole(float width, float height) {
            return PageRegistry.buildSlot(11, width, height);
        }

        public static void releaseMgmtConsole() {
            PageRegistry.releaseSlot(11);
        }
    }
}
