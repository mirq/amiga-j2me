; amiga-j2me: C-callable wrapper around Mikael Kalms' c2p_rect (c2p_rect.s,
; verbatim from kalms-c2p, https://github.com/Kalmalyzer/kalms-c2p; the same
; files serve the author's BobrHopper and OpenTTD ports).
;
; void aj_c2p_rect(const struct AjC2P *a);
;   0  UWORD x        (multiple of 32)
;   2  UWORD y
;   4  UWORD w        (multiple of 32)
;   6  UWORD h
;   8  UWORD cmod     chunky buffer stride in bytes
;  10  UWORD bmod     bitplane BytesPerRow
;  12  ULONG bplsize  bytes per bitplane (planes contiguous, bplsize apart)
;  16  APTR  chunky   base of the chunky buffer (the routine adds x/y)
;  20  APTR  bpl      base of plane 0
;
; c2p_rect takes everything in registers, d2-d7 included, which C expects
; kept: they are saved here before they are loaded.
;
; Assemble: vasmm68k_mot -Fhunk -m68040 -no-opt -o c2p_glue.o c2p_glue.s

	section	.text,code

	xdef	_aj_c2p_rect

_aj_c2p_rect:
	movem.l	d2-d7/a2-a6,-(sp)
	move.l	48(sp),a1		; 11 saved longs = 44, +4 return address
	move.w	(a1),d0
	move.w	2(a1),d1
	move.w	4(a1),d2
	move.w	6(a1),d3
	move.w	8(a1),d4
	move.w	10(a1),d5
	move.l	12(a1),d6
	move.l	16(a1),a0
	move.l	20(a1),a1
	jsr	c2p_rect
	movem.l	(sp)+,d2-d7/a2-a6
	rts

	include	"c2p_rect.s"
