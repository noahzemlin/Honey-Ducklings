package honeyducklings;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {

    // Shared Array Values
    // 0: Captain claiming & Capture Status (11 = Captured 1, 22 = Captured 2, 33 = Captured 3)
    // 1: Flag 1 Location (0 = missing)
    // 2: Flag 2 Location (1 = missing)
    // 3: Flag 3 Location (2 = missing)
    // 4: Update Flag 1 Location
    // 5: Update Flag 2 Location
    // 6: Update Flag 3 Location
    // 7: Flag 1 being carried
    // 8: Flag 2 being carried
    // 9: Flag 3 being carried
    // 10: Command Primary (0 = do whatever, 1 = flag 1, 2 = flag 2, 3 = flag 3)
    // 11: Command Secondary (0 = do whatever, 1 = flag 1, 2 = flag 2, 3 = flag 3)

    public static final int ARR_FLAG1_LOC = 1;
    public static final int ARR_FLAG2_LOC = 2;
    public static final int ARR_FLAG3_LOC = 3;
    public static final int ARR_FLAG1_SET_LOC = 4;
    public static final int ARR_FLAG2_SET_LOC = 5;
    public static final int ARR_FLAG3_SET_LOC = 6;
    public static final int ARR_COMMAND = 7;
    public static final int ARR_COMMAND_SECONDARY = 8;

    private static MapLocation[] allySpawnLocations;
    private static Team ourTeam;
    private static Team enemyTeam;
    private static MapLocation huddle;
    private static Random random;
    private static boolean isCommander = false;
    private static int duckId = 0;
    private static int attacks = 0;
    private static int retreatingTicks = 0;
    private static MapLocation carryingFlag = null;
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

    public static void setup(RobotController rc) throws GameActionException {

//        if (rc.readSharedArray(0) == 0) {
//            rc.writeSharedArray(0, 1);
//            System.out.println("I am da captain now >:)");
//            isCommander = true;
//        }
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

        // Command our honey duckling army!
        if (isCommander) {
            Commander.commandTheLegion(rc);
        }

        // Pick up flag if we can
        attemptToPickupFlag(rc);

        // Attack if we see someone
        RobotInfo attackedRobot = attemptToAttack(rc);

        if (attackedRobot != null && rc.getLocation().distanceSquaredTo(attackedRobot.location) >= 4) {
            // Try to kite
            Direction kiteDirection = attackedRobot.location.directionTo(rc.getLocation());
            if (rc.canMove(kiteDirection)) {
                rc.move(kiteDirection);
            }
        }

        // Move if we can
        attemptToMove(rc, findTargetLocation(rc));

        // Try to attack again if new targets came in range
        attemptToAttack(rc);

        // Heal if we can
        attemptToHeal(rc);
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
            if (attacks >= 6 && rc.canBuild(TrapType.EXPLOSIVE, prevLocation)) {
                rc.build(TrapType.EXPLOSIVE, prevLocation);
                attacks = 0;
            }

            return true;
        }

        return false;
    }

    public static MapLocation findTargetLocation(RobotController rc) throws GameActionException {
        MapLocation targetLocation = huddle;

        int command = 0;
        if (duckId <= 25) {
            command = rc.readSharedArray(ARR_COMMAND);
        } else {
            command = rc.readSharedArray(ARR_COMMAND_SECONDARY);
        }

        switch (command) {
            case 1: targetLocation = locationFromArray(rc, ARR_FLAG1_LOC); break;
            case 2: targetLocation = locationFromArray(rc, ARR_FLAG2_LOC); break;
            case 3: targetLocation = locationFromArray(rc, ARR_FLAG3_LOC); break;
        }

        rc.setIndicatorString("Following command " + command + " to " + targetLocation);

        // If we see a flag, go get it
        FlagInfo[] visibleFlags = rc.senseNearbyFlags(-1);
        for(FlagInfo flag : visibleFlags) {
            if (flag.getTeam().equals(enemyTeam) && !flag.isPickedUp()) {
                targetLocation = visibleFlags[0].getLocation();
                updateFlagNearest(rc, targetLocation);
            }

            // We see an ally flag! Go save it!
//            if (flag.getTeam().equals(ourTeam) && flag.isPickedUp()) {
//                rc.writeSharedArray(2, flag.getLocation().x + flag.getLocation().y + 100 + 1);
//            }
        }

        // No allies! Scary! Return to huddle :(
        if (retreatingTicks == 0) {
            RobotInfo[] alliesNearby = rc.senseNearbyRobots(16, ourTeam);
            if (alliesNearby.length < 4) {
                targetLocation = huddle;
                retreatingTicks = 5;
            }
        } else if (retreatingTicks > 0) {
            targetLocation = huddle;
            retreatingTicks--;
            if (retreatingTicks == 0) {
                retreatingTicks = -20; //Confidence!!
            }
        } else {
            retreatingTicks++;
        }

        // Always go to flag if we have one
        if (rc.hasFlag()) {
            carryingFlag = rc.getLocation();
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
            if (rc.getLocation().distanceSquaredTo(nearestSpawnLocation) <= 1) {
                updateFlagNearestCaptured(rc, rc.getLocation());
            } else {
                // Update commander that we have it
                updateFlagNearest(rc, rc.getLocation());
            }
        }

        if (!rc.hasFlag() && carryingFlag != null) {
            updateFlagNearestReturned(rc, carryingFlag);
            carryingFlag = null;
        }

        if (targetLocation == null) {
            targetLocation = huddle;
        }

        return targetLocation;
    }

    private static void updateFlagNearestReturned(RobotController rc, MapLocation targetLocation) throws GameActionException {
        MapLocation[] flagsFromSharedArray = new MapLocation[3];
        flagsFromSharedArray[0] = locationFromArray(rc, ARR_FLAG1_LOC);
        flagsFromSharedArray[1] = locationFromArray(rc, ARR_FLAG2_LOC);
        flagsFromSharedArray[2] = locationFromArray(rc, ARR_FLAG3_LOC);

        int nearestIndex = -1;
        int nearestDistance = 101;
        for (int i = 0; i < 3; i++) {
            if (flagsFromSharedArray[i] == null) {
                continue;
            }

            int distance = flagsFromSharedArray[i].distanceSquaredTo(targetLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        if (nearestIndex >= 0) {
            System.out.println("Captured " + (nearestIndex + 1));
            switch (nearestIndex) {
                case 0: writeLocationToArray(rc, ARR_FLAG1_SET_LOC, new MapLocation(126, 126)); break;
                case 1: writeLocationToArray(rc, ARR_FLAG2_SET_LOC, new MapLocation(126, 126)); break;
                case 2: writeLocationToArray(rc, ARR_FLAG3_SET_LOC, new MapLocation(126, 126)); break;
            }
        } else {
            System.out.println("Tried marking flag as captured, but failed!!!!!!");
        }
    }

    public static void updateFlagNearest(RobotController rc, MapLocation targetLocation) throws GameActionException {
        MapLocation[] flagsFromSharedArray = new MapLocation[3];
        flagsFromSharedArray[0] = locationFromArray(rc, ARR_FLAG1_LOC);
        flagsFromSharedArray[1] = locationFromArray(rc, ARR_FLAG2_LOC);
        flagsFromSharedArray[2] = locationFromArray(rc, ARR_FLAG3_LOC);

        int nearestIndex = -1;
        int nearestDistance = 101;
        for (int i = 0; i < 3; i++) {
            if (flagsFromSharedArray[i] == null) {
                continue;
            }

            int distance = flagsFromSharedArray[i].distanceSquaredTo(targetLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        if (nearestIndex >= 0) {
            System.out.println("Updating flag at " + flagsFromSharedArray[nearestIndex] + " to be " + targetLocation);
            System.out.println(flagsFromSharedArray[0] + ", " + flagsFromSharedArray[1] + ", " + flagsFromSharedArray[2]);

            switch (nearestIndex) {
                case 0: writeLocationToArray(rc, ARR_FLAG1_SET_LOC, targetLocation); break;
                case 1: writeLocationToArray(rc, ARR_FLAG2_SET_LOC, targetLocation); break;
                case 2: writeLocationToArray(rc, ARR_FLAG3_SET_LOC, targetLocation); break;
            }
        } else {
//            System.out.println("Tried to update flag at " + targetLocation + "but doesn't exist!!!");
//            System.out.println(flagsFromSharedArray[0] + ", " + flagsFromSharedArray[1] + ", " + flagsFromSharedArray[2]);
        }
    }

    public static void updateFlagNearestCaptured(RobotController rc, MapLocation targetLocation) throws GameActionException {
        MapLocation[] flagsFromSharedArray = new MapLocation[3];
        flagsFromSharedArray[0] = locationFromArray(rc, ARR_FLAG1_LOC);
        flagsFromSharedArray[1] = locationFromArray(rc, ARR_FLAG2_LOC);
        flagsFromSharedArray[2] = locationFromArray(rc, ARR_FLAG3_LOC);

        int nearestIndex = -1;
        int nearestDistance = 101;
        for (int i = 0; i < 3; i++) {
            if (flagsFromSharedArray[i] == null) {
                continue;
            }

            int distance = flagsFromSharedArray[i].distanceSquaredTo(targetLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        if (nearestIndex >= 0) {
            System.out.println("Captured " + (nearestIndex + 1));
            switch (nearestIndex) {
                case 0: writeLocationToArray(rc, ARR_FLAG1_SET_LOC, new MapLocation(127, 127)); break;
                case 1: writeLocationToArray(rc, ARR_FLAG2_SET_LOC, new MapLocation(127, 127)); break;
                case 2: writeLocationToArray(rc, ARR_FLAG3_SET_LOC, new MapLocation(127, 127)); break;
            }
        } else {
            System.out.println("Tried marking flag as captured, but failed!!!!!!");
        }
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
        if (rc.getLocation() != null) {
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(2, enemyTeam);
            for (FlagInfo enemyFlag : enemyFlags) {
                if (rc.canPickupFlag(enemyFlag.getLocation())) {
                    rc.pickupFlag(enemyFlag.getLocation());
                    return enemyFlag;
                }
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

    public static MapLocation locationFromArray(RobotController rc, int index) throws GameActionException {
        int arrayValue = rc.readSharedArray(index) - 1;
        if (arrayValue == -1) {
            return null;
        }

        return new MapLocation(arrayValue / 128, arrayValue % 128);
    }

    public static void writeLocationToArray(RobotController rc, int index, MapLocation location) throws GameActionException {
        if (location == null) {
            rc.writeSharedArray(index, 0);
            return;
        }

        rc.writeSharedArray(index, location.x * 128 + location.y + 1);
    }
}
