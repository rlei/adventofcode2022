.equ MACHO_EXIT, 1
.equ MACHO_READ, 3
.equ MACHO_WRITE, 4

.equ STDIN,  0
.equ STDOUT, 1
.equ LINE_BUF_LEN, 256

.macro  adr_p   reg, label
    adrp    \reg, \label@PAGE
    add     \reg, \reg, \label@PAGEOFF
.endm

.macro load_next_item src_ptr, dest
    ldrb    \dest, [\src_ptr], #1   // load and incr
    cmp     \dest, #'Z'
    b.gt    #12     // 3 instructions forward (including this one)
    sub     \dest, \dest, #38   // A-Z to 27-52
    b       #8      // 2 instruction forward (including this one)
    sub     \dest, \dest, #96   // a-z to 1-26
.endm
