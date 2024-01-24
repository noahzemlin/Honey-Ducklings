# Honey Ducklings

This is the code for the Honey Ducklings team as part of MIT Battlecode 2024 https://battlecode.org/

## Strategy and Techniques

### Micro

Each agent follows simple rules for each turn. Each turn goes as follows:

1. Try to spawn at the spawn location nearest to the commanded location (see Macro)
2. Attempt to place water traps on our spawn tiles if nearby
   1. Very effective at climbing low rating since most robots have not implemented filling
3. Attempt to pickup enemy flags if we can
4. If we are a "healer" (every 7th duck), attempt to heal
5. Attempt to attack
   1. Prioritize ducks that we can send to jail with one attack, then prioritize ducks that have ally flags
6. Attempt to move
   1. Grab nearby crumbs if on the way (first 50 turns focus on grabbing crumbs and exploring)
   2. If enemies are nearby, movement is based on a heuristic function combining sum of enemy distance, sum of ally distances, and nearest enemy
   3. Otherwise, attempt to move to the commanded location using a Bug2-like algorithm
   4. If the location we want to move is water, attempt to fill it
7. After moving, attempt to heal and attack again.

Additionally, every time we attack we increment a counter. After 10 attacks, we begin attempting to place a trap on ourselves so that traps are only placed during large encounters which is usually the most contentious locations and the best places to place traps.

### Macro

The Honey Ducklings macro is the weakest part of my strategy and is focused entirely on rushing the opponent to try to gain an early advantage and receive the 50 crumbs for each kill in opponent territory.

To make sure all agents act together, we elect one agent (the first in turn order) as the "Commander" and have it issue map locations over the Shared Array for all other ducks to flock to. Each duck shares information when it sees opponent flags to make sure the Commander knows the most about the current state of the match.

### Weaknesses and Improvements

- The agents path well on open maps but path very poorly in maps with small corridors. This is likely due to the bug algorithm trying to follow obstacles and getting confused on which wall it is following. In the future, adding a small local-search would help this.
- The micro is good when attacking, but it takes too many rounds (~20-50) for the ducks to "get into formation".
- There is 0 defense in this strategy and I quickly lose to agents who can sneak past the one large army I create.

## Version History

### Version 11
Minor heuristic optimization

### Version 10

Added heuristic function for planning around enemies. It might actually make things worse but it's a good idea.

### Version 9

Chase enemies

### Version 8

Added splitting the group into two under circumstances where lots of flags are captured.

### Version 7

Finally remembered to actually choose upgrades. Our upgrade path will be attacking > healing > capturing.

Ideally, capturing would be the highest but I don't really benefit from either effect based on my agent behavior.

### Version 6

Added basic setup with water+stun traps. Attempted to reduce teammates blocking each other.

### Version 5

Major overhaul. Replaced A* with Bug2. Added flag tracking.

### Version 4

Mostly just fixed a bug that caused the ducks to not attack if they were confused on where to go. We should always attack even if we are just standing AFK waiting.

### Version 3

Added a Commander duck who commands the other ducklings to their ~~doom~~ victory!

### Version 2

Added basic A* that will require a TON of optimization. Was this a smart choice? No, but it was fun to make.

### Version 1

Just the starter bot with some taped-on features such as preference for moving towards enemy flags, protecting the flag carrier, and attacking enemies in range.