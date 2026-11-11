# Linux host, 32-bit (CLDC-HI is a 32-bit VM). Runs the same os/amiga VM
# layer as the Amiga targets on top of hal/sdl-host, for fast testing.
set(AJ_TARGET host)
set(CMAKE_C_FLAGS_INIT   "-m32")
set(CMAKE_CXX_FLAGS_INIT "-m32")
set(CMAKE_EXE_LINKER_FLAGS_INIT "-m32")
