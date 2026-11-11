# phoneME CLDC-HI (C interpreter loop) for the Amiga targets.
# Requires the generated tree from tools/gen-vm-amiga.sh.
set(PM ${CMAKE_SOURCE_DIR}/vm/phoneme-cldc/src)
set(VMGEN ${CMAKE_SOURCE_DIR}/build/vmgen/amiga_c/target CACHE PATH "generated CLDC-HI target tree")
set(VM_BUILD release CACHE STRING "release or debug (debug checks oopmaps and asserts)")

if(NOT EXISTS ${VMGEN}/generated/jvmconfig.h)
  message(FATAL_ERROR "Run tools/gen-vm-amiga.sh first (missing ${VMGEN}/generated)")
endif()

# empty ROM image (non-romized build: classes are loaded from cldc_classes.zip)
configure_file(${PM}/vm/share/ROM/ROMSkeleton.cpp ${CMAKE_BINARY_DIR}/vmgen/ROMImage.cpp COPYONLY)

set(VM_SRC
  ${VMGEN}/generated/_MergedSrc001.cpp
  ${VMGEN}/generated/_MergedSrc002.cpp
  ${VMGEN}/generated/_MergedSrc003.cpp
  ${VMGEN}/generated/_MergedSrc004.cpp
  ${VMGEN}/generated/_MergedSrc005.cpp
  ${VMGEN}/generated/_MergedSrc006.cpp
  ${VMGEN}/generated/NativesTable.cpp
  ${VMGEN}/${VM_BUILD}/OopMaps.cpp
  ${CMAKE_BINARY_DIR}/vmgen/ROMImage.cpp
  ${PM}/vm/cpu/c/FloatSupport_c.cpp
  ${PM}/vm/share/runtime/HotRoutines0.cpp
  ${PM}/vm/share/runtime/HotRoutines1.cpp
  ${PM}/vm/share/runtime/JarFileUncommon.cpp
  ${PM}/vm/share/runtime/jvmspi.cpp
  ${PM}/vm/share/runtime/OsFile.cpp
  ${PM}/vm/share/runtime/OsMemory.cpp
  ${PM}/vm/share/natives/KniUncommon.cpp
  ${PM}/vm/share/natives/ReflectNatives.cpp
  ${PM}/vm/os/amiga/Main_amiga.cpp
  ${PM}/vm/os/amiga/OS_amiga.cpp
  ${PM}/vm/os/amiga/OsFile_amiga.cpp
  ${PM}/vm/os/amiga/OsMemory_amiga.cpp
  ${PM}/vm/os/amiga/OsMisc_amiga.cpp
  ${PM}/vm/os/amiga/OsSocket_amiga.cpp
)

add_library(cldcvm STATIC ${VM_SRC})
target_link_libraries(cldcvm PUBLIC hal)

set(VM_INC ${VMGEN}/generated
  ${PM}/vm/cpu/c ${PM}/vm/os/amiga ${PM}/vm/os/utilities
  ${PM}/vm/share/compiler ${PM}/vm/share/debugger ${PM}/vm/share/float
  ${PM}/vm/share/handles ${PM}/vm/share/interpreter ${PM}/vm/share/isolate
  ${PM}/vm/share/memory ${PM}/vm/share/memoryprofiler ${PM}/vm/share/natives
  ${PM}/vm/share/reflection ${PM}/vm/share/ROM ${PM}/vm/share/runtime
  ${PM}/vm/share/utilities ${PM}/vm/share/verifier)
target_include_directories(cldcvm PRIVATE ${VM_INC})
target_include_directories(cldcvm PUBLIC ${PM}/vm/share/natives ${PM}/vm/share/runtime)

target_compile_definitions(cldcvm PRIVATE
  AMIGA_J2ME=1 GCC=1 REQUIRES_JVMCONFIG_H=1
  HARDWARE_LITTLE_ENDIAN=0 HOST_LITTLE_ENDIAN=0
  MSW_FIRST_FOR_DOUBLE=1 MSW_FIRST_FOR_LONG=1
  SUPPORTS_MONOTONIC_CLOCK=1
  JVM_RELEASE_VERSION="1.1" JVM_BUILD_VERSION="amiga-j2me" JVM_NAME="phoneME Feature VM")
if(VM_BUILD STREQUAL "debug")
  target_compile_definitions(cldcvm PRIVATE _DEBUG AZZERT)
endif()

target_compile_options(cldcvm PRIVATE
  $<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions -fno-rtti -fno-gnu-keywords -fno-operator-names>
  -O2 -w
  # 2007-era C++: keep the optimizer from exploiting UB the code relies on
  -fno-strict-aliasing -fno-delete-null-pointer-checks)

# NOTE: m68k aligns int/pointer struct members to 2 bytes, while the host
# generator (i386) that computed OopMaps.cpp aligns to 4. -malign-int would
# match it but crashes bebbo gcc 6.5; the debug VM verifies the oopmaps at
# startup (jvm_check_oopmap) instead.
if(NOT AJ_TARGET STREQUAL "aos3-m68k")
  target_compile_options(cldcvm PRIVATE -fpermissive -Wno-narrowing -fno-lifetime-dse)
endif()

# VM launcher (console): cldc_vm -classpath <cldc_classes.zip>;<app> Main
aj_add_program(aj_cldc ${CMAKE_SOURCE_DIR}/src/vm_main.c LIBS cldcvm)
