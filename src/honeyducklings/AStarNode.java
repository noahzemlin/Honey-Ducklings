package honeyducklings;

import battlecode.common.MapLocation;

import java.util.Comparator;

import static java.lang.Math.sqrt;

public class AStarNode implements Comparable<AStarNode> {
    public int x;
    public int y;

    public double g = 0.0;
    public double f;

    public AStarNode cameFrom;


    public AStarNode(int x, int y, MapLocation to) {
        this.x = x;
        this.y = y;
        this.g = h(to);
    }

    public double h(MapLocation to) {
        return sqrt((x - to.x)^2 + (y - to.y)^2);
    }

    @Override
    public boolean equals(Object obj) {
        AStarNode comparable = (AStarNode)obj;
        return (this.x == comparable.x) && (this.y == comparable.y);
    }

    @Override
    public int compareTo(AStarNode o) {
        return (int) (this.f - o.f);
    }
}
