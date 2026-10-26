# Third-party notices

## QuickJS-ng

Spowlo bundles QuickJS-ng to give yt-dlp a JavaScript runtime for its EJS challenges.

- Version: 0.17.0
- Source: https://github.com/quickjs-ng/quickjs/tree/v0.17.0
- License: MIT (see below)

| File | ABI | Contents |
| --- | --- | --- |
| `app/src/main/jniLibs/arm64-v8a/libqjs.so` | arm64-v8a | QuickJS-ng shared library |
| `app/src/main/jniLibs/arm64-v8a/libqjs_bin.so` | arm64-v8a | `qjs` CLI |

`libqjs_bin.so` is the upstream `qjs` executable. Android only lets an app execute files from its `nativeLibraryDir`, and the build only packages native libraries whose name ends in `.so`, so it is shipped under that name.

### Build

The binaries come from the `v0.17.0` tag, built with the Android NDK (clang 21.0.0), release mode and `BUILD_SHARED_LIBS=ON`:

```sh
cmake -B build -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=ON
cmake --build build --target qjs_exe
```

Copy `libqjs.so` from the build directory, and `qjs` as `libqjs_bin.so`.

### License

```
The MIT License (MIT)

Copyright (c) 2017-2026 Fabrice Bellard
Copyright (c) 2017-2024 Charlie Gordon
Copyright (c) 2023-2026 Ben Noordhuis
Copyright (c) 2023-2026 Saúl Ibarra Corretgé

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
