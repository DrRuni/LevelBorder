package runi.myddns.levelborder.Utils;

@SuppressWarnings("unused")
public class ConsoleColor {

    // 🔁 Reset
    public static final String RESET = "\u001B[0m";

    // 🖌️ Textformate
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String REVERSED = "\u001B[7m";

    // 🧩 Plugin / Systemfarben
    public static final String MANAGER = "\u001B[38;5;33m";           // Light Blue
    public static final String PLUGIN  = "\u001B[38;2;255;95;31m";    // Neon Orange
    public static final String WORLD   = "\u001B[38;5;29m";           // Petrol

    // 🎨 ANSI Standardfarben (8)
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String PURPLE  = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    // 🌈 ANSI Helle Farben (16)
    public static final String LIGHT_BLACK   = "\u001B[90m";
    public static final String LIGHT_RED     = "\u001B[91m";
    public static final String BRIGHT_GREEN  = "\u001B[92m";
    public static final String LIGHT_YELLOW  = "\u001B[93m";
    public static final String LIGHT_PURPLE  = "\u001B[95m";
    public static final String LIGHT_CYAN    = "\u001B[96m";
    public static final String LIGHT_WHITE   = "\u001B[97m";
    public static final String LIGHT_BLUE    = "\u001B[38;5;33m";
    public static final String LIGHT_PETROL  = "\u001B[38;5;30m";

    // ⚪ Dunkle Farben
    public static final String DARK_PETROL = "\u001B[38;5;23m";
    public static final String DARK_PURPLE = "\u001B[38;2;75;0;130m";// Indigo / Dark Purple
    public static final String DARK_AQUA = "\u001B[38;5;31m"; // Dark Aqua / Türkis
    public static final String DARK_RED = "\u001B[38;2;139;0;0m"; // Dark Red
    public static final String DARK_LIME = "\u001B[38;2;111;191;58m";
    public static final String DEEP_YELLOW_GREEN = "\u001B[38;2;94;159;52m";
    public static final String DARK_GOLDEN_LIME = "\u001B[38;2;122;154;51m";

    // ⚪ Grautöne
    public static final String SOFT_WHITE = "\u001B[38;2;245;245;245m";
    public static final String LIGHT_GREY = "\u001B[38;2;211;211;211m";
    public static final String SILVER     = "\u001B[38;2;192;192;192m";
    public static final String GRAY       = "\u001B[38;2;128;128;128m";
    public static final String DARK_GREY  = "\u001B[38;2;105;105;105m";
    public static final String DIM_GREY   = "\u001B[38;2;85;85;85m";
    public static final String BLACK      = "\u001B[38;2;0;0;0m";

    // 🌌 Ruhige / edle Systemfarben
    public static final String SLATE_BLUE = "\u001B[38;2;106;90;205m";
    public static final String STEEL_BLUE = "\u001B[38;2;70;130;180m";
    public static final String MIDNIGHT   = "\u001B[38;2;25;25;112m";
    public static final String CHARCOAL   = "\u001B[38;2;54;69;79m";
    public static final String GRAPHITE   = "\u001B[38;2;65;74;76m";
    public static final String MUTED_BLUE = "\u001B[38;2;120;144;156m";

    // 🌿 Naturfarben
    public static final String FOREST_GREEN = "\u001B[38;2;34;139;34m";
    public static final String MOSS         = "\u001B[38;2;124;252;0m";
    public static final String SAGE         = "\u001B[38;2;152;193;152m";
    public static final String TEAL         = "\u001B[38;2;0;128;128m";
    public static final String SEAFOAM      = "\u001B[38;2;159;226;191m";
    public static final String CHARTREUSE = "\u001B[38;2;173;255;47m";

    // 🔥 Warme Akzentfarben
    public static final String AMBER     = "\u001B[38;2;255;191;0m";
    public static final String COPPER    = "\u001B[38;2;184;115;51m";
    public static final String BRONZE    = "\u001B[38;2;205;127;50m";
    public static final String WINE      = "\u001B[38;2;114;47;55m";
    public static final String MAHOGANY  = "\u001B[38;2;192;64;0m";
    public static final String BLOOD_ORANGE = "\u001B[38;2;255;69;0m";
    public static final String SOFT_LIME = "\u001B[38;2;183;229;106m";

    // 💎 Neon / starke Farben
    public static final String ELECTRIC_BLUE = "\u001B[38;2;44;117;255m";
    public static final String CYBER_GREEN   = "\u001B[38;2;0;255;153m";
    public static final String LASER_PURPLE  = "\u001B[38;2;199;36;177m";
    public static final String HOT_PINK      = "\u001B[38;2;255;105;180m";
    public static final String NEON_GREEN    = "\u001B[38;2;57;255;20m";
    public static final String NEON_BLUE     = "\u001B[38;2;77;77;255m";
    public static final String NEON_PINK     = "\u001B[38;2;255;20;147m";
    public static final String NEON_PURPLE  = "\u001B[38;2;190;0;255m";
    public static final String NEON_CYAN    = "\u001B[38;2;0;255;255m";
    public static final String NEON_YELLOW  = "\u001B[38;2;255;255;0m";

    // 🧡 256-Color ANSI
    public static final String ORANGE     = "\u001B[38;5;208m";
    public static final String GOLD       = "\u001B[38;5;220m";
    public static final String BROWN      = "\u001B[38;5;94m";
    public static final String TURQUOISE  = "\u001B[38;5;45m";
    public static final String SKY_BLUE   = "\u001B[38;5;75m";
    public static final String AQUA       = "\u001B[38;5;39m";
    public static final String LIME       = "\u001B[38;5;118m";
    public static final String MINT       = "\u001B[38;5;121m";
    public static final String OLIVE      = "\u001B[38;5;100m";
    public static final String PINK       = "\u001B[38;5;205m";
    public static final String VIOLET     = "\u001B[38;5;135m";
    public static final String ROSE       = "\u001B[38;5;203m";
    public static final String ICE_BLUE   = "\u001B[38;5;123m";
    public static final String PETROL     = "\u001B[38;5;24m";

    // 🧱 Hintergrundfarben
    public static final String BG_BLACK  = "\u001B[40m";
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE   = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN   = "\u001B[46m";
    public static final String BG_WHITE  = "\u001B[47m";

    // 🌈 TrueColor Helper
    public static String rgb(int r, int g, int b) {
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }
}