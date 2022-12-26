import Foundation

enum Facing: Int, CaseIterable {
    case right
    case down
    case left
    case up
}

enum Turn {
    case left       // counter-clockwise 90°
    case right      // clockwise 90°
}

enum Action {
    case forward(Int)
    case turn(Turn)
}

struct ActionSeq: Sequence {
    let path: String

    func makeIterator() -> ActionIterator {
        return ActionIterator(path)
    }
}

struct ActionIterator: IteratorProtocol {
    let path: String
    var off: String.Index

    init(_ path: String) {
        self.path = path 
        self.off = path.startIndex
    }

    mutating func next() -> Action? {
        var action: [Character] = []
        while off < path.endIndex {
            let ch = path[off]
            if ch == "L" || ch == "R" {
                if action.isEmpty {
                    // consume
                    off = path.index(after: off)
                    return Action.turn(ch == "L" ? Turn.left : Turn.right)
                }
                return Action.forward(Int(String(action))!)
            }
            off = path.index(after: off)
            action.append(ch)
        }
        if !action.isEmpty {
            return Action.forward(Int(String(action))!)
        }
        return nil
    }
}

// inclusive - inclusive
typealias TileRange = (start: Int, end: Int)
typealias TileAreaMap = [TileRange]

func nonEmptyTileRange(tiles: [Character]) -> TileRange {
    return (start: tiles.firstIndex(where: {$0 != " "})!, end: tiles.lastIndex(where: {$0 != " "})!)
}

func findRowTileArea(map: [[Character]]) -> TileAreaMap {
    var rowTileArea: TileAreaMap = []
    for row in map {
        rowTileArea.append(nonEmptyTileRange(tiles: row))
    }
    return rowTileArea
}

func findColTileArea(map: [[Character]]) -> TileAreaMap {
    var colTileArea: TileAreaMap = []
    let cols = map.map({$0.count}).max()!
    for colNo in 0..<cols{
        let col = map.map( { colNo < $0.count ? $0[colNo] : nil }).filter({ $0 != nil}).map({ $0! })
        colTileArea.append(nonEmptyTileRange(tiles: col))
    }
    return colTileArea
}

var rawMap: [[Character]] = []

while let line = readLine(strippingNewline: true), line != "" {
    rawMap.append(Array(line))
}
let path = readLine(strippingNewline: true)!

var rowTileArea: TileAreaMap = findRowTileArea(map: rawMap)
var colTileArea: TileAreaMap = findColTileArea(map: rawMap)

func moveIn2dSurfaces() -> (Int, Int, Facing) {
    var row = 0
    var col = rowTileArea[row].start

    var facing = Facing.right

    for nextAction in ActionSeq(path: path) {
        switch nextAction {
        case .forward(let steps):
            for _ in 0..<steps {
                var nextRow = row
                var nextCol = col
                switch facing {
                case .right:
                    nextCol += 1
                    if nextCol > rowTileArea[row].end {
                        nextCol = rowTileArea[row].start
                    }
                case .left:
                    nextCol -= 1
                    if nextCol < rowTileArea[row].start{
                        nextCol = rowTileArea[row].end
                    }
                case .down:
                    nextRow += 1
                    if nextRow > colTileArea[col].end {
                        nextRow = colTileArea[col].start
                    }
                case .up:
                    nextRow -= 1
                    if nextRow < colTileArea[col].start{
                        nextRow = colTileArea[col].end
                    }
                }
                if rawMap[nextRow][nextCol] != "#" {
                    row = nextRow
                    col = nextCol
                }
            }
            
        case .turn(.left):
            var rawFacing = facing.rawValue - 1
            if rawFacing < 0 {
                rawFacing = Facing.allCases.count - 1
            }
            facing = Facing(rawValue: rawFacing)!
            // y += 1

        case .turn(.right):
            let rawFacing = (facing.rawValue + 1) % Facing.allCases.count
            facing = Facing(rawValue: rawFacing)!
            // y += 1
        }
    }
    return (row, col, facing)
}
let (row, col, facing) = moveIn2dSurfaces()
print("row \(row), col \(col), facing \(facing)")
print("final password: \((row + 1) * 1000 + (col + 1) * 4 + facing.rawValue)")

// part 2
let cubeEdgeSize = Int(Float(rawMap.joined().filter({$0 != " "}).count / 6).squareRoot())

enum Flipped {
    case no 
    case yes
}

struct Exit: Hashable {
    let x: Int
    let y: Int
    let facing: Facing
}

struct Entry: Hashable {
    let x: Int
    let y: Int
    let facing: Facing
    let flipped: Flipped
}

// surface exit facing -> surface entry facing
// note the *entry* facing is opposite to the border direction. i.e. entry facing left means to enter from the right side
let adjacent = [
    //          (1,0) (2,0)
    //          (1,1)
    //    (0,2) (1,2)
    //    (0,3)

    // connected in the flattened map
    Exit(x: 1, y: 0, facing:.right): Entry(x: 2, y: 0, facing: .right, flipped: .no),
    Exit(x: 1, y: 0, facing:.down):  Entry(x: 1, y: 1, facing: .down,  flipped: .no),
    Exit(x: 1, y: 1, facing:.down):  Entry(x: 1, y: 2, facing: .down,  flipped: .no),
    Exit(x: 1, y: 2, facing:.left):  Entry(x: 0, y: 2, facing: .left,  flipped: .no),
    Exit(x: 0, y: 2, facing:.down):  Entry(x: 0, y: 3, facing: .down,  flipped: .no),

    // not connected in the flattened map
    Exit(x: 2, y: 0, facing:.down):  Entry(x: 1, y: 1, facing: .left,  flipped: .no),
    Exit(x: 1, y: 2, facing:.down):  Entry(x: 0, y: 3, facing: .left,  flipped: .no),
    Exit(x: 1, y: 1, facing:.left):  Entry(x: 0, y: 2, facing: .down,  flipped: .no),
    Exit(x: 1, y: 0, facing:.up):    Entry(x: 0, y: 3, facing: .right, flipped: .no),
    Exit(x: 1, y: 0, facing:.left):  Entry(x: 0, y: 2, facing: .right, flipped: .yes),
    Exit(x: 2, y: 0, facing:.up):    Entry(x: 0, y: 3, facing: .up,    flipped: .no),
    Exit(x: 2, y: 0, facing:.right): Entry(x: 1, y: 2, facing: .left,  flipped: .yes),
]

func opposite(facing: Facing) -> Facing {
    return Facing(rawValue: (facing.rawValue + 2) % Facing.allCases.count)!
}

func mapAdjacent(adjMap: [Exit: Entry]) -> [Exit: Entry] {
    var map: [Exit: Entry] = [:]
    for (exit, entry) in adjMap {
        map[exit] = entry
        let reversedExit = Exit(x: entry.x, y: entry.y, facing: opposite(facing: entry.facing))
        let reversedEntry = Entry(x: exit.x, y: exit.y, facing: opposite(facing: exit.facing), flipped: entry.flipped)
        map[reversedExit] = reversedEntry
    }
    return map
}

let fullAdjacent = mapAdjacent(adjMap: adjacent)

func moveToSurface(x: Int, y: Int, facing: Facing) -> (Int, Int, Facing) {
    let fromSurfaceX = x / cubeEdgeSize
    let fromSurfaceY = y / cubeEdgeSize
    let xOffset = x % cubeEdgeSize
    let yOffset = y % cubeEdgeSize

    var off = facing == .right || facing == .left ? yOffset : xOffset
    let exit = Exit(x: fromSurfaceX, y: fromSurfaceY, facing: facing)
    if (fullAdjacent[exit] == nil) {
        print("no adjacent defined for \(exit)")
    }
    let nextSurface = fullAdjacent[exit]!
    if (nextSurface.flipped == .yes) {
        off = cubeEdgeSize - 1 - off
    }
    
    var nextX: Int
    var nextY: Int
    switch (nextSurface.facing) {
    case .right:
        nextX = 0
        nextY = off
    case .left:
        nextX = cubeEdgeSize - 1
        nextY = off
    case .down:
        nextX = off
        nextY = 0
    case .up:
        nextX = off
        nextY = cubeEdgeSize - 1
    }

    return (nextSurface.x * cubeEdgeSize + nextX, nextSurface.y * cubeEdgeSize + nextY, nextSurface.facing)
}

func nextPosition(row: Int, col: Int, facing: Facing) -> Optional<(Int, Int, Facing)> {
    var nextRow = row
    var nextCol = col
    var nextFacing = facing
    var crossSurface = false
    switch facing {
    case .right:
        nextCol += 1
        if (nextCol % cubeEdgeSize == 0) {
            crossSurface = true
        }
    case .left:
        nextCol -= 1
        if (nextCol < 0 || nextCol % cubeEdgeSize == cubeEdgeSize - 1) {
            crossSurface = true
        }
    case .down:
        nextRow += 1
        if (nextRow % cubeEdgeSize == 0) {
            crossSurface = true
        }
    case .up:
        nextRow -= 1
        if (nextRow < 0 || nextRow % cubeEdgeSize == cubeEdgeSize - 1) {
            crossSurface = true
        }
    }
    if (crossSurface) {
        // Pass the old x,y here (before +1/-1), for moveToSurface to calculate the surface to move away
        // from. The x or y that just received +1/-1 won't be used, only the other unchanged one will.
        (nextCol, nextRow, nextFacing) = moveToSurface(x: col, y: row, facing: facing)
    }
    if rawMap[nextRow][nextCol] != "#" {
        // print("\(col) \(row) \(facing) to \(nextCol) \(nextRow) \(nextFacing)")
        return Optional.some((nextRow, nextCol, nextFacing))
    }
    return Optional.none
}

func moveIn3dSurfaces() -> (Int, Int, Facing) {
    var row = 0
    var col = rowTileArea[row].start

    var facing = Facing.right

    for nextAction in ActionSeq(path: path) {
        switch nextAction {
        case .forward(let steps):
            // print("forward \(steps)")
            for _ in 0..<steps {
                let nextPos = nextPosition(row: row, col: col, facing: facing)
                if nextPos != nil {
                    (row, col, facing) = nextPos!
                }
            }
            
        case .turn(.left):
            // print("turn left")
            var rawFacing = facing.rawValue - 1
            if rawFacing < 0 {
                rawFacing = Facing.allCases.count - 1
            }
            facing = Facing(rawValue: rawFacing)!
            // y += 1

        case .turn(.right):
            // print("turn right")
            var rawFacing = facing.rawValue + 1
            if rawFacing >= Facing.allCases.count {
                rawFacing = 0
            }
            facing = Facing(rawValue: rawFacing)!
            // y += 1
        }
    }
    return (row, col, facing)
}
let (row3d, col3d, facing3d) = moveIn3dSurfaces()
print("Part 2: row \(row3d), col \(col3d), facing \(facing3d)")
print("Part 2 final password: \((row3d + 1) * 1000 + (col3d + 1) * 4 + facing3d.rawValue)")