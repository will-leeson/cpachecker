#!/bin/bash

[ -d ggnnLogFiles ] || mkdir ggnnLogFiles
DIR=`dirname "$0"`
$DIR/graph-builder $1 > ggnnLogFiles/tempFile.txt
python3 $DIR/dataFormatter.py ggnnLogFiles/tempFile.txt
python3 $DIR/prepData.py ggnnLogFiles/tempFile.json $DIR
python3 $DIR/predict.py ggnnLogFiles $DIR/model.pt