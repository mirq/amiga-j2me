/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp;

public class NotAllowedException extends RuntimeException {
    public NotAllowedException() {}
    public NotAllowedException(String s) { super(s); }
}
