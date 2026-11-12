/*
 *   
 *
 * Copyright  1990-2007 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 */

# include "incls/_precompiled.incl"
# include "incls/_StackmapListDesc.cpp.incl"

const jushort StackmapListDesc::SHORT_ENTRY_FLAG              = 0x1;
const jushort StackmapListDesc::SHORT_ENTRY_STACK_HEIGHT_MASK = 0x3;
const jushort StackmapListDesc::SHORT_ENTRY_STACK_HEIGHT_MAX  = 0x3; 
const jushort StackmapListDesc::SHORT_ENTRY_LOCALS_MAX        = 0xF; 
const jushort StackmapListDesc::SHORT_STACKMAP_BITS_MAX       = 0x10;  //16
const jushort StackmapListDesc::SHORT_ENTRY_BCI_MAX           = 0x1FF; //511

// Initializes the object after allocation

void StackmapListDesc::initialize(OopDesc* klass, int size) {
  _entry_count = size;
  OopDesc::initialize(klass);
}

// amiga-j2me: the flag test lives in a function of its own, on a value in a
// register. Written inline, bebbo's m68k gcc 6.5 compiled "(*entries & 1) == 0"
// to "btst #-24,(a0)", which tests bit 24 of the entry, not bit 0: long maps
// were then skipped (and collected) whenever the heap's addresses had bit 24
// set, and short maps were taken for pointers at random.
// tools/check-m68k-bittest.sh looks for that instruction in every build.
static bool aj_is_long_map_entry(unsigned int entry_stat) __attribute__((noinline));
static bool aj_is_long_map_entry(unsigned int entry_stat) {
  return (entry_stat & 1u) == 0;
}

void StackmapListDesc::variable_oops_do(void do_oop(OopDesc**)) {
  unsigned int* entries = stackmap_entries_base();
  unsigned int* end = entries + _entry_count;

  while (entries < end) {
    if (aj_is_long_map_entry(*entries)) {
      do_oop((OopDesc**)entries);
    }
    ++ entries;
  }
}

bool StackmapListDesc::is_shortmap_type(juint locals, juint stack_height, 
                                        juint bci) {
  if ((locals > SHORT_ENTRY_LOCALS_MAX) ||
      (bci > SHORT_ENTRY_BCI_MAX) ||
      (stack_height > SHORT_ENTRY_STACK_HEIGHT_MAX)) {
    return false;
  }

  if (stack_height + locals > SHORT_STACKMAP_BITS_MAX) {
    return false;
  }
  
  return true;
}
