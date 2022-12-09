
def commands = System.in.newReader().readLines().collect {
    def (dir, stepsStr) = it.split(" ")
    [dir, stepsStr as int]
}

res = commands.inject([0,0],[0,0],[[0,0]]]) {
    state, command -> {
        def (head, tail, tailTrail) = state
        def (dir, steps) = command
        def (headX, headY) = head
        def (tailX, tailY) = tail
        switch (dir) {
            case "R":
                headX += steps
                if (headX > tailX + 1) {
                    tailY = headY
                    while (headX > tailX + 1) {
                        tailX++
                        tailTrail << [tailX, tailY] // the first move may be diagonal
                    }
                }
                break

            case "L":
                headX -= steps
                if (headX < tailX - 1) {
                    tailY = headY
                    while (headX < tailX - 1) {
                        tailX--
                        tailTrail << [tailX, tailY] // the first move may be diagonal
                    }
                }
                break

            case "U":
                headY += steps
                if (headY > tailY + 1) {
                    tailX = headX
                    while (headY > tailY + 1) {
                        tailY++
                        tailTrail << [tailX, tailY] // the first move may be diagonal
                    }
                }
                break

            case "D":
                headY -= steps
                if (headY < tailY - 1) {
                    tailX = headX
                    while (headY < tailY - 1) {
                        tailY--
                        tailTrail << [tailX, tailY] // the first move may be diagonal
                    }
                }
                break
        }
        [[headX, headY], [tailX, tailY], tailTrail]
    }
}

def (endHead, endTail, tailTrail) = res

def uniquePositions = GQ {
    from tail in tailTrail
    select distinct(tail)
}

// println uniquePositions
println uniquePositions.size()
