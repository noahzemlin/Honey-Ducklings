package honeyducklings_v3;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.Map;

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
        public MapLocation cameFrom = null;
    }

    public static class QueueItem {
        public QueueItem nextItem = null;
        public double f;
        public int x;
        public int y;

        public QueueItem(double f, int x, int y) {
            this.f = f;
            this.x = x;
            this.y = y;
        }

        public QueueItem setNext(QueueItem item) {
            this.nextItem = item;
            return this;
        }

        public MapLocation getLocation() {
            return new MapLocation(x, y);
        }
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

    public static double h(MapLocation node, MapLocation goal) {
//        return (abs(node.x - goal.x) + abs(node.y - goal.y)) / 2.0;
        return sqrt(pow(node.x - goal.x, 2) + pow(node.y - goal.y, 2));
    }

    public static Direction planRoute(MapType[][] map, MapLocation from, MapLocation to) {
        int width = map.length;
        int height = map[0].length;

        if (from.equals(to) || to.x < 0 || to.y < 0 || to.x >= width || to.y >= height) {
            return Direction.CENTER;
        }

//        Direction firstStep = from.directionTo(to);
//        MapLocation firstStepLocation = from.add(firstStep);

        // SHORT CUT BABY
//        if (map[firstStepLocation.x][firstStepLocation.y] == MapType.EMPTY) {
//            return firstStep;
//        }

//        System.out.println("Original to: " + to);
//        int bytecodeBefore = Clock.getBytecodeNum();

        // Location must be acceptable! Search until we find one
        while (map[to.x][to.y] != MapType.EMPTY) {
            Direction backTrack = to.directionTo(from);
            to = to.add(backTrack);
            if (to.equals(from)) {
                return Direction.CENTER;
            }
        }

        // Okay, now keep going until we hit an obstacle
        while (true) {
            Direction backTrack = to.directionTo(from);
            MapLocation backTrackLocation = to.add(backTrack);

            if (map[backTrackLocation.x][backTrackLocation.y] == MapType.EMPTY) {
                to = backTrackLocation;
            } else {
                break;
            }
        }

//        System.out.println("Improved to: " + to + " from " + from + " using " + (Clock.getBytecodeNum() - bytecodeBefore) + " bytes");

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

        int openNodes = 1;
        int exploredNodes = 0;

        QueueItem head = new QueueItem(0, from.x, from.y);

        while (openNodes > 0) {

            // Don't burn all our bytecodes :(
//            if (Clock.getBytecodesLeft() < 1000) {
//                System.out.println("Out of byte codes :(");
//                return Direction.CENTER;
//            }

            if (exploredNodes == 10) {
                QueueItem lookingat = head;
                while (lookingat != null) {
//                    System.out.println(lookingat.f + ", ");
                    lookingat = lookingat.nextItem;
                }
            }

            exploredNodes++;

            MapLocation current = head.getLocation();
            head = head.nextItem;

            nodeMap[current.x][current.y].open = false;
            openNodes--;

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

//                System.out.println("Searched! Nodes: " + exploredNodes);

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
                    tentativeG += 3;
                } else if (map[neighborX][neighborY] != MapType.EMPTY) {
                    tentativeG += 10000; // DO NOT ENTER
                }

                if (tentativeG < nodeMap[neighborX][neighborY].g) {
                    MapLocation neighborLocation = new MapLocation(neighborX, neighborY);
                    nodeMap[neighborX][neighborY].cameFrom = current;
                    nodeMap[neighborX][neighborY].g = tentativeG;

                    double fscore = tentativeG + h(neighborLocation, to);

                    if (fscore < 20000 && !nodeMap[neighborX][neighborY].open) {
                        openNodes++;
                        nodeMap[neighborX][neighborY].open = true;

                        if (head == null) {
                            head = new QueueItem(fscore, neighborX, neighborY);
                        } else if (head.f > fscore) {
                            head = new QueueItem(fscore, neighborX, neighborY).setNext(head);

//                            System.out.println("Added new head to queue with score " + fscore);
                        } else {
                            QueueItem lookingAt = head;
                            int maxQueueItems = 8;
                            while (maxQueueItems >= 0 && lookingAt.nextItem != null && fscore > lookingAt.nextItem.f) {
                                lookingAt = head.nextItem;
                                maxQueueItems--;
                            }

//                            System.out.println("Added new item to queue with score " + fscore);

                            lookingAt.nextItem = new QueueItem(fscore, neighborX, neighborY).setNext(lookingAt.nextItem);
                        }
                    }
                }
            }
        }

        System.out.println("Out of explorable nodes!");
        return Direction.CENTER;
    }
}

