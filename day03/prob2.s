// Advent of Code 2022 - Day 3.2

.include "common.s"

.text

.global _start
.align 4

// return X0: 0 if EOF, X1: bitset of item types if X0 is not 0
readline:
    // Save LR as we'll call other routines. aarch64 requires SP to be 16 bytes aligned.
    str     X30, [SP, #-16]!

    adr_p   X0, linebuf
    mov     X1, #LINE_BUF_LEN
    mov     X2, X19         // STDIN as FILE*
    bl      _fgets
    cbz     X0, line_done   // EOF?

    bl      _strlen
    sub     X0, X0, 1       // chop '\n'
    mov     X1, #0
    adr_p   X2, linebuf
    mov     X3, #1
    mov     X4, #0
next_char:
    // X0: remaining chars
    // X1: bitset
    // X2: ptr to next char in line
    // X3: const 1
    load_next_item X2, W4
    lsl     X5, X3, X4
    orr     X1, X1, X5

    sub     X0, X0, #1
    cbnz    X0, next_char

    mov     X0, 1   // return "not eof"

line_done:
    ldr     X30, [SP], #16
    ret

_start:
    mov     X0, #STDIN
    adr     X1, openmode
    bl      _fdopen
    // X19-X29 are callee saved
    mov     X19, X0     // X19: STDIN as FILE*
    mov     X20, #0     // X20: sum of items priority appeared in both compartments

next_3_lines:
    bl      readline
    cbz     X0, completed   // EOF?
    mov     X21, X1
    bl      readline
    cbz     X0, completed   // EOF?
    and     X21, X21, X1
    bl      readline
    cbz     X0, completed   // EOF?
    and     X21, X21, X1

    // now there should be only 1 bit set in X21
    mov     X0, #63
    clz     X2, X21
    sub     X0, X0, X2

    add     X20, X20, X0    // accumulate total priorities

    b       next_3_lines

completed:
    adr     X0, fmt
    // macOS aarch64 ABI requires varargs passed on stack
    // SP must be 16 bytes aligned for aarch64, hence move sp by 16 but only use 8 for X20
    str     X20, [SP, #-16]!
    bl      _printf
    add     SP, SP, #16

    mov     X0, #0      // Exit code 0  
    mov     X16, #MACHO_EXIT
    svc     #0x80

openmode:   .asciz  "r"
fmt:        .asciz  "%d\n"

.bss
linebuf:    .space LINE_BUF_LEN
