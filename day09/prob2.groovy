import java.lang.Math;

def commands = System.in.newReader().readLines().collect {
    def (dir, stepsStr) = it.split(" ")
    [dir, stepsStr as int]
}

def initialKnots = (0..<10).collect { [0, 0] }

res = commands.inject([initialKnots, [[0, 0]]]) {
    state, command -> {
        def (knots, tailTrail) = state
        def (dir, steps) = command

        def headStepX, headStepY;
        if (dir in ["L", "R"]) {
            headStepX = (dir == "R") ? 1 : -1
            headStepY = 0
        } else {
            headStepX = 0
            headStepY = (dir == "U") ? 1 : -1
        }
        while (steps--) {
            def (headX, headY) = knots[0]
            knots[0] = [headX + headStepX, headY + headStepY]

            for (idx in 1..<10) {
                // predecessor knot
                def (predX, predY) = knots[idx - 1]
                def (thisX, thisY) = knots[idx]

                def deltaX = predX - thisX;
                def deltaY = predY - thisY;
                if (Math.abs(deltaX) > 1 || Math.abs(deltaY) > 1) {
                    thisX += Integer.signum(deltaX);
                    thisY += Integer.signum(deltaY);

                    knots[idx] = [thisX, thisY]
                    if (idx == 9) {
                        tailTrail << knots[idx]
                    }
                } else {
                    break
                }
            }
        }
        // println knots
        // println tailTrail
        [knots, tailTrail]
    }
}

def (_, tailTrail) = res

def uniquePositions = GQ {
    from tail in tailTrail
    select distinct(tail)
}

// println tailTrail
println uniquePositions.size()
