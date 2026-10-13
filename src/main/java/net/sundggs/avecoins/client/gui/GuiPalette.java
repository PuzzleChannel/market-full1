/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.sundggs.avecoins.config.ShopColor;

public final class GuiPalette {
    public static final GuiPalette DEFAULT = new GuiPalette();
    public final boolean custom;
    public final int BACKGROUND;
    public final int PANEL;
    public final int HEADER;
    public final int SURFACE;
    public final int SURFACE_HOVER;
    public final int SURFACE_SELECTED;
    public final int HEADER_BUTTON;
    public final int HEADER_BUTTON_HOVER;
    public final int SELECTED_ACCENT;
    public final int BORDER;
    public final int DIVIDER;
    public final int TEXT;
    public final int MUTED;
    public final int DISABLED;
    public final int SUCCESS;
    public final int SUCCESS_BOX;
    public final int SUCCESS_BOX_HOVER;
    public final int INFO_BOX;
    public final int ERROR;
    public final int WARNING;
    public final int SOFT_WARNING;
    public final int DISCOUNT;
    public final int DANGER;
    public final int DANGER_HOVER;
    public final int ALTERNATE;
    public final int UNAVAILABLE;
    public final int HISTORY_SUCCESS;
    public final int HISTORY_ERROR;

    private GuiPalette() {
        this.custom = false;
        this.BACKGROUND = 0xFF101318;
        this.PANEL = -15263203;
        this.HEADER = -14407636;
        this.SURFACE = -14670808;
        this.SURFACE_HOVER = -12959674;
        this.SURFACE_SELECTED = -11906728;
        this.HEADER_BUTTON = -13354432;
        this.HEADER_BUTTON_HOVER = -11511711;
        this.SELECTED_ACCENT = -4669241;
        this.BORDER = -10656661;
        this.DIVIDER = -12959932;
        this.TEXT = -1710103;
        this.MUTED = -6183253;
        this.DISABLED = -9933454;
        this.SUCCESS = -6307928;
        this.SUCCESS_BOX = -14137548;
        this.SUCCESS_BOX_HOVER = -13543361;
        this.INFO_BOX = -11844312;
        this.ERROR = -3044212;
        this.WARNING = -1718678;
        this.SOFT_WARNING = -1716349;
        this.DISCOUNT = -9252725;
        this.DANGER = -10930377;
        this.DANGER_HOVER = -8960442;
        this.ALTERNATE = -14933981;
        this.UNAVAILABLE = -15658219;
        this.HISTORY_SUCCESS = -14800606;
        this.HISTORY_ERROR = -13950944;
    }

    private GuiPalette(int base) {
        this.custom = true;
        this.BACKGROUND = 0xFF101318;
        this.PANEL = base;
        int foreground = GuiPalette.contrast(-16777216, base) >= GuiPalette.contrast(-1, base) ? -16777216 : -1;
        int blackEndpoint = GuiPalette.surface(-16777216, foreground);
        int whiteEndpoint = GuiPalette.surface(-1, foreground);
        int endpoint = GuiPalette.contrast(base, blackEndpoint) >= GuiPalette.contrast(base, whiteEndpoint) ? blackEndpoint : whiteEndpoint;
        this.HEADER = GuiPalette.separated(base, endpoint, foreground, 1.15);
        this.SURFACE = GuiPalette.separated(base, endpoint, foreground, 1.35);
        this.ALTERNATE = GuiPalette.separated(base, endpoint, foreground, 1.22);
        this.SURFACE_HOVER = GuiPalette.separated(base, endpoint, foreground, 1.6);
        this.SURFACE_SELECTED = GuiPalette.separated(base, endpoint, foreground, 1.9);
        this.HEADER_BUTTON = GuiPalette.separated(base, endpoint, foreground, 1.45);
        this.HEADER_BUTTON_HOVER = GuiPalette.separated(base, endpoint, foreground, 1.8);
        this.UNAVAILABLE = GuiPalette.separated(base, endpoint, foreground, 1.1);
        this.SUCCESS_BOX = GuiPalette.separatedFrom(base, GuiPalette.surface(GuiPalette.mix(base, -13466805, 0.3), foreground), endpoint, foreground, 1.35);
        this.SUCCESS_BOX_HOVER = GuiPalette.separated(this.SUCCESS_BOX, endpoint, foreground, 1.15);
        this.DANGER = GuiPalette.separatedFrom(base, GuiPalette.surface(GuiPalette.mix(base, -5883848, 0.3), foreground), endpoint, foreground, 1.35);
        this.DANGER_HOVER = GuiPalette.separated(this.DANGER, endpoint, foreground, 1.15);
        this.INFO_BOX = GuiPalette.separatedFrom(base, GuiPalette.surface(GuiPalette.mix(base, -4616926, 0.3), foreground), endpoint, foreground, 1.35);
        this.HISTORY_SUCCESS = GuiPalette.separatedFrom(base, GuiPalette.surface(GuiPalette.mix(base, -13466805, 0.15), foreground), endpoint, foreground, 1.22);
        this.HISTORY_ERROR = GuiPalette.separatedFrom(base, GuiPalette.surface(GuiPalette.mix(base, -5883848, 0.15), foreground), endpoint, foreground, 1.22);
        int[] backgrounds = this.surfaces();
        this.TEXT = GuiPalette.readable(foreground == -16777216 ? -15328996 : -723466, backgrounds, 4.5);
        this.MUTED = GuiPalette.readable(foreground == -16777216 ? -12038055 : -4340533, backgrounds, 4.5);
        this.DISABLED = GuiPalette.readable(foreground == -16777216 ? -10327438 : -7169374, backgrounds, 4.5);
        this.SUCCESS = GuiPalette.readable(-12080792, backgrounds, 4.5);
        this.ERROR = GuiPalette.readable(-3450535, backgrounds, 4.5);
        this.SOFT_WARNING = this.WARNING = GuiPalette.readable(-4420845, backgrounds, 4.5);
        this.DISCOUNT = GuiPalette.readable(-12341912, backgrounds, 4.5);
        this.DIVIDER = this.BORDER = GuiPalette.readable(GuiPalette.mix(base, foreground, 0.45), backgrounds, 3.0);
        this.SELECTED_ACCENT = GuiPalette.readable(GuiPalette.mix(base, foreground, 0.7), backgrounds, 3.0);
    }

    public static GuiPalette fromHex(String hex) {
        String normalized = ShopColor.normalize(hex);
        return normalized.isEmpty() ? DEFAULT : new GuiPalette(0xFF000000 | Integer.parseInt(normalized.substring(1), 16));
    }

    public int[] surfaces() {
        return new int[]{this.PANEL, this.HEADER, this.SURFACE, this.ALTERNATE, this.SURFACE_HOVER, this.SURFACE_SELECTED, this.HEADER_BUTTON, this.HEADER_BUTTON_HOVER, this.UNAVAILABLE, this.SUCCESS_BOX, this.SUCCESS_BOX_HOVER, this.DANGER, this.DANGER_HOVER, this.INFO_BOX, this.HISTORY_SUCCESS, this.HISTORY_ERROR};
    }

    public int textOn(int preferred, int background) {
        return this.custom ? GuiPalette.readable(preferred, new int[]{background}, 4.5) : preferred;
    }

    int textRole(int color) {
        for (int role = 0; role < 8; ++role) {
            if (this.roleColor(role) != color) continue;
            return role;
        }
        return -1;
    }

    int roleColor(int role) {
        return switch (role) {
            case 0 -> this.TEXT;
            case 1 -> this.MUTED;
            case 2 -> this.DISABLED;
            case 3 -> this.SUCCESS;
            case 4 -> this.ERROR;
            case 5 -> this.WARNING;
            case 6 -> this.SOFT_WARNING;
            case 7 -> this.DISCOUNT;
            default -> throw new IllegalArgumentException("Unknown text role");
        };
    }

    private static int surface(int candidate, int foreground) {
        if (GuiPalette.contrast(candidate, foreground) >= 4.5) {
            return candidate;
        }
        int target = foreground ^ 0xFFFFFF;
        for (int step = 1; step <= 255; ++step) {
            int adjusted = GuiPalette.mix(candidate, target, (double)step / 255.0);
            if (!(GuiPalette.contrast(adjusted, foreground) >= 4.5)) continue;
            return adjusted;
        }
        return target;
    }

    private static int separated(int base, int endpoint, int foreground, double minimum) {
        return GuiPalette.separatedFrom(base, base, endpoint, foreground, minimum);
    }

    private static int separatedFrom(int background, int start, int endpoint, int foreground, double minimum) {
        for (int step = 0; step <= 255; ++step) {
            int candidate = GuiPalette.mix(start, endpoint, (double)step / 255.0);
            if (!(GuiPalette.contrast(background, candidate) >= minimum) || !(GuiPalette.contrast(candidate, foreground) >= 4.5)) continue;
            return candidate;
        }
        return endpoint;
    }

    static int readable(int preferred, int[] backgrounds, double minimum) {
        if (GuiPalette.meets(preferred, backgrounds, minimum)) {
            return preferred;
        }
        int target = GuiPalette.worstContrast(-16777216, backgrounds) >= GuiPalette.worstContrast(-1, backgrounds) ? -16777216 : -1;
        for (int step = 1; step <= 255; ++step) {
            int adjusted = GuiPalette.mix(preferred, target, (double)step / 255.0);
            if (!GuiPalette.meets(adjusted, backgrounds, minimum)) continue;
            return adjusted;
        }
        return target;
    }

    private static boolean meets(int color, int[] backgrounds, double minimum) {
        return GuiPalette.worstContrast(color, backgrounds) >= minimum;
    }

    private static double worstContrast(int color, int[] backgrounds) {
        double result = 21.0;
        for (int background : backgrounds) {
            result = Math.min(result, GuiPalette.contrast(color, background));
        }
        return result;
    }

    public static double contrast(int first, int second) {
        double a = GuiPalette.luminance(first);
        double b = GuiPalette.luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(int rgb) {
        return 0.2126 * GuiPalette.linear(rgb >>> 16 & 0xFF) + 0.7152 * GuiPalette.linear(rgb >>> 8 & 0xFF) + 0.0722 * GuiPalette.linear(rgb & 0xFF);
    }

    private static double linear(int channel) {
        double value = (double)channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int mix(int from, int to, double amount) {
        int rgb = -16777216;
        for (int shift : new int[]{16, 8, 0}) {
            int a = from >>> shift & 0xFF;
            int b = to >>> shift & 0xFF;
            rgb |= (int)Math.round((double)a + (double)(b - a) * amount) << shift;
        }
        return rgb;
    }
}

