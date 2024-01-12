package honeyducklings_v2;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.PriorityQueue;

import static java.lang.Math.*;

public class PathPlanner {
    public enum MapType {
        EMPTY,
        WALL,
        WATER,
        NOT_PASSABLE
    }

    public static class ANode {
        public boolean open = false;
        public double g = 1000000;
        public double f = 1000000;
        public MapLocation cameFrom = null;
    }

    public static boolean hasPrinted = false;

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

    public static Direction getFirstStep() {
        return Direction.CENTER;
    }

    public static double h(MapLocation node, MapLocation goal) {
//        return (abs(node.x - goal.x) + abs(node.y - goal.y)) / 2.0;
        return sqrt(pow(node.x - goal.x, 2) + pow(node.y - goal.y, 2));
    }

    public static Direction planRoute(MapType[][] map, MapLocation from, MapLocation to) {
        int width = map.length;
        int height = map[0].length;

        if (from.equals(to) || to.x < 0 || to.y < 0 || to.x >= width || to.y >= height) {
            System.out.println("Tried to path no where lol");
            return Direction.CENTER;
        }

        MapLocation lastBacktrackLocation = to;

        int backTrackBeforeCost = Clock.getBytecodeNum();

        while (true) {
            Direction backtrack = lastBacktrackLocation.directionTo(from);
            MapLocation backtrackLocation = lastBacktrackLocation.add(backtrack);

            if (backtrackLocation.equals(from) || map[backtrackLocation.x][backtrackLocation.y] != MapType.EMPTY) {
                break;
            } else {
                lastBacktrackLocation = backtrackLocation;
            }
        }

        int backTrackCost = Clock.getBytecodeNum() - backTrackBeforeCost;

        to = lastBacktrackLocation;

        if (map[to.x][to.y] != MapType.EMPTY) {
//            System.out.println("Backtrack BAD");
            return Direction.CENTER;
        }

        if (from.isAdjacentTo(to)) {
            return from.directionTo(to);
        }

        ANode[][] nodeMap = new ANode[width][height];

        for (int i=0; i < width; i++) {
            for (int j=0; j < height; j++) {
                nodeMap[i][j] = new ANode();
            }
        }

        nodeMap[from.x][from.y].open = true;
        nodeMap[from.x][from.y].g = 0;
        nodeMap[from.x][from.y].f = h(from, to);

        int openNodes = 1;
        int exploredNodes = 0;

        while (openNodes > 0) {

            // Dont burn all our bytecodes :(
//            if (Clock.getBytecodesLeft() < 1000) {
//                System.out.println("Out of byte codes :(");
//                return Direction.CENTER;
//            }

            exploredNodes++;

            MapLocation current = null;
            double lowestF = 1000000;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (nodeMap[x][y].open && nodeMap[x][y].f < lowestF) {
                        current = new MapLocation(x, y);
                        lowestF = nodeMap[x][y].f;
                    }
                }
            }

            if (lowestF > 10000) {
                // There is no good path
                return Direction.CENTER;
            }

            if (current == null) {
                System.out.println("BAD");
                continue;
            }

            nodeMap[current.x][current.y].open = false;

            if (current.equals(to)) {
                if (nodeMap[current.x][current.y].cameFrom == null) {
                    System.out.println("From == goal???");
                    return Direction.CENTER;
                }

                while (!nodeMap[current.x][current.y].cameFrom.equals(from)) {
                    current = nodeMap[current.x][current.y].cameFrom;

                    if (nodeMap[current.x][current.y].cameFrom == null) {
                        System.out.println("From == goal??? and used " + exploredNodes + " exploration steps");
                        return Direction.CENTER;
                    }
                }

//                System.out.println("Searched! Nodes: " + exploredNodes + ", Backtrack: " + backTrackCost + ", Search: " + (Clock.getBytecodeNum() - backTrackBeforeCost - backTrackCost));
//
//                if (map[current.x][current.y] != MapType.EMPTY && !hasPrinted) {
//                    StringBuilder message = new StringBuilder();
//                    for (int i = 0; i < width; i++) {
//                        for (int j = 0; j < height; j++) {
//                            if (i == from.x && j == from.y) {
//                                message.append("O");
//                            } else if (i == to.x && j == to.y) {
//                                message.append("G");
//                            } else if (map[i][j] == MapType.EMPTY) {
//                                message.append(" ");
//                            } else {
//                                message.append("X");
//                            }
//                        }
//                        message.append("\n");
//                    }
//                    System.out.println(message);
//                    hasPrinted = true;
//                }

                return from.directionTo(current);
            }

            for (int i = 0; i < 8; i++) {
                int neighborX = current.x + neighborDirections[i].dx;
                int neighborY = current.y + neighborDirections[i].dy;

                if (neighborX < 0 || neighborX >= width || neighborY < 0 || neighborY >= height) {
                    continue;
                }

                double tentativeG = nodeMap[current.x][current.y].g + 1;

                if (map[neighborX][neighborY] == MapType.WATER) {
                    tentativeG += 1;
                } else if (map[neighborX][neighborY] != MapType.EMPTY) {
                    tentativeG += 10000; // DO NOT ENTER
                }

                if (tentativeG < nodeMap[neighborX][neighborY].g) {
                    nodeMap[neighborX][neighborY].cameFrom = current;
                    nodeMap[neighborX][neighborY].g = tentativeG;
                    nodeMap[neighborX][neighborY].f = tentativeG + h(new MapLocation(neighborX, neighborY), to);
                    if (!nodeMap[neighborX][neighborY].open) {
                        openNodes++;
                    }
                    nodeMap[neighborX][neighborY].open = true;
                }
            }
        }

        return Direction.CENTER;
    }
}

