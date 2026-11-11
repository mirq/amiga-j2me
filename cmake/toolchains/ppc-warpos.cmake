# WarpOS PPC via adtools ppc-amigaos-gcc + MOS2WOS SDK overlay.
# Same contract as BlitzQuake_src (MiniGL_WOS_V19 Makefile.wos.gcc.common):
# objects are linked with `ld -r` into an ET_REL .warpelf, then converted ON THE
# AMIGA with:  Stack 100000 / Elf2Exe2 <name>.warpelf <name>
set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR ppc)
set(AJ_TARGET wos-ppc)

set(WOS_TOOLCHAIN_BIN /home/mirek/ppc-amigaos-gcc/native-build/root-cross/bin CACHE PATH "")
set(WOS_SDK_ROOT /home/mirek/Downloads/gg/ppc-warpos CACHE PATH "")
set(WOS_CPU 604e CACHE STRING "")

set(CMAKE_C_COMPILER ${WOS_TOOLCHAIN_BIN}/ppc-amigaos-gcc)
set(CMAKE_AR         ${WOS_TOOLCHAIN_BIN}/ppc-amigaos-ar)
set(CMAKE_RANLIB     ${WOS_TOOLCHAIN_BIN}/ppc-amigaos-ranlib)
set(WOS_LD           ${WOS_TOOLCHAIN_BIN}/ppc-amigaos-ld)
set(CMAKE_C_COMPILER_WORKS TRUE)   # can't link a test exe the normal way

set(WOS_ARCH_CFLAGS "-mcpu=${WOS_CPU} -mcall-sysv -mbig-endian -mmultiple -fno-pic -fno-pie -fno-stack-protector -fno-asynchronous-unwind-tables -fno-unwind-tables -fno-jump-tables")
set(WOS_CPPFLAGS "-nostdinc -U__AMIGAOS4__ -U__AMIGAOS4 -UAMIGAOS4 -U__amigaos4__ -U__amigaos4 -Uamigaos4 -U__NEWLIB__ -D__PPC__ -DWARPUP -DNEWLIB -DWOS \
 -isystem ${WOS_SDK_ROOT}/newlib/include -isystem ${WOS_SDK_ROOT}/os-includeppc -isystem ${WOS_SDK_ROOT}/include -isystem ${WOS_SDK_ROOT}/gcc-include")
set(CMAKE_C_FLAGS_INIT "${WOS_ARCH_CFLAGS} ${WOS_CPPFLAGS}")
