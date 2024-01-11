# Honey Ducklings

This is the code for the Honey Ducklings team as part of MIT Battlecode 2024 https://battlecode.org/

# Strategy

## Defense wins championships
Build a strong defense around our bread. If we can't capture theirs, at least they won't capture ours.
- Take all three flags at the start and move them the farthest we can
- Hold all three flags such that `rc.getAllySpawnLocations()` returns nothing
- Build a moat around where the three flags are and set up defenders

## Offense is the best defense
The first two tiebreakers (except flags) are sum of all unit levels and amount of crumbs. We can maximize both of these by attacking the enemy in their zone.
- Align an offense just on the enemy's dam line
- Attack enemies as they come near. Each kill grants us 50 crumbs
- Place traps between dam spots to bait enemies into approaching
  - Have one or two builders building these for max builder points
- Place healers behind attackers

# Versions

## Version 1

Just the starter bot with some taped-on features such as preference for moving towards enemy flags, protecting the flag carrier, and attacking enemies in range.