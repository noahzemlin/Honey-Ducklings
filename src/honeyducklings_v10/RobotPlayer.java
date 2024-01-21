package honeyducklings_v10;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {

    // Shared Array Values
    //  0: Duck ID Claiming
    //  1: Command Primary
    //  2: Command Secondary
    //  3: Flag 1 ID
    //   : Flag 1 Location
    //   : Flag 1 Set Location
    //   : Flag 1 Carried
    //   : Flag 1 Dropped
    //   : Flag 1 Captured
    //  9: Flag 2 ID
    //   : Flag 2 Location
    //   : Flag 2 Set Location
    //   : Flag 2 Carried
    //   : Flag 2 Dropped
    //   : Flag 2 Captured
    // 15: Flag 3 ID
    //   : Flag 3 Location
    //   : Flag 3 Set Location
    //   : Flag 3 Carried
    //   : Flag 3 Dropped
    //   : Flag 3 Captured

    public static final int ARR_COMMAND = 1;
    public static final int ARR_COMMAND_SECONDARY = 2;
    private static MapLocation[] allySpawnLocations;
    private static Team ourTeam;
    private static Team enemyTeam;
    private static MapLocation huddle;
    private static Random random;
    private static boolean isCommander = false;
    private static int duckId = 0;
    private static int attacks = 0;
    private static int retreatingTicks = 0;
    private static FlagStatus carryingFlag = null;
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
    private static final FlagStatus[] flags = new FlagStatus[3];
    private static FlagInfo[] visibleFlags;
    private static boolean amDead = false;

    public static void setup(RobotController rc) throws GameActionException {

        duckId = rc.readSharedArray(0);
        rc.writeSharedArray(0, duckId + 1);

        if (duckId == 0) {
            isCommander = true;
        }

        random = new Random(2718);

        ourTeam = rc.getTeam();
        enemyTeam = ourTeam.opponent();

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

        flags[0] = new FlagStatus(3);
        flags[1] = new FlagStatus(9);
        flags[2] = new FlagStatus(15);
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

        RobotInfo attackedRobot = attemptToAttack(rc);;

        if (attackedRobot != null && rc.getLocation().distanceSquaredTo(attackedRobot.location) >= 4) {
            // Try to kite
            Direction kiteDirection = attackedRobot.location.directionTo(rc.getLocation());
            if (rc.canMove(kiteDirection)) {
                rc.move(kiteDirection);
            }
        }

        // Move if we can
        attemptToMove(rc, findTargetLocation(rc));

        // Some ducks will try to heal first (healers)
        // Some will try to attack first (attackers)
        if (duckId % 3 == 0) {
            attemptToHeal(rc);
            attemptToAttack(rc);
        } else {
            attemptToAttack(rc);
            attemptToHeal(rc);
        }
    }

    private static void attemptToPlaceMine(RobotController rc) throws GameActionException {
        if (rc.getCrumbs() < 190) {
            return;
        }

        for (MapLocation location : allySpawnLocations) {
            if (((location.x + location.y) % 2) == 0) {
                if (rc.canBuild(TrapType.WATER, location)) {
                    rc.build(TrapType.WATER, location);
                }
            } else {
                if (rc.canBuild(TrapType.STUN, location)) {
                    rc.build(TrapType.STUN, location);
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
        for (MapLocation spawnLocation : allySpawnLocations) {
            if (random.nextInt(5) <= 3) {
                continue;
            }
            if (rc.canSpawn(spawnLocation)) {
                rc.spawn(spawnLocation);
                attacks = 0;
                return true;
            }
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

        if (rc.canMove(toTarget)) {
            MapLocation prevLocation = rc.getLocation();
            rc.move(toTarget);

            // Build trap behind us IF we have recent kills
            if (attacks >= 10 && rc.canBuild(TrapType.EXPLOSIVE, prevLocation) && rc.getCrumbs() >= 350) {
                rc.build(TrapType.EXPLOSIVE, prevLocation);
                attacks = 0;
            }

            return true;
        }

        return false;
    }

    public static MapLocation findTargetLocation(RobotController rc) throws GameActionException {
        MapLocation targetLocation = huddle;

        MapLocation command = Utils.locationFromArray(rc, ARR_COMMAND);
        MapLocation commandSecondary = Utils.locationFromArray(rc, ARR_COMMAND_SECONDARY);

        if (command != null) {
            targetLocation = command;
        }

        if (duckId % 2 == 1 && commandSecondary != null) {
            targetLocation = commandSecondary;
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
        if (rc.getRoundNum() < 80) {
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

        if (!rc.hasFlag() && carryingFlag != null) {
            System.out.println("[Duck] Marking " + carryingFlag.id + " as dropped!");
            carryingFlag.drop(rc);
            carryingFlag = null;
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(20, enemyTeam);
        if (nearbyEnemies.length > 0) {

            MapLocation nearestEnemy = nearbyEnemies[0].location;

            for (int i=1; i<nearbyEnemies.length; i++) {
                if (rc.getLocation().distanceSquaredTo(nearbyEnemies[i].location) < rc.getLocation().distanceSquaredTo(nearestEnemy)) {
                    nearestEnemy = nearbyEnemies[i].location;
                }
            }

            Direction bestKiteDirection = Direction.CENTER;
            int bestDistance = 10000;

            for (Direction direction : Direction.values()) {
                MapLocation directionPlacement = rc.getLocation().add(direction);
                int distance = nearestEnemy.distanceSquaredTo(directionPlacement);

                if (distance >= 4 && distance < bestDistance) {
                    bestDistance = distance;
                    bestKiteDirection = direction;
                }
            }

            if (bestKiteDirection != Direction.CENTER) {
                targetLocation = rc.getLocation().add(bestKiteDirection);
            }
        }

        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
        if (crumbs.length > 0) {
            targetLocation = crumbs[0];
        }

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(20, ourTeam);
        if (nearbyAllies.length > 0) {
            MapLocation nearestAlly = nearbyAllies[0].location;

            for (int i=1; i<nearbyAllies.length; i++) {
                if (rc.getLocation().distanceSquaredTo(nearbyAllies[i].location) < rc.getLocation().distanceSquaredTo(nearestAlly)) {
                    nearestAlly = nearbyAllies[i].location;
                }
            }

            if (rc.getLocation().distanceSquaredTo(nearestAlly) >= 9) {
                rc.setIndicatorString("Going to nearby ally!");
                targetLocation = nearestAlly;
            }
        } else {
            targetLocation = huddle;
        }

        if (targetLocation == null) {
            targetLocation = huddle;
        }

        rc.setIndicatorLine(rc.getLocation(), targetLocation, 200, 100, 100);

        return targetLocation;
    }

    public static RobotInfo attemptToHeal(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), 4, ourTeam);
        for (RobotInfo ally : allies) {
            if (ally.health < 920 && rc.canHeal(ally.getLocation())) {
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
