// Advent of Code 2022 - Day 3.1

.include "common.s"

.text

.global _start
.align 4

_start:
    mov     X0, #STDIN
    adr     X1, openmode
    bl      _fdopen
    // X19-X29 are callee saved
    mov     X19, X0     // X19: STDIN as FILE*
    mov     X20, #0     // X20: sum of items priority appeared in both compartments

readline:
    adr_p   X0, linebuf
    mov     X1, #LINE_BUF_LEN
    mov     X2, X19     // STDIN as FILE*
    bl      _fgets

    cbz     X0, completed   // EOF?

    bl      _strlen
    sub     X0, X0, 1   // chop '\n'
    lsr     X2, X0, #1  // half line length

    adr_p   X1, linebuf
    mov     X3, #1
    mov     X4, #0
    mov     X6, #0

first_half:
    // X0: line len
    // X1: ptr to next char in line
    // X2: length of remaining chars in first half
    // X3: const 1
    // X6: bitset of items seen in the first half
    load_next_item X1, W4
    lsl     X5, X3, X4
    orr     X6, X6, X5

    sub     X2, X2, #1
    cbnz    X2, first_half

    lsr     X2, X0, #1  // half line length

second_half:
    // X1: ptr to next char in line
    // X2: length of remaining chars in the second half
    // X3: const 1
    // X6: bitset of items seen in the first half
    load_next_item X1, W4
    lsl     X5, X3, X4
    tst     X6, X5
    b.eq    next_char

    add     X20, X20, W4, uxtw  // accumulate total priorities
    b       readline

next_char:
    sub     X2, X2, #1
    cbnz    X2, second_half

    b       readline

completed:
    adr     X0, fmt
    // macOS aarch64 ABI requires varargs passed on stack
    // note SP must be 16 bytes aligned, hence move sp by 16 but only use 8 for X20
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
