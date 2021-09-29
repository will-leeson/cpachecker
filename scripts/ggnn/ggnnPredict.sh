#!/bin/bash

[ -d ggnnLogFiles ] || mkdir ggnnLogFiles
DIR=`dirname "$0"`
FILE=`basename $1`
echo $FILE
[ -d ggnnLogFiles/$FILE ] || mkdir ggnnLogFiles/$FILE
$DIR/../../llvm-project/build/bin/graph-builder $1 > ggnnLogFiles/${FILE}/${FILE}graph.txt
python3 $DIR/dataFormatter.py ggnnLogFiles/${FILE}/${FILE}graph.txt ggnnLogFiles/$FILE
python3 $DIR/prepData.py ggnnLogFiles/${FILE}/${FILE}graph.json $DIR
python3 $DIR/predict.py ggnnLogFiles/${FILE}/${FILE}graph.json.npz ggnnLogFiles/${FILE}/${FILE}graph.jsonEdges.npz $DIR/model.pt ggnnLogFiles/$FILE