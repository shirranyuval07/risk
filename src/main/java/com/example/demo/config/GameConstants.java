package com.example.demo.config;

/**
 * GameConstants - קבועים מרכזיים לכל המשחק Risk
 */
public final class GameConstants {
    
    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }

    // ==================== DICE CONSTANTS ====================
    /** Minimum value on a single die roll */
    public static final int DICE_MIN_VALUE = 1;
    
    /** Maximum value on a single die roll */
    public static final int DICE_MAX_VALUE = 6;
    
    /** Maximum number of dice an attacker can roll */
    public static final int MAX_ATTACKER_DICE = 3;
    
    /** Maximum number of dice a defender can roll */
    public static final int MAX_DEFENDER_DICE = 2;



    // ==================== GAME MECHANICS CONSTANTS ====================
    /** Minimum number of armies required to move from a country (always leave 1 behind) */
    public static final int MIN_ARMIES_TO_STAY = 1;
    
    /** Minimum reinforcement armies per turn */
    public static final int MIN_REINFORCEMENT = 3;
    
    /** Divisor for calculating reinforcements based on territories */
    public static final int REINFORCEMENT_DIVISOR = 3;
    
    /** Minimum armies a defending country can have during attack consideration */
    public static final int MIN_ARMIES_FOR_DEFENSE_CHECK = 1;

    /** Starting armies for setup phase*/
    public static final int STARTING_ARMIES = 50;

    /** Max Players in a game */
    public static final int MAX_PLAYERS = 6;
    
    
    // ==================== MAP DISPLAY CONSTANTS ====================
    /** Scale factor for the global map display */
    public static final double MAP_GLOBAL_SCALE = 1.35;
    
    /** X-axis offset for the global map display */
    public static final double MAP_GLOBAL_OFFSET_X = -250.0;
    
    /** Y-axis offset for the global map display */
    public static final double MAP_GLOBAL_OFFSET_Y = -150.0;
    


    // ==================== UI CONSTANTS ====================

    
    /** Small spacing for compact UI */
    public static final double UI_SMALL_SPACING = 5.0;
    
    /** Inset padding for boxes */
    public static final double BOX_PADDING = 10.0;

    // ==================== COLOR & GRAPHICS CONSTANTS ====================

    /** Opacity for background overlays */
    public static final double OVERLAY_OPACITY_HIGH = 0.85;
    
    /** Opacity for subtle overlays */
    public static final double OVERLAY_OPACITY_MEDIUM = 0.15;
    
    /** Opacity for very subtle overlays */
    public static final double OVERLAY_OPACITY_LOW = 0.05;
    
    /** Opacity for sea route display */
    public static final double SEA_ROUTE_OPACITY = 0.5;
    
    /** Stroke width for normal borders */
    public static final double BORDER_WIDTH_NORMAL = 1.0;
    
    /** Stroke width for highlighted borders */
    public static final double BORDER_WIDTH_HIGHLIGHT = 4.0;
    
    /** Stroke width for special borders (Asia-Europe boundary) */
    public static final double BORDER_WIDTH_SPECIAL = 2.5;
    
    /** Stroke width for sea routes */
    public static final double SEA_ROUTE_WIDTH = 2.5;

    // ==================== SEA ROUTE CONSTANTS ====================
    /** Dash array length for sea route visualization */
    public static final double SEA_ROUTE_DASH_LENGTH = 8.0;
    
    /** Gap length for sea route visualization */
    public static final double SEA_ROUTE_GAP_LENGTH = 6.0;
    
    /** Offset for Alaska-Kamchatka route visualization (left side) */
    public static final double ALASKA_ROUTE_OFFSET_X = -150.0;
    
    /** Offset for Alaska-Kamchatka route visualization (vertical) */
    public static final double ALASKA_ROUTE_OFFSET_Y = 20.0;
    
    /** Offset for Kamchatka-Alaska route visualization (right side) */
    public static final double KAMCHATKA_ROUTE_OFFSET_X = 150.0;
    
    /** Offset for Kamchatka-Alaska route visualization (vertical) */
    public static final double KAMCHATKA_ROUTE_OFFSET_Y = -20.0;

    // ==================== FONT CONSTANTS ====================
    /** Main font size for titles */
    public static final double FONT_SIZE_TITLE = 20.0;
    
    /** Font size for headers */
    public static final double FONT_SIZE_HEADER = 16.0;
    
    /** Font size for body text */
    public static final double FONT_SIZE_BODY = 14.0;

    // ==================== AI CONSTANTS ====================
    /** Maximum threats to check for AI strategies */
    public static final int MAX_ISOLATION_CHECK = 3;
    
    /** Bonus multiplier for isolated targets */
    public static final double ISOLATION_BONUS_MULTIPLIER = 0.5;
    
    /** Minimum bonus value (used when no bonus applies) */
    public static final double MINIMUM_BONUS = 0.0;
    
    /** Armor advantage divisor for an aftermath of conflict */
    public static final int ARMOR_DIVISOR = 2;
    

    /** Threat multiplier for bottleneck countries */
    public static final double BOTTLENECK_THREAT_MULTIPLIER = 2.0;
    
    /** Minimum armies required for fortifying */
    public static final int MIN_ARMIES_FOR_FORTIFY = 3;
    
    /** Armies to keep when moving armies to border */
    public static final int KEEP_ARMIES_AT_SOURCE = 2;
    
    /** Balanced strategy attack threshold */
    public static final double BALANCED_ATTACK_THRESHOLD = 0.2;

    // ==================== EASY WIN BONUS CONSTANTS ====================
    /** Base multiplier for easy win bonuses (scales all endgame bonuses) */
    public static final double EASY_WIN_BONUS_BASE = 1.0;
    
    /** Divisor for calculating easy win bonus based on enemy armies (lower = bigger bonus) */
    public static final int EASY_WIN_ARMY_DIVISOR = 5;
    
    /** Max territories before endgame bonus kicks in (1-2 territories = endgame) */
    public static final int EASY_WIN_TERRITORY_THRESHOLD = 2;
    
    /** Min enemy neighbors to qualify for endgame bonus (must be surrounded) */
    public static final int EASY_WIN_MIN_NEIGHBORS = 2;

    /** Max enemy neighbors to qualify for endgame bonus (must be surrounded) */
    public static final int EASY_WIN_MAX_NEIGHBORS = 4;

    /** Max armies for endgame bonus (too strong = doesn't get bonus) */
    public static final int EASY_WIN_ARMY_THRESHOLD = 50;
    
    /** Multiplier for strong endgame position (1-2 territories + surrounded and weak) */
    public static final double EASY_WIN_STRONG_POSITION_MULTIPLIER = 10.0;
    
    /** Multiplier for final territory (guaranteed win soon) */
    public static final double EASY_WIN_FINAL_TERRITORY_MULTIPLIER = 5.0;

    // ==================== COUNTRY ID CONSTANTS ====================
    /** Country ID for Alaska */
    public static final int ALASKA_ID = 1;
    
    /** Country ID for Kamchatka */
    public static final int KAMCHATKA_ID = 30;

    // ==================== HEX COLOR CONSTANTS ====================
    /** Hex value 255 for color component */
    public static final int COLOR_HEX_MAX = 255;
}

