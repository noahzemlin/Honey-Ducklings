package honeyducklings;

import battlecode.common.*;

import java.util.Random;

import static java.lang.Math.min;

public strictfp class RobotPlayer {

    private static MapLocation[] allySpawnLocations;
    private static Team ourTeam;
    private static Team enemyTeam;
    private static MapLocation huddle;
    private static Random random;
    private static int mapWidth;
    private static int mapHeight;
    private static boolean isCommander = false;
    private static int attacks = 0;

    private static boolean triedToAttack = false;
    private static int retreatingTicks = 0;
    private static int commanderSaveOurFlagLatch = 0;
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

        if (rc.readSharedArray(0) == 0) {
            rc.writeSharedArray(0, 1);
            System.out.println("I am da captain now >:)");
            isCommander = true;
        }

        random = new Random(2718);

        ourTeam = rc.getTeam();
        enemyTeam = ourTeam.opponent();

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

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
            commandTheLegion(rc);
        }

        // Pick up flag if we can
        attemptToPickupFlag(rc);

        // Attack if we see someone
        RobotInfo attackedRobot = attemptToAttack(rc);

        if (attackedRobot != null && rc.getLocation().distanceSquaredTo(attackedRobot.location) >= 3) {
            // Try to kite
            Direction kiteDirection = attackedRobot.location.directionTo(rc.getLocation());
            if (rc.canMove(kiteDirection)) {
                rc.move(kiteDirection);
            }
        }

        // Move if we can
        attemptToMove(rc, findTargetLocation(rc));

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

        rc.setIndicatorString("Moving " + toTarget + " to target.");

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

        int command = rc.readSharedArray(0) - 1;
        if (command > 0) {
            targetLocation = new MapLocation(command % 100, command / 100);
        }

        FlagInfo[] visibleFlags = rc.senseNearbyFlags(-1);
        for(FlagInfo flag : visibleFlags) {
            if (flag.getTeam().equals(enemyTeam) && !flag.isPickedUp()) {
                targetLocation = visibleFlags[0].getLocation();
            }

            if (flag.getTeam().equals(ourTeam) && flag.isPickedUp()) {
                rc.writeSharedArray(2, flag.getLocation().x + flag.getLocation().y + 100 + 1);
            }
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
            targetLocation = allySpawnLocations[rc.getID() % allySpawnLocations.length];
        }

        rc.setIndicatorLine(rc.getLocation(), targetLocation, 255, isCommander ? 255 : 0, 0);

        return targetLocation;
    }

    public static void commandTheLegion(RobotController rc) throws GameActionException {
        MapLocation commandLocation = huddle;

        int ourFlagStatus = rc.readSharedArray(2) - 1;

        if (commanderSaveOurFlagLatch == 0 && ourFlagStatus >= 0) {
            // They have our flag!!!111!!
            commandLocation = new MapLocation(ourFlagStatus % 100, ourFlagStatus / 100);
            commanderSaveOurFlagLatch = 40;
        } else if (commanderSaveOurFlagLatch > 0) {
            commanderSaveOurFlagLatch--;
            if (commanderSaveOurFlagLatch <= 0) {
                rc.writeSharedArray(2, 0);
            } else {
                commandLocation = new MapLocation(ourFlagStatus % 100, ourFlagStatus / 100);
            }
        } else {
            MapLocation[] flagLocations = rc.senseBroadcastFlagLocations();
            int round = rc.getRoundNum();
            if (round < 200) {
                round = 200;
            }
            int roundBasedFlagTarget = 2 - (round - 200) / 601;

            if (flagLocations.length > 0) {
                commandLocation = flagLocations[min(roundBasedFlagTarget, flagLocations.length - 1)];
            }
        }

        rc.writeSharedArray(0, commandLocation.x + commandLocation.y * 100 + 1);
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
            triedToAttack = true;
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
