import std/sequtils, std/sets, std/strformat, std/strscans, std/tables
import std/sugar

type
    Coord3D* = (int, int, int)

iterator adjacent(coords: Coord3D): Coord3D =
    let (x, y, z) = coords
    yield (x + 1, y, z)
    yield (x - 1, y, z)
    yield (x, y + 1, z)
    yield (x, y - 1, z)
    yield (x, y, z + 1)
    yield (x, y, z - 1)

proc scanSurface(cubes: HashSet[Coord3D]): int =
    for coords in cubes:
        for adj in adjacent(coords):
            if not cubes.contains(adj):
                result += 1

proc markConnectedAirComponent(
    coords: Coord3D,
    cubes: HashSet[Coord3D],
    visited: var HashSet[Coord3D],
    componentNo: int,
    componentToCoords: var Table[int, HashSet[Coord3D]],
    upperBound: int): bool =
    let (x, y, z) = coords
    if [x, y, z].any(v => v < 0 or v > upperBound):
        return false
    if visited.contains(coords) or cubes.contains(coords):
        return false

    visited.incl(coords)
    if not componentToCoords.hasKey(componentNo):
        componentToCoords[componentNo] = initHashSet[Coord3D]()
    componentToCoords[componentNo].incl(coords)

    for adj in adjacent(coords):
        discard markConnectedAirComponent(adj, cubes, visited, componentNo, componentToCoords, upperBound)
    
    return true


var cubes = initHashSet[Coord3D]()

var upper_bound = 0
for line in io.stdin.lines:
    var x, y, z: int
    if line.scanf("$i,$i,$i", x, y, z):
        cubes.incl((x, y, z))
        for n in [x, y, z]:
            upper_bound = max(n + 1, upper_bound)
            
let surface = cubes.scanSurface()

echo fmt"Coords upper bound {upper_bound}"

var visited = initHashSet[Coord3D]()
var componentToCoords = initTable[int, HashSet[Coord3D]]()
var componentNo = 0

for x in 0..upper_bound:
    for y in 0..upper_bound:
        for z in 0..upper_bound:
            if markConnectedAirComponent((x, y, z), cubes, visited, componentNo, componentToCoords, upper_bound):
                componentNo += 1

echo fmt"Found {componentNo - 1} inner air components"

# echo fmt"Component 0 has {componentToCoords[0].len()} air"

var trappedAirSurface = 0
for comp in 1..<componentNo:
    # echo fmt"Component {comp} {componentToCoords[comp]}"
    trappedAirSurface += componentToCoords[comp].scanSurface()

echo fmt"Surface area {surface}, exterior surface area {surface - trappedAirSurface}"