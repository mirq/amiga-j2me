| amiga-j2me, WarpOS: the 68k helper process that runs library calls for the
| PPC program (hal_tls_amissl.c).
|
| Why it is started in this roundabout way: the Sonnet powerpc.library patches
| AllocMem and sends every request to "ppc memory" (which the 68060 reaches
| several times more slowly than its Fast RAM) when the calling task is marked
| PPC, or when its Task structure lies in ppc memory. A process created by the
| PPC program's own 68k side gets its Process structure from that patched
| AllocMem, so it is "PPC" for good, and with it all that AmiSSL allocates.
| So:
|   boot      runs on the PPC program's 68k side (Run68K). It takes a block
|             straight from a Fast RAM header with Allocate(), which is not
|             patched, copies this code there and starts a plain exec task in
|             it;
|   boottask  that task, clean, creates the helper process: its structure and
|             stack now come from Fast RAM;
|   start     the helper: takes requests from its message port.
| The block: 0 the boot task's structure (cleared, the size of a Process),
| 256 its stack, HEAD this code. The PPC side frees it when the helper has gone.
|
| The word after the first instruction is patched with the address of the
| shared block: 0 = the helper's message port (set here), 4 = set to 1 when the
| helper has gone or could not be made.
| A request is a Message followed by: 20 function address, 24 a6, 28 d0, 32 d1,
| 36 a0, 40 a1, 44 a2, 48 a3, 52 result, 56 quit flag.
|
| Assemble: tools/gen-tls-stub.sh (writes tls68k.h).
        .equ    PROC,256
        .equ    HEAD,256+8192
        .text
start:  movea.l #0x12345678,%a5
        movea.l 4.w,%a6
        jsr     -666(%a6)               | CreateMsgPort
        move.l  %d0,(%a5)
        beq.s   gone
loop:   movea.l (%a5),%a0
        jsr     -384(%a6)               | WaitPort
        movea.l (%a5),%a0
        jsr     -372(%a6)               | GetMsg
        tst.l   %d0
        beq.s   loop
        movea.l %d0,%a3
        tst.l   56(%a3)
        bne.s   quit
        movem.l %a3/%a5/%a6,-(%sp)
        move.l  28(%a3),%d0
        move.l  32(%a3),%d1
        movea.l 36(%a3),%a0
        movea.l 40(%a3),%a1
        movea.l 44(%a3),%a2
        movea.l 20(%a3),%a4
        movea.l 24(%a3),%a6
        movea.l 48(%a3),%a3
        jsr     (%a4)
        movem.l (%sp)+,%a3/%a5/%a6
        move.l  %d0,52(%a3)
        movea.l %a3,%a1
        jsr     -378(%a6)               | ReplyMsg
        bra.s   loop
quit:   jsr     -132(%a6)               | Forbid: nothing may run between the reply and the end
        movea.l (%a5),%a0
        jsr     -672(%a6)               | DeleteMsgPort
        clr.l   (%a5)
        movea.l %a3,%a1
        jsr     -378(%a6)               | ReplyMsg
gone:   moveq   #1,%d0
        move.l  %d0,4(%a5)
        moveq   #0,%d0
        rts

| d0 = boot(a0 = these bytes, d0 = their count): the block, or 0
        .globl  boot
boot:   movem.l %d2-%d3/%a2-%a3/%a6,-(%sp)
        movea.l %a0,%a2
        move.l  %d0,%d2
        movea.l 4.w,%a6
        jsr     -132(%a6)               | Forbid
        movea.l 322(%a6),%a3            | the memory headers
1:      tst.l   (%a3)
        beq.s   none
        move.w  14(%a3),%d0
        btst    #2,%d0                  | Fast RAM
        beq.s   2f
        btst    #13,%d0                 | but not the PPC card's
        bne.s   2f
        move.l  %d2,%d0
        add.l   #HEAD,%d0
        movea.l %a3,%a0
        jsr     -186(%a6)               | Allocate
        tst.l   %d0
        bne.s   got
2:      movea.l (%a3),%a3
        bra.s   1b
none:   jsr     -138(%a6)               | Permit
        moveq   #0,%d0
        bra.s   out
got:    jsr     -138(%a6)               | Permit (keeps the registers)
        movea.l %d0,%a3
        move.l  %d0,%d3
        movea.l %a3,%a0
        move.w  #HEAD/4-1,%d1
3:      clr.l   (%a0)+
        dbra    %d1,3b
        lea     HEAD(%a3),%a1
4:      move.b  (%a2)+,(%a1)+
        subq.l  #1,%d2
        bne.s   4b
        jsr     -636(%a6)               | CacheClearU
        lea     HEAD(%a3),%a0           | the copy
        move.b  #1,8(%a3)               | NT_TASK
        lea     name-start(%a0),%a1
        move.l  %a1,10(%a3)
        lea     PROC(%a3),%a1
        move.l  %a1,58(%a3)             | tc_SPLower
        move.l  %a0,62(%a3)             | tc_SPUpper
        move.l  %a0,54(%a3)             | tc_SPReg
        lea     74(%a3),%a1             | tc_MemEntry: an empty list
        move.l  %a1,8(%a1)
        addq.l  #4,%a1
        move.l  %a1,74(%a3)
        lea     208(%a3),%a1            | where a Process has its local variables: empty too
        move.l  %a1,8(%a1)
        addq.l  #4,%a1
        move.l  %a1,208(%a3)
        lea     boottask-start(%a0),%a2
        movea.l %a3,%a1
        suba.l  %a3,%a3
        jsr     -282(%a6)               | AddTask
        move.l  %d3,%d0
out:    movem.l (%sp)+,%d2-%d3/%a2-%a3/%a6
        rts

boottask:
        movea.l 4.w,%a6
        movea.l start+2(%pc),%a5
        lea     dosname(%pc),%a1
        moveq   #37,%d0
        jsr     -552(%a6)               | OpenLibrary
        tst.l   %d0
        beq.w   bfail
        movea.l %d0,%a6
        clr.l   -(%sp)
        clr.l   -(%sp)                  | TAG_DONE
        moveq   #-1,%d0
        move.l  %d0,-(%sp)
        move.l  #0x800003F7,-(%sp)      | NP_WindowPtr: no requesters
        clr.l   -(%sp)
        move.l  #0x800003FA,-(%sp)      | NP_Cli
        clr.l   -(%sp)
        move.l  #0x800003F9,-(%sp)      | NP_CopyVars
        clr.l   -(%sp)
        move.l  #0x800003F8,-(%sp)      | NP_HomeDir
        clr.l   -(%sp)
        move.l  #0x800003F6,-(%sp)      | NP_ConsoleTask
        clr.l   -(%sp)
        move.l  #0x800003F2,-(%sp)      | NP_CurrentDir
        clr.l   -(%sp)
        move.l  #0x800003ED,-(%sp)      | NP_Output
        clr.l   -(%sp)
        move.l  #0x800003EC,-(%sp)      | NP_Input
        move.l  #262144,-(%sp)
        move.l  #0x800003F3,-(%sp)      | NP_StackSize
        pea     name(%pc)
        move.l  #0x800003F4,-(%sp)      | NP_Name
        pea     start(%pc)
        move.l  #0x800003EB,-(%sp)      | NP_Entry
        move.l  %sp,%d1
        jsr     -498(%a6)               | CreateNewProc
        lea     96(%sp),%sp
        move.l  %d0,%d2
        movea.l %a6,%a1
        movea.l 4.w,%a6
        jsr     -414(%a6)               | CloseLibrary
        tst.l   %d2
        bne.s   bdone
bfail:  moveq   #1,%d0
        move.l  %d0,4(%a5)
bdone:  rts

name:   .asciz  "amiga-j2me tls"
dosname: .asciz "dos.library"
        .even
