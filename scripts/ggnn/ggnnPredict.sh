#!/bin/bash

[ -d ggnnLogFiles ] || mkdir ggnnLogFiles
DIR=`dirname "$0"`
FILE=`basename $1`
$DIR/../../llvm-project/build/bin/graph-builder $1 > ggnnLogFiles/${FILE}graph.txt
python3 $DIR/dataFormatter.py ggnnLogFiles/${FILE}graph.txt
python3 $DIR/prepData.py ggnnLogFiles/${FILE}graph.json $DIR
python3 $DIR/predict.py ggnnLogFiles/${FILE}graph.json.npz ggnnLogFiles/${FILE}graph.jsonEdges.npz $DIR/model.pt