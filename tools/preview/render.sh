#!/bin/sh
# Compile the art code + desktop renderer and write preview PNGs to tools/preview/out/.
set -e
cd "$(dirname "$0")/../.."
OUT=tools/preview/build; rm -rf "$OUT"; mkdir -p "$OUT" tools/preview/out
javac -nowarn -d "$OUT" app/src/main/java/com/benjaminosleo/locumwatch/art/*.java tools/preview/*.java
java -Djava.awt.headless=true -cp "$OUT" Render "$@"
