package honeyducklings_v10;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
}
