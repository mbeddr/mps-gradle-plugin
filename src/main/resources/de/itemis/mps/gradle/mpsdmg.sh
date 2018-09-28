#!/bin/bash
if [[ $# -ne 3 ]]; then
  cat <<EOF
Usage:

  $0 INPUT_DIR OUTPUT_FILE BG_PIC

Creates a .dmg image named OUTPUT_FILE using INPUT_DIR as content and BG_PIC as the background picture.

1. Sets up BG_PIC as the background picture of the image.
2. Creates a .dmg image in OUTPUT_FILE
EOF
  exit 1
fi

set -o errexit # Exit immediately on any error

# Arguments
INPUT_DIR="$1"
OUTPUT_FILE="$2"
BG_PIC="$3"

BUILD_NAME=$(ls "$INPUT_DIR")
echo "Build Name: $BUILD_NAME"
VOLNAME=$(echo "$BUILD_NAME" | sed 's/\.app$//')
echo "Vol Name: $VOLNAME"

# Commented out since cbmc-mac is mbeddr-specific.
#TODO Implement a generic way to add things to the image, including icons and layout (see mpsdmg.pl)
#echo "copying cbmc-mac to ${INPUT_DIR}..."
#cp -R ./cbmc-mac ./${INPUT_DIR}

mkdir "$INPUT_DIR/.background"
cp "$BG_PIC" "$INPUT_DIR/.background"
ln -s /Applications "$INPUT_DIR/ " # Single space as the file name is on purpose

# Allocate space for .DS_Store
dd if=/dev/zero of="$INPUT_DIR/DSStorePlaceHolder" bs=1024 count=512

# Add .dmg suffix so that hdiutil isn't tempted to append one itself.
temp_dmg=$(mktemp -u).dmg
echo "Creating unpacked r/w disk image $VOLNAME in $temp_dmg..."
hdiutil create -srcfolder "$INPUT_DIR" -volname "$VOLNAME" -anyowners -nospotlight -quiet -fs HFS+ -fsargs "-c c=64,a=16,e=16" -format UDRW "$temp_dmg"

# mount this image
echo "Mounting unpacked r/w disk image..."
DEVICE_DETAILS=$(hdiutil attach -readwrite -noverify -noautoopen "$temp_dmg")
DEVICE_NAME=$(echo $DEVICE_DETAILS | egrep '^/dev/' | sed 1q | awk '{print $1}')
TMP_VOL_PATH=$(echo $DEVICE_DETAILS | egrep -o '/Volumes/.*$')
echo "Mounted as $DEVICE_NAME under $TMP_VOL_PATH."
sleep 10

# set properties
echo "Updating disk image styles..."
rm "$TMP_VOL_PATH/DSStorePlaceHolder"
arch -32 perl5.18 mpsdmg.pl "$(basename "$TMP_VOL_PATH")" "$VOLNAME" $(basename "$BG_PIC")
sync;sync;sync
hdiutil detach "$DEVICE_NAME"

echo "Compressing r/w disk image to $OUTPUT_FILE..."
rm -f "$OUTPUT_FILE"
hdiutil convert "$temp_dmg" -quiet -format UDZO -imagekey zlib-level=9 -o "$OUTPUT_FILE"
rm -f "$temp_dmg"

hdiutil internet-enable -no "$OUTPUT_FILE"
