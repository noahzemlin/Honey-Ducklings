package honeyducklings;

import battlecode.common.MapLocation;

public class FlagManager {
    private class Flag {
        boolean isCaptured = false;
        boolean pickedUp = false;
        MapLocation location = null;

        void writeArrayValue(int arrayValue) {
            int locationX = arrayValue % 61;
            int locationY = (arrayValue / 61) % 61;
            int pickedUp = arrayValue / (61 * 61) % 2;
            int isCaptured = arrayValue / (61 * 61 * 2) % 2;

            this.location = new MapLocation(locationX, locationY);
            this.pickedUp = pickedUp == 1;
            this.isCaptured = isCaptured == 1;
        }

        int readArrayValue() {
            return location.x + location.y * 61 + (pickedUp ? 1 : 0) * (61 * 61) + (isCaptured ? 1 : 0) * (61 * 61 * 2);
        }
    }

    private static final Flag[] flags = new Flag[3];

    private static void flagFromArray(int arrayValue, int flagId) {
        flags[flagId].writeArrayValue(arrayValue);
    }

    private static int flagToArray(int flagId) {
        return flags[flagId].readArrayValue();
    }
}
