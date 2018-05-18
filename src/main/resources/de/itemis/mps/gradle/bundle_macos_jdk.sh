#!/bin/bash
if [[ $# -ne 4 ]]; then
  cat <<EOF
Usage:

  $0 RCP_FILE TMP_DIR JDK_FILE OUT_FILE

1. Extracts RCP_FILE into TMP_DIR
2. Creates symlinks Contents/bin/*.dylib -> *.jnilib if any .jnilib files are present
   (this used to be the case for earlier versions of MPS)
3. If JDK_FILE is given, extracts JDK_FILE under Contents/jre/
4. Sets executable permissions on Contents/MacOS/* and appropriate Contents/bin/ files
5. Compresses the result into OUT_FILE (tar/gzip)

IMPORTANT: All arguments must use absolute paths because the script changes the current directory several times!
EOF
  exit 1
fi

set -o errexit # Exit immediately on any error

# Arguments
RCP_FILE="$1"
TMP_DIR="$2"
JDK_FILE="$3"
OUT_FILE="$4"

echo "Unzipping $RCP_FILE to $TMP_DIR..."
unzip -q -o "$RCP_FILE" -d "$TMP_DIR"
BUILD_NAME=$(ls "$TMP_DIR")
CONTENTS="$TMP_DIR/$BUILD_NAME/Contents"

if ls "$CONTENTS/bin"/*.jnilib >& /dev/null; then
  echo 'Creating symlinks from *.jnilib to *.dylib:'
  for f in "$CONTENTS/bin"/*.jnilib; do
    b="$(basename "$f" .jnilib)"
    echo "  $f -> $b.dylib"
    ln -sf "$b.jnilib" "$(dirname "$f")/$b.dylib"
  done
  echo 'Done creating symlinks'
fi

if [[ -n "$JDK_FILE" ]]; then
  if [[ ! -f "$JDK_FILE" ]]; then
    echo "$JDK_FILE is not a file"
    exit 1
  fi
  echo "Modifying Info.plist"
  sed -i -e 's/1.6\*/1.6\+/' "$CONTENTS/Info.plist"
  sed -i -e 's/NoJavaDistribution/custom-jdk-bundled/' "$CONTENTS/Info.plist"

  # TODO This command appears to be useless, it only inserts a blank line into the file.
  sed -i -e '/public.app-category.developer-tools/G' "$CONTENTS/Info.plist"

  sed -i -e '/public.app-category.developer-tools/a\'$'\n''<key>NSSupportsAutomaticGraphicsSwitching</key><true/>' "$CONTENTS/Info.plist"

  # sed -i -e seems to work with both GNU sed and OS X sed, with the latter leaving the original file with -e suffix
  # as a backup.
  rm -f "$CONTENTS/Info.plist-e"
  echo "Info.plist has been modified"

  echo "Extracting JDK: $JDK_FILE to $CONTENTS/jre"
  mkdir -p "$CONTENTS/jre"
  pushd "$CONTENTS/jre"
  COPY_EXTENDED_ATTRIBUTES_DISABLE=true COPYFILE_DISABLE=true tar xvf "$JDK_FILE" --exclude='._jdk' || exit 1
  echo "JDK has been extracted"
  popd
fi

chmod a+x "$CONTENTS"/MacOS/*
chmod a+x "$CONTENTS"/bin/*.py
chmod a+x "$CONTENTS"/bin/fs*
chmod a+x "$CONTENTS"/bin/restarter

cd "$TMP_DIR"
COPY_EXTENDED_ATTRIBUTES_DISABLE=true COPYFILE_DISABLE=true tar czvf "$OUT_FILE" "$BUILD_NAME"
echo "Bundle created in $OUT_FILE"