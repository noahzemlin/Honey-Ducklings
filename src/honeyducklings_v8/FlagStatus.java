package honeyducklings_v8;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class FlagStatus {

    //  +0: Flag ID
    //  +1: Flag Location
    //  +2: Flag Set Location
    //  +3: Carried
    //  +4: Dropped
    //  +5: Captured

    public final static int FLAG_ID = 0;
    public final static int FLAG_LOC = 1;
    public final static int FLAG_SET_LOC = 2;
    public final static int FLAG_CARRIED = 3;
    public final static int FLAG_DROPPED = 4;
    public final static int FLAG_CAPTURED = 5;

    public int arrayStart;
    public int id = -1;
    public MapLocation location;
    public MapLocation spawnLocation;
    public boolean isCarried = false;
    public boolean isCaptured = false;

    public FlagStatus(int arrayStart) {
        this.arrayStart = arrayStart;
    }

    public void pickup(RobotController rc, FlagInfo flagInfo) throws GameActionException {
        rc.writeSharedArray(arrayStart + FLAG_CARRIED, 1);
        reportLocation(rc, flagInfo);
    }

    public void capture(RobotController rc) throws GameActionException {
        rc.writeSharedArray(arrayStart + FLAG_CAPTURED, 1);
    }

    public void drop(RobotController rc) throws GameActionException {
        rc.writeSharedArray(arrayStart + FLAG_CARRIED, 0);
        rc.writeSharedArray(arrayStart + FLAG_DROPPED, 1);
    }

    public int fetchId(RobotController rc) throws GameActionException {
        if (id == -1) {
            id = rc.readSharedArray(arrayStart + FLAG_ID) - 1;
        }
        return id;
    }

    public void loadFlagStatus(RobotController rc) throws GameActionException {
        id = rc.readSharedArray(arrayStart + FLAG_ID) - 1;
        location = Utils.locationFromArray(rc, arrayStart + FLAG_LOC);
        isCarried = rc.readSharedArray(arrayStart + FLAG_CARRIED) > 0;
        isCaptured = rc.readSharedArray(arrayStart + FLAG_CAPTURED) > 0;
    }

    public void reportLocation(RobotController rc, FlagInfo flagInfo) throws GameActionException {
        Utils.writeLocationToArray(rc, arrayStart + FLAG_SET_LOC, flagInfo.getLocation());
        this.location = flagInfo.getLocation();
    }

    public void applyReports(RobotController rc) throws GameActionException {
        // Apply location report
        MapLocation reportedLocation = Utils.locationFromArray(rc, arrayStart + FLAG_SET_LOC);
        if (reportedLocation != null) {

            // If this is the first time we see this flag, this must be its spawn location!
            if (location == null) {
                spawnLocation = reportedLocation;
            }

            Utils.writeLocationToArray(rc, arrayStart + FLAG_LOC, reportedLocation);
            Utils.writeLocationToArray(rc, arrayStart + FLAG_SET_LOC, null);
            this.location = reportedLocation;
        }

        // Apply dropped report
        if (rc.readSharedArray(arrayStart + FLAG_DROPPED) > 0) {
            rc.writeSharedArray(arrayStart + FLAG_DROPPED,0);
            this.location = spawnLocation;
            Utils.writeLocationToArray(rc, arrayStart + FLAG_LOC, this.location);
        }
    }

    public void setID(RobotController rc, FlagInfo flagInfo) throws GameActionException {
        rc.writeSharedArray(arrayStart + FLAG_ID, flagInfo.getID() + 1);
        this.id = flagInfo.getID();
    }
}
