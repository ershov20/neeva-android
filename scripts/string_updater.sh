#!/bin/bash

OUTPUT_DIR=/tmp/`date "+%y-%m-%d-%H-%M"`
echo $OUTPUT_DIR

unzip "$1" -d "$OUTPUT_DIR"

mv "$OUTPUT_DIR/de-DE/strings.xml" "app/src/main/res/values-de-rDE/strings.xml"
mv "$OUTPUT_DIR/es-ES/strings.xml" "app/src/main/res/values-es-rES/strings.xml"
mv "$OUTPUT_DIR/fr-FR/strings.xml" "app/src/main/res/values-fr-rFR/strings.xml"

# TODO(danalcantara): Add Spanish support once we start shipping those strings.
