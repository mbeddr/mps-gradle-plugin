#!/bin/bash
USAGE="Usage:

$0 -r <rcp_file> -o <output_dir> [-j <jdk_file>] [-p <password keychain> -k <keychain> -i <sign identity>]

1. Extracts RCP_FILE into OUTPUT_DIR
2. Creates symlinks Contents/bin/*.dylib -> *.jnilib
3. If JDK_FILE is given extracts JDK_FILE under Contents/jre/
4. Builds help indices using hiutil if help is present under Contents/Resources/
5. Sets executable permissions on Contents/MacOS/* and appropriate Contents/bin/ files
6. If given, signs the application with the passed SIGN_PW, SIGN_KEY_CHAIN and SIGN_IDENTITY
IMPORTANT: All arguments must use absolute paths because the script changes the current directory several times!
"
set -o errexit # Exit immediately on any error

# Parse arguments
while getopts ":r:o:j:p:k:i:h" option; 
do
  case "${option}" in
    r) RCP_FILE="$OPTARG";;
    o) OUTPUT_DIR="$OPTARG";;
    j) JDK_FILE="$OPTARG";;
    p) SIGN_PW="$OPTARG";;
    k) SIGN_KEY_CHAIN="$OPTARG";;
    i) SIGN_IDENTITY="$OPTARG";;
    h) echo "$USAGE" 
       exit 0;;
    \?) echo "illegal option: -$OPTARG usage: $0 -r <rcp_file> -o <output_dir> [-j <jdk_file>] [-p <password keychain> -k <keychain> -i <sign identity>]" >&2
        exit 1;;
    :) echo "option: -$OPTARG requires an argument" >&2
       exit 1;;
  esac
done
shift $((OPTIND -1)) #remove options that have already been handled from $@

if [[ -z "$RCP_FILE" || -z "$OUTPUT_DIR" ]]; then
  echo "$USAGE"
  exit 1
fi

echo "Unzipping $RCP_FILE to $OUTPUT_DIR..."
unzip -q -o "$RCP_FILE" -d "$OUTPUT_DIR"
BUILD_NAME=$(ls "$OUTPUT_DIR")
CONTENTS="$OUTPUT_DIR/$BUILD_NAME/Contents"

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

HELP_FILE=$(ls "$CONTENTS/Resources/" | grep -i help) || HELP_FILE=''
HELP_DIR="$CONTENTS/Resources/$HELP_FILE/Contents/Resources/English.lproj/"

if [[ -d "$HELP_DIR" ]]; then
    echo "Building help indices for $HELP_DIR"
    hiutil -Cagvf "$HELP_DIR/search.helpindex" "$HELP_DIR"
fi

# Make sure your certificate is imported into local KeyChain
if [[ -n "$SIGN_PW" && -n "$SIGN_KEY_CHAIN" && -n "$SIGN_IDENTITY" ]]; then
    echo "Signing application $BUILD_NAME"
    echo "key chain: $SIGN_KEY_CHAIN"
    echo "sign identity: $SIGN_IDENTITY"
    security unlock-keychain -p $SIGN_PW $SIGN_KEY_CHAIN
    codesign -v --deep -s "$SIGN_IDENTITY" "$OUTPUT_DIR/$BUILD_NAME"
    echo "signing is done"
    echo "check sign"
    codesign -v "$OUTPUT_DIR/$BUILD_NAME" -vvvvv
    echo "check sign done"
else
    echo "for signing the application $BUILD_NAME: SIGN_PW, SIGN_KEY_CHAIN and SIGN_IDENTITY needs to be provided"
fi

chmod a+x "$CONTENTS"/MacOS/*
chmod a+x "$CONTENTS"/bin/*.py
chmod a+x "$CONTENTS"/bin/fs*
chmod a+x "$CONTENTS"/bin/restarter