package honeyducklings;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Commander {

    private class FlagStatus {
        public MapLocation location;
        public MapLocation spawnLocation;
        public boolean captured = false;
        public boolean held = false;
    }

    private static FlagStatus[] flags = new FlagStatus[3];
    private static int[] ARR_FLAG_LOC = {RobotPlayer.ARR_FLAG1_LOC, RobotPlayer.ARR_FLAG2_LOC, RobotPlayer.ARR_FLAG3_LOC};
    private static int[] ARR_FLAG_SET_LOC = {RobotPlayer.ARR_FLAG1_SET_LOC, RobotPlayer.ARR_FLAG2_SET_LOC, RobotPlayer.ARR_FLAG3_SET_LOC};

    public static void commandTheLegion(RobotController rc) throws GameActionException {
        // Commander strategy:
        // Try to capture flags in order 3, 2, 1

        readFlagLocationsFromWorld(rc);

        readFlagLocationsFromArray(rc);

        int commandPrimary = 0;
        int commandSecondary = 0;

        for (int i=0; i < flags.length; i++) {
            if (flags[i].location != null) {
                if (flags[i].captured) {
                    RobotPlayer.writeLocationToArray(rc, ARR_FLAG_LOC[i], null);
                } else {
                    RobotPlayer.writeLocationToArray(rc, ARR_FLAG_LOC[i], flags[i].location);
                    commandSecondary = commandPrimary;
                    commandPrimary = i + 1;
                }
            }
        }

        if (commandSecondary == 0) {
            commandSecondary = commandPrimary;
        }

        rc.writeSharedArray(RobotPlayer.ARR_COMMAND, commandPrimary);
        rc.writeSharedArray(RobotPlayer.ARR_COMMAND_SECONDARY, commandSecondary);
    }

    private static void readFlagLocationsFromWorld(RobotController rc) throws GameActionException {
        MapLocation[] broadcastFlagLocations = rc.senseBroadcastFlagLocations();

        // Update all three locations only if we see all three flags (to match indicies)
        if (broadcastFlagLocations.length == 3) {
            for (int i=0; i<3; i++) {
                flags[i].location = flags[i].spawnLocation;
            }
        }
    }

    public static void readFlagLocationsFromArray(RobotController rc) throws GameActionException {
        for (int i=0; i<3; i++) {
            MapLocation flag_location = RobotPlayer.locationFromArray(rc, ARR_FLAG_SET_LOC[i]);

            if (flag_location != null) {
                flags[i].location = flag_location;

                if (flag_location.x == 127) {
                    flags[i].captured = true;
                } else if(flag_location.x == 126) {
                    flags[i].location = flags[i].spawnLocation;
                }
            }
        }
    }
}
