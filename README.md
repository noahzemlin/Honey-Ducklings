# Honey Ducklings

This is the code for the Honey Ducklings team as part of MIT Battlecode 2024 https://battlecode.org/

## Version 11
Minor heuristic optimization

## Version 10

Added heuristic function for planning around enemies. It might actually make things worse but it's a good idea.

## Version 9

Chase enemies

## Version 8

Added splitting the group into two under circumstances where lots of flags are captured.

## Version 7

Finally remembered to actually choose upgrades. Our upgrade path will be attacking > healing > capturing.

Ideally, capturing would be the highest but I don't really benefit from either effect based on my agent behavior.

## Version 6

Added basic setup with water+stun traps. Attempted to reduce teammates blocking each other.

## Version 5

Major overhaul. Replaced A* with Bug2. Added flag tracking.

## Version 4

Mostly just fixed a bug that caused the ducks to not attack if they were confused on where to go. We should always attack even if we are just standing AFK waiting.

## Version 3

Added a Commander duck who commands the other ducklings to their ~~doom~~ victory!

## Version 2

Added basic A* that will require a TON of optimization. Was this a smart choice? No, but it was fun to make.

## Version 1

Just the starter bot with some taped-on features such as preference for moving towards enemy flags, protecting the flag carrier, and attacking enemies in range.