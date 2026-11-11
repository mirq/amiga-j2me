cmake_minimum_required(VERSION 3.16)
project(amiga_j2me C CXX)

if(NOT AJ_TARGET)
  set(AJ_TARGET host)
endif()
message(STATUS "amiga-j2me target: ${AJ_TARGET}")

set(CMAKE_C_STANDARD 99)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()
set(CMAKE_C_FLAGS_RELEASE "-O2")
set(CMAKE_CXX_FLAGS_RELEASE "-O2")

if(AJ_TARGET STREQUAL "host")
  set(HAL_SRC hal/sdl-host/hal_host.c)
elseif(AJ_TARGET STREQUAL "aos3-m68k" OR AJ_TARGET STREQUAL "morphos-ppc" OR AJ_TARGET STREQUAL "wos-ppc")
  # The Amiga platform layer (from the civctp2 port) + our HAL on top of it
  set(HAL_SRC hal/amiga/hal_amiga.c hal/amiga/amiga_platform.c hal/amiga/amiga_clock.c
              hal/amiga/amiga_log.c hal/amiga/amiga_video.c hal/amiga/amiga_input.c
              hal/amiga/amiga_audio.c)
  if(AJ_TARGET STREQUAL "aos3-m68k")
    set(AJ_PLATFORM_DEF AJ_AMIGA_060)
    list(APPEND HAL_SRC hal/amiga/amiga_malloc.c)   # size-class malloc over exec
  elseif(AJ_TARGET STREQUAL "wos-ppc")
    set(AJ_PLATFORM_DEF AJ_AMIGA_WOS)
    list(APPEND HAL_SRC hal/amiga/amiga_malloc.c)
  else()
    set(AJ_PLATFORM_DEF AJ_AMIGA_MOS)
  endif()
else()
  message(FATAL_ERROR "Unsupported AJ_TARGET ${AJ_TARGET}")
endif()

add_library(hal STATIC ${HAL_SRC})
if(AJ_PLATFORM_DEF)
  target_compile_definitions(hal PRIVATE ${AJ_PLATFORM_DEF}=1 AJ_AMIGA_BUILD=1)
  target_include_directories(hal PRIVATE hal/amiga)
  target_compile_options(hal PRIVATE -w)
endif()
if(AJ_TARGET STREQUAL "aos3-m68k")
  # libnix's swapstack.o gives main() the stack __stack asks for
  target_link_options(hal PUBLIC -Wl,-u,___stkswap)
endif()
target_include_directories(hal PUBLIC hal/include)
if(AJ_TARGET STREQUAL "host")
  # SDL2 window only for 64-bit host builds; the 32-bit VM host build
  # (host32 toolchain) renders offscreen and dumps PPM screenshots.
  if(NOT CMAKE_C_FLAGS MATCHES "-m32")
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(SDL2 IMPORTED_TARGET sdl2)
    if(SDL2_FOUND)
      target_link_libraries(hal PUBLIC PkgConfig::SDL2)
      target_compile_definitions(hal PUBLIC AJ_HAVE_SDL=1)
    endif()
  endif()
  target_link_libraries(hal PUBLIC m)
endif()
target_compile_options(hal PRIVATE -Wall -Wextra)

# aj_add_program(name sources...): normal executable, or ET_REL .warpelf on WarpOS
if(AJ_TARGET STREQUAL "wos-ppc")
  # Host tool: ET_REL .warpelf -> WarpOS hunk executable (replaces the
  # Amiga-side Elf2Exe2). From the civctp2 Amiga port.
  set(ELF2WARPOS ${CMAKE_BINARY_DIR}/elf2warpos)
  add_custom_command(OUTPUT ${ELF2WARPOS}
    COMMAND cc -O2 -o ${ELF2WARPOS} ${CMAKE_SOURCE_DIR}/tools/elf2warpos/elf2warpos.c
    DEPENDS ${CMAKE_SOURCE_DIR}/tools/elf2warpos/elf2warpos.c
            ${CMAKE_SOURCE_DIR}/tools/elf2warpos/stub.inc)
  add_custom_target(elf2warpos_tool DEPENDS ${ELF2WARPOS})
endif()

# aj_add_program(name src... [LIBS lib...]): normal executable, or an ET_REL
# .warpelf on WarpOS, converted to a WarpOS executable by tools/elf2warpos
function(aj_add_program name)
  cmake_parse_arguments(P "" "" "LIBS" ${ARGN})
  if(AJ_TARGET STREQUAL "wos-ppc")
    add_library(${name}_obj OBJECT ${P_UNPARSED_ARGUMENTS})
    target_link_libraries(${name}_obj PRIVATE hal ${P_LIBS})
    set(libfiles "")
    foreach(l ${P_LIBS})
      list(APPEND libfiles $<TARGET_FILE:${l}>)
    endforeach()
    set(out ${CMAKE_CURRENT_BINARY_DIR}/${name}.warpelf)
    add_custom_command(OUTPUT ${out}
      COMMAND ${WOS_LD} -r -EB -d -x -T ${CMAKE_SOURCE_DIR}/cmake/warpos-etrel.ld
              -L${WOS_SDK_ROOT}/newlib/lib -L${WOS_SDK_ROOT}/lib -L${WOS_LIBGCC_DIR} -o ${out}
              ${WOS_SDK_ROOT}/lib/startupwos.o $<TARGET_OBJECTS:${name}_obj> ${libfiles} $<TARGET_FILE:hal>
              --start-group -lamigawos -lgccwos -lm -lglosswos -lcwos -lglosswos -lgccwos --end-group
              ${WOS_SDK_ROOT}/lib/end.o
      COMMAND ${CMAKE_SOURCE_DIR}/tools/validate-etrel.sh ${WOS_TOOLCHAIN_BIN} ${out}
      COMMAND ${ELF2WARPOS} --stack 1048576 ${out} ${CMAKE_CURRENT_BINARY_DIR}/${name}
      DEPENDS ${name}_obj hal ${P_LIBS} $<TARGET_OBJECTS:${name}_obj> ${ELF2WARPOS}
              ${CMAKE_SOURCE_DIR}/cmake/warpos-etrel.ld COMMAND_EXPAND_LISTS)
    add_custom_target(${name} ALL DEPENDS ${out})
  else()
    add_executable(${name} ${P_UNPARSED_ARGUMENTS})
    target_link_libraries(${name} hal ${P_LIBS})
  endif()
endfunction()

aj_add_program(aj_hello src/hello.c)

if(NOT AJ_TARGET STREQUAL "host" OR CMAKE_C_FLAGS MATCHES "-m32")
  add_subdirectory(vm)
endif()
