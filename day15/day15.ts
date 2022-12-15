// Requires Deno to run, not nodejs.
import { readLines } from "https://deno.land/std@0.168.0/io/read_lines.ts"

const lines: string[] = await fromAsync(readLines(Deno.stdin))

// See TC39 proposal https://github.com/tc39/proposal-array-from-async
async function fromAsync<T>(asyncIterator: AsyncIterable<T>): Promise<T[]> { 
    const res: T[] = []
    for await(const i of asyncIterator) res.push(i)
    return res
}

interface Point {
    x: number
    y: number
}

interface Diamond {
    center: Point
    radius: number
}

interface Segment {
    x1: number
    x2: number
}

const regex = /Sensor at x=(-?\d+),\s*y=(-?\d+): closest beacon is at x=(-?\d+),\s*y=(-?\d+)/

const devices = lines.map( l => {
    const res = regex.exec(l)
    if (res) {
        const [sensorX, sensorY, beaconX, beaconY] = res.slice(1).map(s => parseInt(s))
        return {sensor: {x: sensorX, y: sensorY}, beacon: {x: beaconX, y: beaconY}}
    } else {
        throw new Error(`unrecognized input: ${l}`);
    }
})

const diamonds: Diamond[] = devices.map( pair => {
    const {sensor: center, beacon: b} = pair;
    const manhattanDistance = Math.abs(b.x - center.x) + Math.abs(b.y - center.y)
    return { center: center, radius: manhattanDistance }
})

// See TC39 proposal https://github.com/tc39/proposal-array-grouping 
const deviceXsByY = devices.flatMap(pair => [pair.sensor, pair.beacon])
    .reduce((aMap, point) => {
        if (!aMap.has(point.y)) {
            aMap.set(point.y, new Set()) 
        }
        aMap.get(point.y)?.add(point.x)
        return aMap
    }, new Map<number, Set<number>>())

// const YtoCheck = 10
// const MaxCoord = 20
const YtoCheck = 2000000
const MaxCoord = 4000000

// problem 1
const segments = findSegments(diamonds, YtoCheck)
const numCoveredDevices = [...(deviceXsByY.get(YtoCheck) || [])]
    .filter(x => segments.some(seg => seg.x1 <= x && x <= seg.x2))
    .length

const numPositions = segments.map(seg => seg.x2 - seg.x1 + 1).reduce((acc, n) => acc + n, 0)
console.log(`At ${YtoCheck}: ${numPositions - numCoveredDevices} open positions, after excluding ${numCoveredDevices} covered devices`)

// problem 2. may be further optimized, such as pruning diamonds out of range with a tree
for (let y = 0; y <= MaxCoord; y++) {
    const segments = findSegments(diamonds, y)
    // we're told there's only one possible location, so checks can be simplified
    const res = segments.map(seg => {
        if (seg.x1 > 0) {
            return {x: seg.x1 - 1, y: y}
        } else if (seg.x2 < MaxCoord) {
            return {x: seg.x2 + 1, y: y}
        }
    }).find(result => result !== undefined)
    if (res) {
        console.log(`Available at ${res.x}, ${res.y}; frequency ${res.x * 4000000 + y}`)
        break
    }
} 

function findSegments(diamonds: Diamond[], y: number): Segment[] {
    return diamonds.filter(diamond => diamond.center.y - diamond.radius <= y && y <= diamond.center.y + diamond.radius)
        .map(diamond => {
            const { center: c, radius: r } = diamond
            const half = r - Math.abs(c.y - y)
            return { x1: c.x - half, x2: c.x + half }
        })
        .sort((a, b) => (a.x1 != b.x1 ? a.x1 - b.x1 : a.x2 - b.x2))
        .reduce((merged, next) => {
            if (!merged.length) {
                merged.push(next)
            } else {
                const prev = merged[merged.length - 1]
                if (prev.x2 < next.x1 - 1) {
                    merged.push(next)
                } else {
                    prev.x2 = Math.max(prev.x2, next.x2)
                    merged[merged.length - 1] = prev
                }
            }
            return merged
        }, <Segment[]>[])
}
