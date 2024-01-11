package honeyducklings;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.ArrayList;
import java.util.PriorityQueue;

import static java.lang.Math.sqrt;

public class PathPlanner {
    public enum MapType {
        EMPTY,
        WALL,
        WATER,
        NOT_PASSABLE
    }

    public static Direction[] neighborDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static Direction getFirstStep(MapLocation from, AStarNode lastNode) {
        AStarNode nextNode = lastNode.cameFrom;
        if (nextNode.x == from.x && nextNode.y == from.y) {
            return from.directionTo(new MapLocation(lastNode.x, lastNode.y));
        } else {
            return getFirstStep(from, nextNode);
        }
    }

    public static Direction planRoute(MapType[][] map, MapLocation from, MapLocation to) {
        int width = map.length;
        int height = map[0].length;

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        openSet.add(new AStarNode(from.x, from.y, to));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.x == to.x && current.y == to.y) {
                return getFirstStep(from, current);
            }

            for (Direction neighborDirection : neighborDirections) {

                int newX = current.x + neighborDirection.dx;
                int newY = current.y + neighborDirection.dy;

                if (newX < 0 || newX >= width || newY < 0 || newY >= height) {
                    continue;
                }

                AStarNode neighborNode = new AStarNode(newX, newY, to);
                double approxG = current.g + ((map[newX][newY] == MapType.EMPTY) ? 1 : 1000);

                if (approxG < neighborNode.g) {
                    neighborNode.cameFrom = current;
                    neighborNode.g = approxG;
                    neighborNode.f = approxG + neighborNode.h(to);
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }


        }

        return from.directionTo(to);
    }
}

