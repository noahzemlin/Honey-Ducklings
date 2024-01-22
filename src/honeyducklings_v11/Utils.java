package honeyducklings_v11;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Utils {
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

    public static MapLocation getNearestRobot(MapLocation location, RobotInfo[] robots) {
        MapLocation nearestRobot = robots[0].location;

        for (int i=1; i<robots.length; i++) {
            if (location.distanceSquaredTo(robots[i].location) < location.distanceSquaredTo(nearestRobot)) {
                nearestRobot = robots[i].location;
            }
        }

        return nearestRobot;
    }
    public static double getAverageRobotDistance(MapLocation location, RobotInfo[] robots) {
        double sumRobotDistance = 0;

        for (int i=1; i<robots.length; i++) {
            sumRobotDistance += Math.sqrt(location.distanceSquaredTo(robots[i].location));
        }

        return sumRobotDistance / (double) robots.length;
    }

    public static double getSumRobotDistance(MapLocation directionPlacement, RobotInfo[] robots) {
        double sumRobotDistance = 0;

        for (int i=1; i<robots.length; i++) {
            sumRobotDistance += Math.sqrt(directionPlacement.distanceSquaredTo(robots[i].location));
        }

        return sumRobotDistance;
    }
}
