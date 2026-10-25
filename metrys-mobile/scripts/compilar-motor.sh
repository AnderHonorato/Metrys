#!/usr/bin/env bash
set -euo pipefail
LLAMA_COMMIT=620d2225cc436ab0b001e3200e590e3b286ef9ab
PROJETO=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
FERRAMENTAS=${FERRAMENTAS:-"$(dirname -- "$PROJETO")/ferramentas"}
ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"$FERRAMENTAS/sdk"}
ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-"$FERRAMENTAS/android-ndk-r29"}
LLAMA_SOURCE_DIR=${LLAMA_SOURCE_DIR:-"$FERRAMENTAS/llama.cpp-620d222"}
CMAKE=${CMAKE:-"$ANDROID_SDK_ROOT/cmake/3.31.6/bin/cmake"}
NINJA=${NINJA:-"$ANDROID_SDK_ROOT/cmake/3.31.6/bin/ninja"}
BUILD_MOTOR=${BUILD_MOTOR:-"$PROJETO/android/cpp/build-arm64"}
if [[ ! -f "$LLAMA_SOURCE_DIR/include/llama.h" ]]; then
 mkdir -p "$(dirname -- "$LLAMA_SOURCE_DIR")"
 git init "$LLAMA_SOURCE_DIR"
 git -C "$LLAMA_SOURCE_DIR" remote add origin https://github.com/ggml-org/llama.cpp.git
 git -C "$LLAMA_SOURCE_DIR" fetch --depth 1 origin "$LLAMA_COMMIT"
 git -C "$LLAMA_SOURCE_DIR" checkout --detach FETCH_HEAD
fi
if [[ -d "$LLAMA_SOURCE_DIR/.git" ]]; then
 [[ "$(git -C "$LLAMA_SOURCE_DIR" rev-parse HEAD)" == "$LLAMA_COMMIT" ]] || exit 1
else
 [[ -f "$LLAMA_SOURCE_DIR/METRYS_COMMIT" && "$(cat "$LLAMA_SOURCE_DIR/METRYS_COMMIT")" == "$LLAMA_COMMIT" ]] || { echo "Fonte llama.cpp sem procedência fixada" >&2;exit 1; }
fi
"$CMAKE" -S "$PROJETO/android/cpp" -B "$BUILD_MOTOR" -G Ninja -DCMAKE_MAKE_PROGRAM="$NINJA" \
 -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" -DCMAKE_BUILD_TYPE=Release \
 -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static \
 -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON -DLLAMA_SOURCE_DIR="$LLAMA_SOURCE_DIR"
"$CMAKE" --build "$BUILD_MOTOR" --target metrys --parallel "${JOBS:-4}"
mkdir -p "$PROJETO/android/jniLibs/arm64-v8a"
cp "$BUILD_MOTOR/saida/libmetrys.so" "$PROJETO/android/jniLibs/arm64-v8a/libmetrys.so"
"$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$PROJETO/android/jniLibs/arm64-v8a/libmetrys.so"
