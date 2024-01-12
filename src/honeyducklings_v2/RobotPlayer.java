package honeyducklings_v2;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public strictfp class RobotPlayer {

    private static PathPlanner.MapType[][] map;
    private static MapLocation[] allySpawnLocations;
    private static Team enemyTeam;
    private static MapLocation safeCorner;
    private static MapLocation huddle;
    private static Random random;
    private static int mapWidth;
    private static int mapHeight;
    private static boolean willHeal;
    private static boolean isCommander = false;

    public static void setup(RobotController rc) throws GameActionException {

        if (rc.readSharedArray(0) == 0) {
            rc.writeSharedArray(0, 1);
            System.out.println("I am da captain now >:)");
            isCommander = true;
        }

        random = new Random(2718);

        enemyTeam = rc.getTeam().opponent();

        willHeal = rc.getID() % 3 == 0;

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        map = new PathPlanner.MapType[mapWidth][mapHeight];
//        for (int x = 0; x < mapWidth; x++) {
//            for (int y = 0; y < mapHeight; y++) {
//                map[x][y] = PathPlanner.MapType.EMPTY;
//            }
//        }

        allySpawnLocations = rc.getAllySpawnLocations();

        int sumSpawnLocationX = 0;
        int sumSpawnLocationY = 0;

        for (MapLocation spawnLocation : allySpawnLocations) {
            sumSpawnLocationX += spawnLocation.x;
            sumSpawnLocationY += spawnLocation.y;
        }

        int averageSpawnLocationX = (int) (sumSpawnLocationX / (double) (allySpawnLocations.length));
        int averageSpawnLocationY = (int) (sumSpawnLocationY / (double) (allySpawnLocations.length));

        safeCorner = new MapLocation(
                (averageSpawnLocationX / (mapWidth / 2)) * (mapWidth - 1),
                (averageSpawnLocationY / (mapHeight / 2)) * (mapHeight - 1)
        );

        huddle = new MapLocation(averageSpawnLocationX, averageSpawnLocationY);
    }

    public static void runStep(RobotController rc) throws GameActionException {
        if (!rc.isSpawned()) {
            for (MapLocation spawnLocation : allySpawnLocations) {
                if (rc.canSpawn(spawnLocation)) {
                    rc.spawn(spawnLocation);

                    // Wipe map for performance reasons i guess
                    for (int x = 0; x < mapWidth; x++) {
                        for (int y = 0; y < mapHeight; y++) {
                            map[x][y] = PathPlanner.MapType.EMPTY;
                        }
                    }
                }
            }

            if (!rc.isSpawned()) {
                // uh oh, we're DONEZO
                return;
            }
        }

//        // Run early game separate
//        if (rc.getRoundNum() < 200) {
//            runEarlyGame(rc);
//            return;
//        }

        // Attack if we see someone
        RobotInfo[] enemies = rc.senseNearbyRobots(4, enemyTeam);
        boolean triedToAttack = false;
        if (enemies.length > 0) {
            triedToAttack = true;
            RobotInfo bestEnemy = enemies[0];
            for (int i = 1; i < enemies.length; i++) {
                if (rc.canAttack(enemies[i].getLocation()) && enemies[i].health < bestEnemy.health) {
                    bestEnemy = enemies[i];
                }
            }

            if (rc.canAttack(bestEnemy.getLocation())) {
                rc.attack(bestEnemy.getLocation());
            }
        }

        // Heal if we can
        boolean triedToHeal = false;
        if (willHeal) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo ally : allies) {
                if (ally.health < 500 && rc.canHeal(ally.getLocation())) {
                    triedToHeal = true;
                    rc.heal(ally.getLocation());
                    break;
                }
            }
        }

        // Pick up flag if we can
        FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, enemyTeam);
        for (FlagInfo enemyFlag: enemyFlags) {
            if (rc.canPickupFlag(enemyFlag.getLocation())) {
                rc.pickupFlag(enemyFlag.getLocation());
                break;
            }
        }

        // For now, just move to enemy flag
        if (!triedToAttack && !triedToHeal && rc.isMovementReady()) {
            // Check our surroundings and add to our map estimate
            analyzeSurroundings(rc);

            MapLocation ourLocation = rc.getLocation();
            MapLocation targetLocation = ourLocation;

            // Huddle up by default
            targetLocation = huddle;

            if (isCommander) {
                MapLocation[] flagLocations = rc.senseBroadcastFlagLocations();
                if (flagLocations.length > 0) {
                    targetLocation = flagLocations[0];
                }
            }

            int command = rc.readSharedArray(0) - 1;
            if (!isCommander && command > 0) {
                targetLocation = new MapLocation(command % 100, command / 100);
            }

            FlagInfo[] visibleFlags = rc.senseNearbyFlags(-1, enemyTeam);
            if (visibleFlags.length > 0 && !visibleFlags[0].isPickedUp()) {
                targetLocation = visibleFlags[0].getLocation();
            }

            if (rc.hasFlag()) {
                targetLocation = allySpawnLocations[0];
            }

//            if (!isCommander && ourLocation.isWithinDistanceSquared(targetLocation, 64)) {
//                targetLocation = ourLocation.add(Direction.allDirections()[random.nextInt(9)]);
//            }

            Direction toTarget = PathPlanner.planRoute(map, ourLocation, targetLocation);

            if (toTarget == null) {
                rc.setIndicatorString("Failed to path plan!");
                toTarget = ourLocation.directionTo(targetLocation);
            }

            if (toTarget == Direction.CENTER) {
                rc.setIndicatorString("No path.");
                return;
            }

//            rc.setIndicatorLine(ourLocation, targetLocation, 255, 0, 0);
            rc.setIndicatorString("Moving " + toTarget + " to target.");

            if (rc.canMove(toTarget)) {
                rc.move(toTarget);

                if (isCommander) {
                    rc.writeSharedArray(0, targetLocation.x + targetLocation.y * 100 + 1);
                }

                if (rc.canBuild(TrapType.STUN, ourLocation)) {
                    rc.build(TrapType.STUN, ourLocation);
                }
            } else {
                if (rc.canFill(ourLocation.add(toTarget))) {
                    rc.fill(ourLocation.add(toTarget));
                }

                if (isCommander) {
                    rc.writeSharedArray(0, targetLocation.x + targetLocation.y * 100 + 1);
                }
            }
        }
    }

    public static void runEarlyGame(RobotController rc) throws GameActionException {
        // See if we can grab an ally flag and start moving it far away
        // NOPE, this is illegal unless we want to figure out how to keep the flags
        // 6 tiles apart lol

        Direction randomDirection = Direction.allDirections()[random.nextInt(9)];
        if (rc.canMove(randomDirection)) {
            rc.move(randomDirection);
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

    public static PathPlanner.MapType mapInfoToMapType(RobotController rc, MapInfo mapInfo) {
//        if (!mapInfo.isWater()) {
//            return PathPlanner.MapType.WATER;
//        }

        if (!mapInfo.isPassable()) {
            return PathPlanner.MapType.NOT_PASSABLE;
        }

        try {
            if (rc.isLocationOccupied(mapInfo.getMapLocation())) {
                return PathPlanner.MapType.NOT_PASSABLE;
            }
        } catch (GameActionException e) {
            return PathPlanner.MapType.EMPTY;
        }

        return PathPlanner.MapType.EMPTY;
    }

    public static void analyzeSurroundings(RobotController rc) throws GameActionException {
        MapInfo[] nearbyMap = rc.senseNearbyMapInfos(9);

        for (MapInfo tile : nearbyMap) {
            map[tile.getMapLocation().x][tile.getMapLocation().y] = mapInfoToMapType(rc, tile);
        }
    }
}
