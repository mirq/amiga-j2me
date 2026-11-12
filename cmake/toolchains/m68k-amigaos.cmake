# bebbo m68k-amigaos-gcc, AOS 3.x, 68060 + FPU
# -fbbb=-: none of bebbo gcc 6.5's own optimiser passes. Its "s" pass drops a
# load and stores an uninitialised d0 (ExecutionStackDesc::relocate_all_pointers:
# the first full GC hung). The others are off as well, to be safe: without them
# the benchmark is the same within noise.
set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR m68k)
set(AJ_TARGET aos3-m68k)
set(TOOLCHAIN_PREFIX /opt/amiga/bin/m68k-amigaos-)
set(CMAKE_C_COMPILER   ${TOOLCHAIN_PREFIX}gcc)
set(CMAKE_CXX_COMPILER ${TOOLCHAIN_PREFIX}g++)
set(CMAKE_AR           ${TOOLCHAIN_PREFIX}ar)
set(CMAKE_RANLIB       ${TOOLCHAIN_PREFIX}ranlib)
set(CMAKE_C_FLAGS_INIT   "-m68060 -mhard-float -noixemul -fomit-frame-pointer -fbbb=-")
set(CMAKE_CXX_FLAGS_INIT "-m68060 -mhard-float -noixemul -fomit-frame-pointer -fbbb=- -fno-exceptions -fno-rtti")
set(CMAKE_EXE_LINKER_FLAGS_INIT "-noixemul")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
