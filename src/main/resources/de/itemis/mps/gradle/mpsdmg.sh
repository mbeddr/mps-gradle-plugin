#!/bin/bash
if [[ $# -ne 3 ]]; then
  cat <<EOF
Usage:

  $0 SIT_FILE OUTPUT_FILE BG_PIC

Creates a .dmg image named OUTPUT_FILE using SIT_FILE as content and BG_PIC as the background picture.

1. Extracts SIT_FILE into a temporary directory.
2. Sets up BG_PIC as the background picture of the image.
3. Creates a .dmg image in OUTPUT_FILE
EOF
  exit 1
fi

set -o errexit # Exit immediately on any error

# Arguments
SIT_FILE="$1"
OUTPUT_FILE="$2"
BG_PIC="$3"

EXPLODED=$(mktemp -d) || exit 1

# make sure only one dmg is built at a given moment
echo "Unzipping $SIT_FILE to $EXPLODED..."
ditto -x -k "$SIT_FILE" "$EXPLODED"
BUILD_NAME=$(ls "$EXPLODED")
echo "Build Name: $BUILD_NAME"
VOLNAME=$(echo "$BUILD_NAME" | sed 's/\.app$//')
echo "Vol Name: $VOLNAME"

# Commented out since cbmc-mac is mbeddr-specific.
#TODO Implement a generic way to add things to the image, including icons and layout (see mpsdmg.pl)
#echo "copying cbmc-mac to ${EXPLODED}..."
#cp -R ./cbmc-mac ./${EXPLODED}

mkdir "$EXPLODED/.background"
cp "$BG_PIC" "$EXPLODED/.background"
ln -s /Applications "$EXPLODED/ " # Single space as the file name is on purpose

# Allocate space for .DS_Store
dd if=/dev/zero of="$EXPLODED/DSStorePlaceHolder" bs=1024 count=512

# Add .dmg suffix so that hdiutil isn't tempted to append one itself.
temp_dmg=$(mktemp -u).dmg
echo "Creating unpacked r/w disk image $VOLNAME in $temp_dmg..."
hdiutil create -srcfolder "$EXPLODED" -volname "$VOLNAME" -anyowners -nospotlight -quiet -fs HFS+ -fsargs "-c c=64,a=16,e=16" -format UDRW "$temp_dmg"

# mount this image
echo "Mounting unpacked r/w disk image..."
device=$(hdiutil attach -readwrite -noverify -noautoopen "$temp_dmg" | egrep '^/dev/' | sed 1q | awk '{print $1}')
echo "Mounted as $device."
sleep 10

# set properties
echo "Updating disk image styles..."
rm "/Volumes/$VOLNAME/DSStorePlaceHolder"
arch -32 perl5.18 mpsdmg.pl "$VOLNAME" $(basename "$BG_PIC")
sync;sync;sync
hdiutil detach "$device"

echo "Compressing r/w disk image to $OUTPUT_FILE..."
rm -f "$OUTPUT_FILE"
hdiutil convert "$temp_dmg" -quiet -format UDZO -imagekey zlib-level=9 -o "$OUTPUT_FILE"
rm -f "$temp_dmg"

hdiutil internet-enable -no "$OUTPUT_FILE"
rm -rf "$EXPLODED"
