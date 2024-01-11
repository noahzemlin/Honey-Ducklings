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
    private static int mapWidth;
    private static int mapHeight;

    public static void setup(RobotController rc) {

        random = new Random(2718);

        enemyTeam = rc.getTeam().opponent();

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        map = new PathPlanner.MapType[mapWidth][mapHeight];
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
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
                (averageSpawnLocationX / (mapWidth / 2)) * (mapWidth - 1),
                (averageSpawnLocationY / (mapHeight / 2)) * (mapHeight - 1)
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

        // Run early game separate
        if (rc.getRoundNum() < 200) {
            runEarlyGame(rc);
            return;
        }

        // For now, just move to center to test path planning
        // TODO: Replace with something not this
        if (rc.isMovementReady()) {
            // Check our surroundings and add to our map estimate
            analyzeSurroundings(rc);

            MapLocation ourLocation = rc.getLocation();
            MapLocation targetLocation = new MapLocation(10, 15);
            Direction toCenter = PathPlanner.planRoute(map, ourLocation, targetLocation);

            if (toCenter == null) {
                rc.setIndicatorString("Failed to path plan!");
                toCenter = ourLocation.directionTo(targetLocation);
            }

            rc.setIndicatorLine(ourLocation, targetLocation, 255, 0, 0);
            rc.setIndicatorString("Moving " + toCenter + " to center.");

            if (toCenter == Direction.CENTER) {
                return;
            }

            if (rc.canMove(toCenter)) {
                rc.move(toCenter);
            } else {
                System.out.println("Tried to move " + toCenter);
            }
        }
    }

    public static void runEarlyGame(RobotController rc) throws GameActionException {
        // See if we can grab an ally flag and start moving it far away
        if (rc.hasFlag()) {
            if (!rc.isMovementReady()) {
                return;
            }

            if (rc.getRoundNum() > 190) {
                for (Direction direction : Direction.allDirections()) {
                    MapLocation desiredDropLocation = rc.getLocation().add(direction);
                    if (rc.canDropFlag(desiredDropLocation)) {
                        rc.dropFlag(desiredDropLocation);
                    }
                }
            }

            Direction toSafeCorner = PathPlanner.planRoute(map, rc.getLocation(), safeCorner);
            if (toSafeCorner == null) {
                rc.setIndicatorString("Failed to path plan!");
                toSafeCorner = rc.getLocation().directionTo(safeCorner);
            }

            if (rc.canMove(toSafeCorner)) {
                rc.move(toSafeCorner);
                rc.setIndicatorString("Moving flag to " + safeCorner);
            }

            return;
        } else if (rc.getRoundNum() < 10) {
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

    public static PathPlanner.MapType mapInfoToMapType(RobotController rc, MapInfo mapInfo) {
        if (mapInfo.isWall()) {
            return PathPlanner.MapType.WALL;
        }

        if (mapInfo.isWater()) {
            return PathPlanner.MapType.WATER;
        }

        if (!mapInfo.isPassable()) {
            return PathPlanner.MapType.NOT_PASSABLE;
        }

        if (rc.canSenseRobotAtLocation(mapInfo.getMapLocation())) {
            return PathPlanner.MapType.NOT_PASSABLE;
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
