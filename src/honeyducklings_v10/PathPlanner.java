package honeyducklings_v10;

import battlecode.common.*;

public class PathPlanner {

    private static final int[] CWSearch = {
            0, 1, 2, 3
    };

    private static final int[] CCWSearch = {
            0, -1, -2, -3, -4, -5, -6, -7, -8
    };

    private static MapLocation lastFrom;
    private static MapLocation lastTarget;
    private static int lastDirectionIndex = 0;
    private static boolean onObstacle = false;
    private static int roundsOnObstacle = 0;

    public static Direction planRoute(RobotController rc, MapLocation fromLocation, MapLocation goalLocation) throws GameActionException {

        // Are we still on the same path?
        if (lastTarget == null || !lastTarget.equals(goalLocation)) {
            lastFrom = fromLocation;
            lastTarget = goalLocation;
            onObstacle = false;
            roundsOnObstacle = 0;
        }

//        rc.setIndicatorLine(lastFrom, lastTarget, 200, 200, 200);

        // Stop sticking to the obstacle when we get on the line
        if (onObstacle && onLine(fromLocation, lastFrom, lastTarget)) {
            onObstacle = false;
            roundsOnObstacle = 0;
        }

        // If not currently following an obstacle, move in the best direction
        if (!onObstacle) {
            lastDirectionIndex = fromLocation.directionTo(goalLocation).ordinal();
            Direction testDirection = Direction.values()[lastDirectionIndex];

            if (canMove(rc, testDirection)) {
                return testDirection;
            }

            if (rc.canSenseLocation(fromLocation.add(testDirection))) {
                MapInfo blockedArea = rc.senseMapInfo(fromLocation.add(testDirection));
                if (blockedArea != null && !blockedArea.isWall()) {
                    rc.setIndicatorString("On obstacle for " + roundsOnObstacle + " rounds!");
                    return null;
                }
            }

            // Time to start following the obstacle!
            onObstacle = true;
        }

        rc.setIndicatorString("On obstacle for " + roundsOnObstacle + " rounds!");

        if (roundsOnObstacle >= 15) {
            onObstacle = false;
            roundsOnObstacle = 0;
        }
        roundsOnObstacle++;

        // Can we continue how we were going?
        if (canMove(rc, indexToDirection(lastDirectionIndex))) {
            // See if we can hug closer (looking CW)
            for (int i = 1; i < CWSearch.length; i++) {
                if (!canMove(rc, indexToDirection(lastDirectionIndex + CWSearch[i]))) {
                    // Found a place we can't move, go back one
                    lastDirectionIndex = (lastDirectionIndex + CWSearch[i-1] + 8) % 8;

                    return indexToDirection(lastDirectionIndex);
                }
            }

            // If not, then let's just keep going that way and assume the obstacle "disappeared"
//            onObstacle = false;
//            roundsOnObstacle = 0;
            return indexToDirection(lastDirectionIndex);
        }

        // If in front is not good, we have to look CCW
        for (int i=1; i < CCWSearch.length; i++) {
            if (canMove(rc, indexToDirection(lastDirectionIndex + CCWSearch[i]))) {
                // Found a place we can go!
                lastDirectionIndex = (lastDirectionIndex + CCWSearch[i] + 8) % 8;

                return indexToDirection(lastDirectionIndex);
            }
        }

        return null;
    }

    public static boolean canMove(RobotController rc, Direction direction) {
        return (rc.canMove(direction) || rc.canFill(rc.getLocation().add(direction)));
    }

    public static boolean onLine(MapLocation tp, MapLocation p1, MapLocation p2) {
        return Math.abs(eucDistance(tp, p1) + eucDistance(tp, p2) - eucDistance(p1, p2)) < 0.5;
    }

    public static double eucDistance(MapLocation p1, MapLocation p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static Direction indexToDirection(int index) {
        return Direction.values()[(index + 8) % 8];
    }
}

