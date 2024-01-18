package honeyducklings_v4;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.min;

public strictfp class RobotPlayer {

    private static PathPlanner.MapType[][] map;
    private static MapLocation[] allySpawnLocations;
    private static Team ourTeam;
    private static Team enemyTeam;
    private static MapLocation safeCorner;
    private static MapLocation huddle;
    private static Random random;
    private static int mapWidth;
    private static int mapHeight;
    private static boolean willHeal;
    private static boolean isCommander = false;
    private static int attacks = 0;

    private static boolean triedToHeal = false;
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

    public static MapLocation knownEnemyFlagLocation = null;
    public static int flagValid = 0;
    public static boolean resumePlanningLater = false;


    public static void setup(RobotController rc) throws GameActionException {

        if (rc.readSharedArray(0) == 0) {
            rc.writeSharedArray(0, 1);
            System.out.println("I am da captain now >:)");
            isCommander = true;
        }

        random = new Random(2718);

        ourTeam = rc.getTeam();
        enemyTeam = ourTeam.opponent();

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
                if (random.nextInt(5) <= 3) {
                    continue;
                }
                if (rc.canSpawn(spawnLocation)) {
                    rc.spawn(spawnLocation);

                    // Wipe map for performance reasons i guess
                    for (int x = 0; x < mapWidth; x++) {
                        for (int y = 0; y < mapHeight; y++) {
                            if (map[x][y] != PathPlanner.MapType.WALL)
                                map[x][y] = PathPlanner.MapType.EMPTY;
                        }
                    }

                    attacks = 0;
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

        // Start commanding!
        if (isCommander) {
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
//                if (knownEnemyFlagLocation != null) {
//                    commandLocation = knownEnemyFlagLocation;
//                    flagValid--;
//                    if (flagValid <= 0) {
//                        knownEnemyFlagLocation = null;
//                    }
//                } else {
                MapLocation[] flagLocations = rc.senseBroadcastFlagLocations();
                int round = rc.getRoundNum();
                if (round < 200) {
                    round = 200;
                }
                int roundBasedFlagTarget = 2 - (round - 200) / 601;

                if (flagLocations.length > 0) {
                    commandLocation = flagLocations[min(roundBasedFlagTarget, flagLocations.length - 1)];
                }
//                }
            }

            rc.writeSharedArray(0, commandLocation.x + commandLocation.y * 100 + 1);
        }

        // For now, just move to enemy flag
        MapLocation ourLocation = rc.getLocation();
        if ((rc.hasFlag() || !triedToAttack) && rc.isMovementReady()) {
            // Check our surroundings and add to our map estimate
            analyzeSurroundings(rc);

            MapLocation targetLocation = ourLocation;

            // Huddle up by default
            targetLocation = huddle;

            int command = rc.readSharedArray(0) - 1;
            if (command > 0) {
                targetLocation = new MapLocation(command % 100, command / 100);
            }

            FlagInfo[] visibleFlags = rc.senseNearbyFlags(-1);
            for(FlagInfo flag : visibleFlags) {
                if (flag.getTeam().equals(enemyTeam) && !flag.isPickedUp()) {
                    targetLocation = visibleFlags[0].getLocation();

//                    if (isCommander) {
//                        knownEnemyFlagLocation = targetLocation;
//                        flagValid = 200;
//                    }
                }

                if (flag.getTeam().equals(ourTeam) && flag.isPickedUp()) {
                    rc.writeSharedArray(2, flag.getLocation().x + flag.getLocation().y + 100 + 1);
                }
            }

//            if (!isCommander && ourLocation.isWithinDistanceSquared(targetLocation, 64)) {
//                targetLocation = ourLocation.add(Direction.allDirections()[random.nextInt(9)]);
//            }

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

            rc.setIndicatorLine(ourLocation, targetLocation, 255, isCommander ? 255 : 0, 0);
            Direction toTarget = PathPlanner.planRoute(resumePlanningLater, map, ourLocation, targetLocation);

            if (toTarget == null) {
                resumePlanningLater = true;
            } else {
                resumePlanningLater = false;
            }

            if (toTarget == null) {
                rc.setIndicatorString("Failed to path plan!");
                toTarget = ourLocation.directionTo(targetLocation);
            }

            if (toTarget == Direction.CENTER) {
//                rc.setIndicatorString("No path.");
//                return;
                toTarget = reasonableDirections[random.nextInt(reasonableDirections.length)];
            }

            rc.setIndicatorString("Moving " + toTarget + " to target.");

            if (rc.canMove(toTarget) && rc.isSpawned()) {
                rc.move(toTarget);

                if (attacks >= 6 && rc.canBuild(TrapType.EXPLOSIVE, ourLocation)) {
                    rc.build(TrapType.EXPLOSIVE, ourLocation);
                    attacks = 0;
                }

                ourLocation = ourLocation.add(toTarget);
            } else {
                if (rc.canFill(ourLocation.add(toTarget))) {
                    rc.fill(ourLocation.add(toTarget));
                }
            }
        }

        // Now that we've moved, try to take actions
        triedToAttack = false;
        triedToHeal = false;

        if (!rc.isSpawned()) {
            return;
        }

        // Attack if we see someone
        RobotInfo[] enemies = rc.senseNearbyRobots(ourLocation, 4, enemyTeam);
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
            }
        }

        // Heal if we can
        RobotInfo[] allies = rc.senseNearbyRobots(ourLocation, 4, ourTeam);
        for (RobotInfo ally : allies) {
            if (ally.health < 920 && rc.canHeal(ally.getLocation())) {
                triedToHeal = true;
                rc.heal(ally.getLocation());
                break;
            }
        }

        // Pick up flag if we can
        if (rc.getLocation() != null) {
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, enemyTeam);
            for (FlagInfo enemyFlag : enemyFlags) {
                if (rc.canPickupFlag(enemyFlag.getLocation())) {
                    rc.pickupFlag(enemyFlag.getLocation());
                    break;
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
        if (mapInfo.isWall()) {
            return PathPlanner.MapType.WALL;
        }

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
