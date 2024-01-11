package honeyducklings;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

public strictfp class RobotPlayer {

    private static PathPlanner.MapType[][] map;
    private static MapLocation[] allySpawnLocations;
    private static Team enemyTeam;
    private static MapLocation safeCorner;
    private static Random random;

    public static void setup(RobotController rc) {

        random = new Random(2718);

        enemyTeam = rc.getTeam().opponent();

        map = new PathPlanner.MapType[rc.getMapWidth()][rc.getMapHeight()];
        for (int x = 0; x < rc.getMapWidth(); x++) {
            for (int y = 0; y < rc.getMapHeight(); y++) {
                map[x][y] = PathPlanner.MapType.EMPTY;
            }
        }

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
                (averageSpawnLocationX / (rc.getMapWidth() / 2)) * (rc.getMapWidth() - 1),
                (averageSpawnLocationY / (rc.getMapHeight() / 2)) * (rc.getMapHeight() - 1)
        );
    }

    public static void runStep(RobotController rc) throws GameActionException {
        if (!rc.isSpawned()) {
            for (MapLocation spawnLocation : allySpawnLocations) {
                if (rc.canSpawn(spawnLocation)) {
                    rc.spawn(spawnLocation);
                }
            }

            if (!rc.isSpawned()) {
                // uh oh, we're DONEZO
                return;
            }
        }

        // Check our surroundings and add to our map estimate
        analyzeSurroundings(rc);

        // Run early game separate
        if (rc.getRoundNum() < 200) {
            runEarlyGame(rc);
            return;
        }

        // TODO: What do now??
    }

    public static void runEarlyGame(RobotController rc) throws GameActionException {
        // See if we can grab an ally flag and start moving it far away
        if (rc.hasFlag()) {
            Direction toSafeCorner = PathPlanner.planRoute(map, rc.getLocation(), safeCorner);
            if (rc.canMove(toSafeCorner)) {
                rc.move(toSafeCorner);
            }
            rc.setIndicatorString("Moving flag to " + safeCorner);
            rc.setIndicatorLine(rc.getLocation(), safeCorner, 0, 255, 0);
            return;
        } else {
            FlagInfo[] nearbyFlags = rc.senseNearbyFlags(1, rc.getTeam());
            for (FlagInfo flag : nearbyFlags) {
                if (rc.canPickupFlag(flag.getLocation())) {
                    rc.pickupFlag(flag.getLocation());
                    return;
                }
            }
        }

        Direction randomDirection = Direction.allDirections()[random.nextInt(9)];
        if (rc.canMove(randomDirection)) {
            rc.move(randomDirection);
        }
    }

    public static void run(RobotController rc) {
        setup(rc);

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

    public static PathPlanner.MapType mapInfoToMapType(MapInfo mapInfo) {
        if (mapInfo.isWall()) {
            return PathPlanner.MapType.WALL;
        }

        if (mapInfo.isWater()) {
            return PathPlanner.MapType.WATER;
        }

        if (!mapInfo.isPassable()) {
            return PathPlanner.MapType.NOT_PASSABLE;
        }

        return PathPlanner.MapType.EMPTY;
    }

    public static void analyzeSurroundings(RobotController rc) throws GameActionException {
        MapInfo[] nearbyMap = rc.senseNearbyMapInfos(-1);

        for (MapInfo tile : nearbyMap) {
            map[tile.getMapLocation().x][tile.getMapLocation().y] = mapInfoToMapType(tile);
        }
    }
}
