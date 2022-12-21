## Advent of Code 2022 - Day 18

Written in [Nim](https://nim-lang.org/) `1.6.10`.

My first Nim program.

### Problem 1 & 2

To compile:

`nim c -d:release -d:nimCallDepthLimit=8000 day18.nim`

(sorry for the large call depth - I'm too lazy to convert it to BFS xD)

To run:

`./day18 < path/to/input_data`

Or to compile and run at the same time:

`nim c -d:release -d:nimCallDepthLimit=8000 -r day18.nim < path/to/input_data`
