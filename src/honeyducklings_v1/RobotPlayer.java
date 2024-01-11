package honeyducklings_v1;

import battlecode.common.*;

import java.util.Random;

import static java.lang.Math.abs;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(2718);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static Team enemyTeam;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
//        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");
        enemyTeam = rc.getTeam().opponent();

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else{
                    if (rc.canPickupFlag(rc.getLocation()) && !rc.senseNearbyFlags(0)[0].getTeam().equals(rc.getTeam())){
                        rc.pickupFlag(rc.getLocation());
                        rc.writeSharedArray(0, flagLocToArrayStorage(rc.getLocation()));
                        rc.writeSharedArray(1, rc.getRoundNum());
                        rc.setIndicatorString("Holding a flag!");
                        System.out.println("Picked up flag!");
                    }

                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation[] flagSpawnLocs = rc.senseBroadcastFlagLocations();
//                    int sumX = 0;
//                    int sumY = 0;
//                    for (MapLocation mapLoc : spawnLocs) {
//                        sumX += mapLoc.x;
//                        sumY += mapLoc.y;
//                    }
                    MapLocation targetEnemyLocation;
//                    MapLocation targetEnemyLocation = new MapLocation((int)(sumX / (double)spawnLocs.length), (int)(sumY / (double)spawnLocs.length));
                    if (flagSpawnLocs.length > 0) {
                        targetEnemyLocation = flagSpawnLocs[0];
                    } else {
                        targetEnemyLocation = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight()/2);
                    }

                    // If we are holding an enemy flag, singularly focus on moving towards
                    // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                    // to make sure setup phase has ended.
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        MapLocation firstLoc = spawnLocs[0];
                        Direction dir = rc.getLocation().directionTo(firstLoc);
                        rc.writeSharedArray(0, flagLocToArrayStorage(rc.getLocation()));
                        rc.writeSharedArray(1, rc.getRoundNum());
                        if (firstLoc.equals(rc.getLocation().add(dir))) {
                            System.out.println("Captured!");
                            rc.writeSharedArray(0, 0);
                        }
                        if (rc.canMove(dir)) rc.move(dir);
                    }

                    int flagCapRoundNum = rc.readSharedArray(1);
                    if (flagCapRoundNum > 0 && abs(flagCapRoundNum - rc.getRoundNum()) > 2) {
                        rc.writeSharedArray(1, 0);
                        rc.writeSharedArray(0, 0);
                    }

                    int groupLocationValue = rc.readSharedArray(0);

                    RobotInfo[] enemyRobots = rc.senseNearbyRobots(2, enemyTeam);

                    if (enemyRobots.length > 0 && rc.canAttack(enemyRobots[0].getLocation())) {
                        rc.attack(enemyRobots[0].getLocation());
                    }

                    // Move and attack randomly if no objective.
                    Direction dir;
                    MapLocation nextLoc;
                    if (rng.nextInt(10) < 8) {
                        dir = directions[rng.nextInt(directions.length)];
                    } else if(groupLocationValue == 0) {
                        dir = rc.getLocation().directionTo(targetEnemyLocation);
                        rc.setIndicatorString("Heading to " + targetEnemyLocation);
                        rc.setIndicatorLine(rc.getLocation(), targetEnemyLocation, 255, 0, 0);
                    } else {
                        dir = rc.getLocation().directionTo(flagArrayStorageToLoc(groupLocationValue));
                        rc.setIndicatorString("Heading to " + flagArrayStorageToLoc(groupLocationValue));
                        rc.setIndicatorLine(rc.getLocation(), flagArrayStorageToLoc(groupLocationValue), 0, 255, 0);
                    }

                    nextLoc = rc.getLocation().add(dir);

                    if (rc.canMove(dir)){
                        rc.move(dir);
                    }
                    else if (rc.canAttack(nextLoc)){
                        rc.attack(nextLoc);
//                        System.out.println("Take that! Damaged an enemy that was in our way!");
                    }

                    // Rarely attempt placing traps behind the robot.
                    MapLocation prevLoc = rc.getLocation().subtract(dir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1)
                        rc.build(TrapType.EXPLOSIVE, prevLoc);
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    public static int flagLocToArrayStorage(MapLocation location) {
        return location.x + location.y * 100;
    }

    public static MapLocation flagArrayStorageToLoc(int value) {
        return new MapLocation(value % 100, value / 100);
    }
}
