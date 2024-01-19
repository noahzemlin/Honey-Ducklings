package honeyducklings_v6;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Commander {

    private static FlagStatus[] flags = new FlagStatus[3];

    public static void commandTheLegion(RobotController rc) throws GameActionException {

        if (flags[0] == null) {
            flags[0] = new FlagStatus(3);
            flags[1] = new FlagStatus(9);
            flags[2] = new FlagStatus(15);
        }

        MapLocation commandedLocation = null;

        // Update flag info and set command to first flag
        for (FlagStatus flagStatus : flags) {
            if (flagStatus.fetchId(rc) != -1 && !flagStatus.isCaptured) {
                flagStatus.loadFlagStatus(rc);
                flagStatus.applyReports(rc);
                commandedLocation = flagStatus.location;
            }
        }

        // If we don't have any known good flags, go towards our sense location
        if (commandedLocation == null) {
            MapLocation[] sensedFlagLocations = rc.senseBroadcastFlagLocations();

            if (sensedFlagLocations.length > 0) {
                commandedLocation = sensedFlagLocations[sensedFlagLocations.length - 1];
            }
        }

        Utils.writeLocationToArray(rc, RobotPlayer.ARR_COMMAND, commandedLocation);
    }
}
