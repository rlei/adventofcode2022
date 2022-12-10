package main

import (
	"bufio"
	"io"
	"os"
	"strconv"
	"strings"
	"sync"
)

type cpuState struct {
	cycleBeginClosed int
	cycleEndOpen     int
	regX             int
}

// Simulates the cathode-ray tube by listening to CPU state changes
func yourTube(wg *sync.WaitGroup, cpuStateChannel <-chan cpuState) {
	sumSignalStrength := 0
	nextSignalCheckCycle := 20
	for state := range cpuStateChannel {
		for cycle := state.cycleBeginClosed; cycle < state.cycleEndOpen; cycle++ {
			// note scan position is 0 based
			scanPos := (cycle - 1) % 40
			if scanPos < state.regX-1 || scanPos > state.regX+1 {
				print(".")
			} else {
				print("#")
			}

			if scanPos == 39 {
				println()
			}
		}
		if state.cycleBeginClosed <= nextSignalCheckCycle &&
			state.cycleEndOpen > nextSignalCheckCycle {
			sumSignalStrength += nextSignalCheckCycle * state.regX
			nextSignalCheckCycle += 40
		}
	}
	println("\nSum of signal strengths:", sumSignalStrength)
	wg.Done()
}

func main() {
	reader := bufio.NewReader(os.Stdin)

	stateChannel := make(chan cpuState)
	waitGroup := sync.WaitGroup{}
	waitGroup.Add(1)
	// it doesn't really need to run in parallel, but just for fun
	go yourTube(&waitGroup, stateChannel)

	// 1 based
	cycle := 1
	regX := 1

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			if err == io.EOF {
				break
			}
			panic(err)
		}
		instruction := strings.Split(strings.TrimSpace(line), " ")

		cycleStart := cycle
		switch instruction[0] {
		case "noop":
			cycle += 1
			stateChannel <- cpuState{
				cycleBeginClosed: cycleStart,
				cycleEndOpen:     cycle,
				regX:             regX,
			}
		case "addx":
			delta, err := strconv.Atoi(instruction[1])
			if err != nil {
				panic(err)
			}
			cycle += 2
			stateChannel <- cpuState{
				cycleBeginClosed: cycleStart,
				cycleEndOpen:     cycle,
				regX:             regX,
			}
			// after emitting cpu state
			regX += delta
		}
	}

	close(stateChannel)
	waitGroup.Wait()
}
