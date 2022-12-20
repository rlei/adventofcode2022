## Advent of Code 2022 - Day 16

Written in [Rust](https://www.rust-lang.org/) `1.66.0`.

My first DFS try failed utterly for the part 2 problem. Then the BFS "search both together" approach took **20GB** RAM and 33 minutes to solve on an M1 Pro MBP.

Fortunately the final, simpler, divide and conquer solution needs only 20MB RAM and 13 seconds.

To build:

`cargo build -r`

### Problem 1

`target/release/day16 -m 30 < path/to/input_data`

To view verbose progress output:
`target/release/day16 -m 30 -v < path/to/input_data`

### Problem 2

`target/release/day16 -m 26 --prob2 < path/to/input_data`

To view verbose progress output:
`target/release/day16 -m 26 --prob2 -v < path/to/input_data`
