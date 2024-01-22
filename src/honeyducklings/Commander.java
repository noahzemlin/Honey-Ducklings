package honeyducklings;

import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Commander {

    private static FlagStatus[] flags = new FlagStatus[3];
    private static int upgradeStatus = 0;

    public static void commandTheLegion(RobotController rc) throws GameActionException {

        // Purchase an upgrade if we have an upgrade point
        // In order Attack -> Healing -> Capturing
        switch (upgradeStatus) {
            case 0:
                if (rc.canBuyGlobal(GlobalUpgrade.ATTACK) ) {
                    rc.buyGlobal(GlobalUpgrade.ATTACK);
                    upgradeStatus++;
                }
                break;
            case 1:
                if (rc.canBuyGlobal(GlobalUpgrade.HEALING) ) {
                    rc.buyGlobal(GlobalUpgrade.HEALING);
                    upgradeStatus++;
                }
                break;
            case 2:
                if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING) ) {
                    rc.buyGlobal(GlobalUpgrade.CAPTURING);
                    upgradeStatus++;
                }
                break;
        }

        // Initialize flags
        if (flags[0] == null) {
            flags[0] = new FlagStatus(3);
            flags[1] = new FlagStatus(9);
            flags[2] = new FlagStatus(15);
        }

        // Decide commanded (macro) locations
        MapLocation commandedLocation = null;
        MapLocation commandedLocationSecondary = null;

        // Update flag info and set command to first flag
        for (FlagStatus flagStatus : flags) {
            if (flagStatus.fetchId(rc) != -1 && !flagStatus.isCaptured) {
                flagStatus.loadFlagStatus(rc);
                flagStatus.applyReports(rc);

                if (commandedLocation == null) {
                    commandedLocation = flagStatus.location;
                } else {
                    commandedLocationSecondary = flagStatus.location;
                }
            }
        }

        // If we don't have any known good flags, go towards our sense location
        if (commandedLocation == null) {
            MapLocation[] sensedFlagLocations = rc.senseBroadcastFlagLocations();

            if (sensedFlagLocations.length > 0) {
                int sensedFlagIndex = Math.min(sensedFlagLocations.length - 1, rc.getRoundNum() / 700);
                commandedLocation = sensedFlagLocations[sensedFlagIndex];
            }
        }

        Utils.writeLocationToArray(rc, RobotPlayer.ARR_COMMAND, commandedLocation);
        Utils.writeLocationToArray(rc, RobotPlayer.ARR_COMMAND_SECONDARY, commandedLocationSecondary);
    }
}
