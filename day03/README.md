## Advent of Code 2022 - Day 3

Solved with AArch64 (ARMv8, or ARM 64) assembly on an M1 MBP 2021 running macOS Ventura 13.0.1. XCode CLT is required.

For the same code to work on Linux, several things need to be adjusted:
* syscall numbers
* varargs passing (in registers vs. on stack)
* directives (`.equ` vs `equ` etc)

To build, run `make`.

### Problem 1

To run:

`./prob1 < path/to/input_file`

Note there's also a Python solution `1.py` I created for debugging the assembly one.

### Problem 2

To run:

`./prob2 < path/to/input_file`

### See also

A bunch of materials I stumbled upon during the coding.

* https://github.com/below/HelloSilicon
* https://community.arm.com/arm-community-blogs/b/architectures-and-processors-blog/posts/using-the-stack-in-aarch64-implementing-push-and-pop
* https://modexp.wordpress.com/2018/10/30/arm64-assembly/
* https://thinkingeek.com/2016/11/13/exploring-aarch64-assembler-chapter-5/
* https://stackoverflow.com/questions/71294682/how-to-create-an-array-in-armv8-assembly
* https://modexp.wordpress.com/2018/10/30/arm64-assembly/
* https://lldb.llvm.org/use/map.html
