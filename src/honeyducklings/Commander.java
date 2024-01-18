package honeyducklings;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Commander {

    private static MapLocation Flag1Location;
    private static MapLocation Flag1Init;
    private static boolean Flag1Captured = false;
    private static boolean knowFlag1Precise = false;
    private static MapLocation Flag2Location;
    private static MapLocation Flag2Init;
    private static boolean Flag2Captured = false;
    private static boolean knowFlag2Precise = false;
    private static MapLocation Flag3Location;
    private static MapLocation Flag3Init;
    private static boolean Flag3Captured = false;
    private static boolean knowFlag3Precise = false;

    public static void commandTheLegion(RobotController rc) throws GameActionException {
        // Commander strategy:
        // Try to capture flags in order 3, 2, 1

        readFlagLocationsFromWorld(rc);

        readFlagLocationsFromArray(rc);

        int commandPrimary = 0;
        int commandSecondary = 0;

        if (Flag1Location != null) {
            if (Flag1Captured) {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG1_LOC, null);
            } else {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG1_LOC, Flag1Location);
                commandSecondary = commandPrimary;
                commandPrimary = 1;
            }
        }

        if (Flag2Location != null) {
            if (Flag2Captured) {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG2_LOC, null);
            } else {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG2_LOC, Flag2Location);
                commandSecondary = commandPrimary;
                commandPrimary = 2;
            }
        }

        if (Flag3Location != null) {
            if (Flag3Captured) {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG3_LOC, null);
            } else {
                RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG3_LOC, Flag3Location);
                commandSecondary = commandPrimary;
                commandPrimary = 3;
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
            Flag1Init = broadcastFlagLocations[0];
            Flag2Init = broadcastFlagLocations[1];
            Flag3Init = broadcastFlagLocations[2];
        }

        if (!knowFlag1Precise) {
            Flag1Location = Flag1Init;
        }
        if (!knowFlag2Precise) {
            Flag2Location = Flag2Init;
        }
        if (!knowFlag3Precise) {
            Flag3Location = Flag3Init;
        }
    }

    public static void readFlagLocationsFromArray(RobotController rc) throws GameActionException {
        MapLocation flag1_location = RobotPlayer.locationFromArray(rc, RobotPlayer.ARR_FLAG1_SET_LOC);
        if (flag1_location != null) {
            Flag1Location = flag1_location;

            // Captured
            if (flag1_location.x == 127 && flag1_location.y == 127) {
                Flag1Captured = true;
            } else if (flag1_location.x == 126 && flag1_location.y == 126) {
                Flag1Location = Flag1Init;
            } else if (!knowFlag1Precise) {
                Flag1Init = Flag1Location;
                knowFlag1Precise = true;
            }

            RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG1_SET_LOC, null);
        }

        MapLocation flag2_location = RobotPlayer.locationFromArray(rc, RobotPlayer.ARR_FLAG2_SET_LOC);
        if (flag2_location != null) {
            Flag2Location = flag2_location;

            // Captured
            if (flag2_location.x == 127 && flag2_location.y == 127) {
                Flag2Captured = true;
            } else if (flag2_location.x == 126 && flag2_location.y == 126) {
                Flag2Location = Flag2Init;
            } else if (!knowFlag2Precise) {
                Flag2Init = Flag2Location;
                knowFlag2Precise = true;
            }

            RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG2_SET_LOC, null);
        }

        MapLocation flag3_location = RobotPlayer.locationFromArray(rc, RobotPlayer.ARR_FLAG3_SET_LOC);
        if (flag3_location != null) {
            Flag3Location = flag3_location;

            // Captured
            if (flag3_location.x == 127 && flag3_location.y == 127) {
                Flag3Captured = true;
            } else if (flag3_location.x == 126 && flag3_location.y == 126) {
                Flag3Location = Flag3Init;
            } else if (!knowFlag3Precise) {
                Flag3Init = Flag3Location;
                knowFlag3Precise = true;
            }

            RobotPlayer.writeLocationToArray(rc, RobotPlayer.ARR_FLAG3_SET_LOC, null);
        }
    }
}
