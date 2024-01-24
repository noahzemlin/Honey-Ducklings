package honeyducklings;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    private static Random random;
    private static MapLocation[] allySpawnLocations;
    private static Team enemyTeam; // Probably saves bytecode over doing rc.getTeam().opponent() each time?
    private static MapLocation huddle; // The average location of spawn locations as a "retreat" location
    private static boolean isCommander = false; // Equivalent to duckId == 0, but useful for verbosity
    private static int duckId = 0; // Turn order
    private static int attacks = 0; // Track number of attacks between trap placement
    private static FlagStatus carryingFlag = null; // The FlagStatus of the flag we are carrying, null if not carrying a flag
    private static final FlagStatus[] flags = new FlagStatus[3]; // Status of the three enemy flags
    private static FlagInfo[] visibleFlags;

    public static Direction[] reasonableDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * Runs once at the beginning of the match
     */
    public static void setup(RobotController rc) throws GameActionException {
        duckId = rc.readSharedArray(0);
        rc.writeSharedArray(0, duckId + 1);

        if (duckId == 0) {
            isCommander = true;
        }

        random = new Random(2718);
        enemyTeam = rc.getTeam().opponent();
        allySpawnLocations = rc.getAllySpawnLocations();

        int sumSpawnLocationX = 0;
        int sumSpawnLocationY = 0;

        for (MapLocation spawnLocation : allySpawnLocations) {
            sumSpawnLocationX += spawnLocation.x;
            sumSpawnLocationY += spawnLocation.y;
        }

        int averageSpawnLocationX = (int) (sumSpawnLocationX / (double) (allySpawnLocations.length));
        int averageSpawnLocationY = (int) (sumSpawnLocationY / (double) (allySpawnLocations.length));

        huddle = new MapLocation(averageSpawnLocationX, averageSpawnLocationY);

        flags[0] = new FlagStatus(HoneyConstants.ARR_FLAG_1);
        flags[1] = new FlagStatus(HoneyConstants.ARR_FLAG_2);
        flags[2] = new FlagStatus(HoneyConstants.ARR_FLAG_3);
    }

    // Run one turn for this duck
    public static void runStep(RobotController rc) throws GameActionException {
        // Spawn if we aren't spawned
        if (!rc.isSpawned()) {

            boolean didSpawn = attemptToSpawn(rc);

            if (!didSpawn) {
                return;
            }
        }

        // Place mines on spawn
        attemptToPlaceMine(rc);

        // Sense surroundings
        senseNearby(rc);

        // Command our honey duckling army!
        if (isCommander) {
            Commander.commandTheLegion(rc);
        }

        // Pick up flag if we can
        attemptToPickupFlag(rc);

        // If we are a healer duck, try to heal first
        // Otherwise, only heal *after* we've moved to prioritize attacking always
        if (duckId % HoneyConstants.HEALER_EVERY_NTH_DUCK == 0) {
            attemptToHeal(rc);
        }

        // Attempt to attack before moving (in case we move out of attack range)
        attemptToAttack(rc);

        // Move if we can
        attemptToMove(rc, findTargetLocation(rc));

        // Try to heal or attack again now that we may have moved
        if (duckId % HoneyConstants.HEALER_EVERY_NTH_DUCK == 0) {
            attemptToHeal(rc);
            attemptToAttack(rc);
        } else {
            attemptToAttack(rc);
            attemptToHeal(rc);
        }
    }

    private static void attemptToPlaceMine(RobotController rc) throws GameActionException {
        // Place water mines in a lattice on our spawn locations
        // Effectively guarantees victory against any agent that doesn't have filling implemented
        // Also punishes agents that use too many crumbs and can't fill

        if (rc.getCrumbs() < HoneyConstants.MIN_CRUMBS_FOR_SPAWN_TRAPS) {
            return;
        }

        for (MapLocation location : allySpawnLocations) {
            if (((location.x + location.y) % 2) == 0) {
                if (rc.canBuild(TrapType.WATER, location)) {
                    rc.build(TrapType.WATER, location);
                }
            }
        }
    }

    public static void run(RobotController rc) {
        try {
            setup(rc);
        } catch (GameActionException e) {
            System.out.println("Failed on setup!");
            e.printStackTrace();
        }

        while (true) try {
            runStep(rc);
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
    }

    public static boolean attemptToSpawn(RobotController rc) throws GameActionException {

        // Attempt to spawn closest to the target location
        MapLocation targetLocation = allySpawnLocations[duckId % allySpawnLocations.length];

        MapLocation command = Utils.locationFromArray(rc, HoneyConstants.ARR_COMMAND);
        MapLocation commandSecondary = Utils.locationFromArray(rc, HoneyConstants.ARR_COMMAND_SECONDARY);

        if (command != null) {
            targetLocation = command;
        }

        if (duckId % 2 == 1 && commandSecondary != null) {
            targetLocation = commandSecondary;
        }

        MapLocation bestSpawn = null;
        int bestSpawnDistToCommand = 1000000;
        for (MapLocation spawnLocation : allySpawnLocations) {
            if (rc.canSpawn(spawnLocation) && spawnLocation.distanceSquaredTo(targetLocation) < bestSpawnDistToCommand) {
                bestSpawn = spawnLocation;
                bestSpawnDistToCommand = spawnLocation.distanceSquaredTo(targetLocation);
            }
        }

        if (bestSpawn != null && rc.canSpawn(bestSpawn)) {
            rc.spawn(bestSpawn);
            attacks = 0;
        }

        return false;
    }

    private static void senseNearby(RobotController rc) throws GameActionException {

        // Look for nearby flags
        visibleFlags = rc.senseNearbyFlags(-1);
        for (FlagInfo flagInfo : visibleFlags) {
            if (flagInfo.getTeam().equals(enemyTeam)) {
                for (FlagStatus flagStatus : flags) {
                    int currentId = flagStatus.fetchId(rc);

                    // If we already have that flag as an ID, update it's location
                    if (currentId == flagInfo.getID()) {
                        flagStatus.reportLocation(rc, flagInfo);
                        break;
                    }

                    // If we haven't seen this ID, set it to the next available FlagStatus slot
                    if (currentId == -1) {
                        flagStatus.setID(rc, flagInfo);
                        break;
                    }
                }
            }
        }
    }

    public static boolean attemptToMove(RobotController rc, MapLocation targetLocation) throws GameActionException {
        if (!rc.isMovementReady()) {
            return false;
        }

        Direction toTarget = PathPlanner.planRoute(rc, rc.getLocation(), targetLocation);

        // Move randomly if planning fails (should rarely happen, if ever)
        if (toTarget == null) {
            toTarget = reasonableDirections[random.nextInt(reasonableDirections.length)];
        }

        // Fill water if we want to go that way
        if (rc.canFill(rc.getLocation().add(toTarget))) {
            rc.fill(rc.getLocation().add(toTarget));
        }

        // Now move if we can
        if (rc.canMove(toTarget)) {
            rc.move(toTarget);

            // Build trap behind us IF we have recent kills
            if (attacks >= HoneyConstants.MIN_ATTACKS_BETWEEN_COMBAT_TRAPS && rc.canBuild(HoneyConstants.COMBAT_TRAP_TYPE, rc.getLocation()) && rc.getCrumbs() >= HoneyConstants.MIN_CRUMBS_FOR_COMBAT_TRAP) {
                rc.build(HoneyConstants.COMBAT_TRAP_TYPE, rc.getLocation());
                attacks = 0;
            }

            return true;
        }

        return false;
    }

    public static MapLocation findTargetLocation(RobotController rc) throws GameActionException {

        // Generate many target locations
        // Each test overwrites the previous as a "priority"

        MapLocation targetLocation = huddle;

        MapLocation command = Utils.locationFromArray(rc, HoneyConstants.ARR_COMMAND);
        MapLocation commandSecondary = Utils.locationFromArray(rc, HoneyConstants.ARR_COMMAND_SECONDARY);

        if (command != null) {
            targetLocation = command;
        }

        if (duckId % 2 == 1 && commandSecondary != null) {
            targetLocation = commandSecondary;
        }

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(4, rc.getTeam());

        // If we have nearby allies, use heuristic to move away from them in general
        if (nearbyAllies.length > 0) {
            Direction bestDirection = Direction.CENTER;
            double bestHeuristic = -100000;

            for (Direction direction : Direction.values()) {
                MapLocation directionPlacement = rc.getLocation().add(direction);
                double heuristic = directionPlacement.distanceSquaredTo(Utils.getNearestRobot(directionPlacement, nearbyAllies));

                if (heuristic > bestHeuristic) {
                    bestHeuristic = heuristic;
                    bestDirection = direction;
                }
            }

            if (bestDirection != Direction.CENTER) {
                targetLocation = rc.getLocation().add(bestDirection);
            }
        }

        // If we see a flag, go get it
        for(FlagInfo flag : visibleFlags) {
            if (flag.getTeam().equals(enemyTeam) && !flag.isPickedUp()) {
                targetLocation = flag.getLocation();
            }

            // Don't swarm the poor flag carrier :(
            if (flag.getTeam().equals(enemyTeam) && flag.isPickedUp() && flag.getLocation().distanceSquaredTo(targetLocation) <= 4) {
                Direction back = targetLocation.directionTo(rc.getLocation());
                targetLocation = targetLocation.add(back).add(back).add(back);
            }
        }

        // Early game mine placement
        if (rc.getRoundNum() < HoneyConstants.EARLY_GAME_STRAT_ROUNDS) {
            // Find nearest spawn location
            MapLocation nearestSpawnLocation = allySpawnLocations[0];
            for (int i=0; i<allySpawnLocations.length; i++) {
                MapLocation allySpawnLocation = allySpawnLocations[i];

                if (rc.getLocation().distanceSquaredTo(allySpawnLocation) < rc.getLocation().distanceSquaredTo(nearestSpawnLocation)) {
                    nearestSpawnLocation = allySpawnLocation;
                }
            }

            targetLocation = nearestSpawnLocation;
        }

        // Always go to flag if we have one
        if (rc.hasFlag()) {

            // Find nearest spawn location
            MapLocation nearestSpawnLocation = allySpawnLocations[0];
            for (int i=0; i<allySpawnLocations.length; i++) {
                MapLocation allySpawnLocation = allySpawnLocations[i];

                if (rc.getLocation().distanceSquaredTo(allySpawnLocation) < rc.getLocation().distanceSquaredTo(nearestSpawnLocation)) {
                    nearestSpawnLocation = allySpawnLocation;
                }
            }
            targetLocation = nearestSpawnLocation;

            // Mark as captured if we are within 2 tiles (???)
            if (carryingFlag != null && rc.getLocation().distanceSquaredTo(nearestSpawnLocation) <= 4) {
                System.out.println("[Duck] Marking " + carryingFlag.id + " as captured!");
                carryingFlag.capture(rc);
                carryingFlag = null;
            }
        }

        // If we had the flag but now we don't, let the commander know
        if (!rc.hasFlag() && carryingFlag != null) {
            System.out.println("[Duck] Marking " + carryingFlag.id + " as dropped!");
            carryingFlag.drop(rc);
            carryingFlag = null;
        }

        // Collect crumbs if we see them
        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
        if (crumbs.length > 0) {
            targetLocation = crumbs[0];
        }

        // If there are nearby enemies, go into "battle" mode and focus on attacking and micro
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(20, enemyTeam);
        if (nearbyEnemies.length > 0) {
            Direction bestDirection = Direction.CENTER;
            double bestHeuristic = -100000;

            // Choose the direction which has the highest value according to a heuristic
            for (Direction direction : Direction.values()) {
                MapLocation directionPlacement = rc.getLocation().add(direction);
                double heuristic = HoneyConstants.MICRO_HEURISTIC_SUM_ENEMY_PRIORITY * Utils.getSumRobotDistance(directionPlacement, nearbyEnemies)  +
                                HoneyConstants.MICRO_HEURISTIC_NEAREST_ENEMY_PRIORITY * Math.sqrt(directionPlacement.distanceSquaredTo(Utils.getNearestRobot(directionPlacement, nearbyEnemies))) +
                                HoneyConstants.MICRO_HEURISTIC_SUM_ALLY_PRIORITY * Utils.getSumRobotDistance(directionPlacement, nearbyAllies);

                if (heuristic > bestHeuristic) {
                    bestHeuristic = heuristic;
                    bestDirection = direction;
                }
            }

            targetLocation = rc.getLocation().add(bestDirection);
        }

        if (targetLocation == null) {
            targetLocation = huddle;
        }

        rc.setIndicatorLine(rc.getLocation(), targetLocation, 200, 100, 100);

        return targetLocation;
    }

    public static RobotInfo attemptToHeal(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), 4, rc.getTeam());
        for (RobotInfo ally : allies) {
            // Heal the first ally we see that wouldn't waste healing
            // We should probably prioritize non-healer ducks here, but that's a future problem
            if (ally.health < (1000 - rc.getHealAmount()) && rc.canHeal(ally.getLocation())) {
                rc.heal(ally.getLocation());
                return ally;
            }
        }

        return null;
    }

    public static FlagInfo attemptToPickupFlag(RobotController rc) throws GameActionException {
        for (FlagInfo flagInfo : visibleFlags) {
            if (flagInfo.getTeam().equals(enemyTeam) && rc.canPickupFlag(flagInfo.getLocation())) {
                rc.pickupFlag(flagInfo.getLocation());

                // We picked up a flag, now relay that to everyone else
                for (FlagStatus flagStatus : flags) {
                    if (flagStatus.fetchId(rc) == flagInfo.getID()) {
                        flagStatus.pickup(rc, flagInfo);
                        carryingFlag = flagStatus;
                        return flagInfo;
                    }
                }

                System.out.println("Picked up flag without an ID!");
                System.out.println(flags[0].fetchId(rc) + ", " + flags[1].fetchId(rc) + ", " + flags[2].fetchId(rc));

                return flagInfo;
            }
        }

        return null;
    }

    public static RobotInfo attemptToAttack(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), 4, enemyTeam);
        if (enemies.length > 0) {
            RobotInfo bestEnemy = enemies[0];
            for (int i = 1; i < enemies.length; i++) {
                if (!rc.canAttack(enemies[i].location)) {
                    continue;
                }

                // If we can finish them off, it's always the best option
                if (enemies[i].health < rc.getAttackDamage()) {
                    bestEnemy = enemies[i];
                    break;
                }

                // Prioritize flag carriers
                if (enemies[i].hasFlag()) {
                    bestEnemy = enemies[i];
                    break;
                }

                // Otherwise, choose the enemy that is weakest
                if (enemies[i].health < bestEnemy.health && rc.canAttack(enemies[i].getLocation())) {
                    bestEnemy = enemies[i];
                }
            }

            if (rc.canAttack(bestEnemy.getLocation())) {
                rc.attack(bestEnemy.getLocation());
                attacks++;
                return bestEnemy;
            }
        }

        return null;
    }
}
