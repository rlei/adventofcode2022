# Advent of Code 2022 - Day 19

Solved with [Julia](https://julialang.org/) `1.8.3`, [JuMP](https://jump.dev/JuMP.jl/stable/)
and [HiGHS](https://github.com/jump-dev/HiGHS.jl) as the LP solver.

## Problem 1 & 2

To run:

`julia day19.jl < path/to/input_data`

Note for the first run, it needs a moment to download the needed packages.

## See also

* https://calogica.com/julia/optimization/2018/08/12/linear-programming-example-julia-2ways.html
* Cbc solver crashes on M1 MBP: https://discourse.julialang.org/t/malloc-error-with-model-cbc-optimizer-on-v1-8-0-beta3-and-m1/78876/2
  * "Use HiGHS.jl instead. It’s also open source and faster than Cbc."