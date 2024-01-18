package honeyducklings;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class PathPlanner {

    private static final int[] nextIndicies = {
            1, 2, 3, -3, -2, -1
    };

    private static MapLocation lastFrom;
    private static MapLocation lastTarget;
    private static int lastDirectionIndex = 0;
    private static boolean onObstacle = false;

    public static Direction planRoute(RobotController rc, MapLocation fromLocation, MapLocation goalLocation) {

        // Are we still on the same path?
        if (lastTarget == null || !lastTarget.equals(goalLocation)) {
            lastFrom = fromLocation;
            lastTarget = goalLocation;
            onObstacle = false;
            lastDirectionIndex = fromLocation.directionTo(goalLocation).ordinal();
        }

        // If not currently following an obstacle, move in the best direction
        if (!onObstacle) {
            Direction testDirection = Direction.values()[lastDirectionIndex];

            if (canMove(rc, testDirection)) {
                return testDirection;
            }

            // Time to start following the obstacle!
            onObstacle = true;
        }

        // First, find the indicies that separate can and cannot
        int testIndex = lastDirectionIndex;
        int steps = 0;

        while (true) {
            Direction testDirection = Direction.values()[(testIndex + 8) % 8];

            // Searching CW
            if (steps >= 0) {
                // If we can still move, keep going
                if (canMove(rc, testDirection)) {
                    testIndex++;
                    steps++;

                    // Don't go behind, start looking CCW
                    if (steps > 3) {
                        testIndex = lastDirectionIndex - 1;
                        steps = -1;
                    }

                    continue;
                }

                // If we can't move AND it's not forward, we found the location!
                if (steps >= 1) {
                    testIndex--;
                    break;
                }

                // If we can't move, and it IS forward, we need to look CCW
                steps = -1;
            }

            // Searching CCW
            if (steps < 0) {
                if (canMove(rc, testDirection)) {
                    testIndex--;
                    steps--;

                    // No solution!
                    if (steps < -3) {
                        return null;
                    }
                } else {
                    // Can't move, found our target!
                    testIndex++;
                    break;
                }
            }
        }

        Direction finalDirection = Direction.values()[(testIndex + 8) % 8];
        if (onLine(rc.getLocation().add(finalDirection), lastFrom, lastTarget)) {
            onObstacle = false;
        }

        return finalDirection;
    }

    public static boolean canMove(RobotController rc, Direction direction) {
        return (rc.canMove(direction) || rc.canFill(rc.getLocation().add(direction)));
    }

    public static boolean onLine(MapLocation tp, MapLocation p1, MapLocation p2) {
        return Math.abs(eucDistance(tp, p1) + eucDistance(tp, p2) - eucDistance(p1, p2)) < 1.0;
    }

    public static double eucDistance(MapLocation p1, MapLocation p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}

