import java.math.BigInteger

// read all and drop borders
var rawMap = generateSequence(::readLine)
        .map {
            it.substring(1, it.length - 1).toCharArray()
        }.toList().drop(1).dropLast(1)

val WIDTH = rawMap[0].size
val HEIGHT = rawMap.size
val HORIZONTAL_MASK = BigInteger.ONE.shiftLeft(WIDTH) - BigInteger.ONE
val VERTICAL_MASK = BigInteger.ONE.shiftLeft(HEIGHT) - BigInteger.ONE

val movingRightMap = filterToBitmap(rawMap, '>')
val movingLeftMap = filterToBitmap(rawMap, '<')

val transposedMap = rawMap[0].indices.map { i -> rawMap.map { it[i] }.toTypedArray().toCharArray() }.toList()
val movingUpMap = filterToBitmap(transposedMap, '^')
val movingDownMap = filterToBitmap(transposedMap, 'v')

fun move(start: Pair<Int, Int>, entryInValley: Pair<Int, Int>, exitInValley: Pair<Int, Int>, atMinute: Int): Int {
    var minutes = atMinute;

    var positions = setOf(start)

    while (positions.isNotEmpty()) {
        minutes++;
        var beforeMove = positions
//        println("${minutes} ${positions}")
        positions = positions.map { (x, y) -> nextSteps(x, y, minutes) }.flatten().toSet()
        if (positions.isEmpty()) {
            if (beforeMove.contains(start) || beforeMove.contains(entryInValley)) {
                positions = setOf(start)
            }
        } else if (positions.contains(exitInValley)) {
            return minutes + 1
        }
    }
    return -1
}

val start = Pair(0, -1)
val entryInValley = Pair(0, 0)
val exitInValley = Pair(WIDTH - 1, HEIGHT - 1)
val exit = Pair(WIDTH - 1, HEIGHT)
val toExit = move(start, entryInValley, exitInValley, 0)
println("Reached exit at minute ${toExit}")
val goBack = move(exit, exitInValley, entryInValley, toExit)
println("Went back to start point at minute ${goBack}")
val toExitAgain = move(start, entryInValley, exitInValley, goBack)
println("At the exit again at minute ${toExitAgain}")

fun rotateRight(n: BigInteger, numBits: Int, width: Int): BigInteger {
    val Nbits = numBits % width
    return n.shiftRight(Nbits) or (n and (BigInteger.ONE.shiftLeft(Nbits) - BigInteger.ONE)).shiftLeft(width - Nbits)
}

fun rotateLeft(n: BigInteger, numBits: Int, width: Int) = rotateRight(n, width - (numBits % width), width)

fun nextSteps(x: Int, y: Int, minutes: Int): List<Pair<Int, Int>> {
    val horizontalNearbyMaps = (-1..1).map {
        val row = y + it
        if (row < 0 || row >= HEIGHT) BigInteger.ZERO
        else rotateRight(movingRightMap[row], minutes, WIDTH).xor(HORIZONTAL_MASK) and
                rotateLeft(movingLeftMap[row], minutes, WIDTH).xor(HORIZONTAL_MASK)
    }
    val verticalNearbyMaps = (-1..1).map {
        val col = x + it
        if (col < 0 || col >= WIDTH) BigInteger.ZERO
        else rotateRight(movingDownMap[col], minutes, HEIGHT).xor(VERTICAL_MASK) and
                rotateLeft(movingUpMap[col], minutes, HEIGHT).xor(VERTICAL_MASK)
    }
    return listOf(Pair(-1, 0), start, Pair(0, 0), Pair(0, 1), Pair(1, 0))
            .filter { (dx, dy) -> x + dx >= 0 && x + dx < WIDTH && y + dy >= 0 && y + dy < HEIGHT }
            .filter { (dx, dy) ->
                // note nearby bitmaps are 0 based, and dx/dy -1 based
                horizontalNearbyMaps[dy+1] and BigInteger.ONE.shiftLeft(WIDTH - (x+dx) - 1) != BigInteger.ZERO &&
                        verticalNearbyMaps[dx+1] and BigInteger.ONE.shiftLeft(HEIGHT - (y+dy) - 1) != BigInteger.ZERO}
            .map { (dx, dy) -> Pair(x + dx, y + dy) }
}

fun stringToBigInt(s: CharArray, ch: Char) = BigInteger(String(s.map { if (it == ch) '1' else '0' }.toCharArray()), 2)

fun filterToBitmap(strMap: Iterable<CharArray>, ch: Char) = strMap.map {
    stringToBigInt(it, ch)
}.toTypedArray()