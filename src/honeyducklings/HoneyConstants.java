package honeyducklings;

import battlecode.common.TrapType;

public class HoneyConstants {

    //// Configuration Constants ////

    public static final int HEALER_EVERY_NTH_DUCK = 7; // Every Nth duck will prioritize healing
    public static final int MIN_CRUMBS_FOR_SPAWN_TRAPS = 190;
    public static final int MIN_CRUMBS_FOR_COMBAT_TRAP = 290;
    public static final int MIN_ATTACKS_BETWEEN_COMBAT_TRAPS = 10;
    public static final TrapType COMBAT_TRAP_TYPE = TrapType.EXPLOSIVE;
    public static final int EARLY_GAME_STRAT_ROUNDS = 50;
    public static final double MICRO_HEURISTIC_SUM_ENEMY_PRIORITY = 5.0;
    public static final double MICRO_HEURISTIC_NEAREST_ENEMY_PRIORITY = -20.0;
    public static final double MICRO_HEURISTIC_SUM_ALLY_PRIORITY = 3.0;
    public static final int PATH_PLANNING_MAX_ROUNDS_ON_OBJECT = 15;

    //// Shared Array Constants ////

    // Indices
    //  0: Duck ID Claiming
    //  1: Command Primary
    //  2: Command Secondary
    //  3: Flag 1 Status start
    //  9: Flag 2 Status start
    // 15: Flag 3 Status start

    public static final int ARR_COMMAND = 1;
    public static final int ARR_COMMAND_SECONDARY = 2;
    public static final int ARR_FLAG_1 = 3;
    public static final int ARR_FLAG_2 = 9;
    public static final int ARR_FLAG_3 = 15;

    // Flag status index offset
    //  +0: Flag ID
    //  +1: Flag Location
    //  +2: Flag Set Location
    //  +3: Carried
    //  +4: Dropped
    //  +5: Captured

    public final static int FLAG_ID = 0;
    public final static int FLAG_LOC = 1;
    public final static int FLAG_SET_LOC = 2;
    public final static int FLAG_CARRIED = 3;
    public final static int FLAG_DROPPED = 4;
    public final static int FLAG_CAPTURED = 5;
}
