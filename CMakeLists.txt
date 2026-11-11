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
elseif(AJ_TARGET STREQUAL "aos3-m68k" OR AJ_TARGET STREQUAL "morphos-ppc")
  set(HAL_SRC hal/aos3-m68k/hal_amiga.c)
elseif(AJ_TARGET STREQUAL "wos-ppc")
  set(HAL_SRC hal/wos-ppc/hal_wos.c)
else()
  message(FATAL_ERROR "Unsupported AJ_TARGET ${AJ_TARGET}")
endif()

# fast size-class malloc over exec AllocMem / AllocVecPPC (see the file)
if(AJ_TARGET STREQUAL "aos3-m68k")
  list(APPEND HAL_SRC hal/amiga/amiga_malloc.c)
  set(AJ_MALLOC_DEF AJ_AMIGA_060)
elseif(AJ_TARGET STREQUAL "wos-ppc")
  list(APPEND HAL_SRC hal/amiga/amiga_malloc.c)
  set(AJ_MALLOC_DEF AJ_AMIGA_WOS)
endif()

add_library(hal STATIC ${HAL_SRC})
if(AJ_MALLOC_DEF)
  target_compile_definitions(hal PRIVATE ${AJ_MALLOC_DEF})
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
# aj_add_program(name src... [LIBS lib...]): normal executable, or an ET_REL
# .warpelf on WarpOS (converted on the Amiga with Elf2Exe2)
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
      DEPENDS ${name}_obj hal ${P_LIBS} $<TARGET_OBJECTS:${name}_obj> COMMAND_EXPAND_LISTS)
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
