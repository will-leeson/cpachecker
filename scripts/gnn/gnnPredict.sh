#!/bin/bash

[ -d gnnLogFiles ] || mkdir gnnLogFiles
DIR=`dirname "$0"`
FILE=`basename $1`
[ -d gnnLogFiles/$FILE ] || mkdir gnnLogFiles/$FILE
$DIR/../../llvm-project/build/bin/graph-builder $1 > gnnLogFiles/${FILE}/${FILE}graph.txt
python3 $DIR/dataFormatter.py gnnLogFiles/${FILE}/${FILE}graph.txt gnnLogFiles/$FILE
python3 $DIR/prepData.py gnnLogFiles/${FILE}/${FILE}graph.json $DIR
python3 $DIR/predict.py gnnLogFiles/${FILE}/${FILE}graph.json.npz gnnLogFiles/${FILE}/${FILE}graph.jsonEdges.npz $DIR/model.pt gnnLogFiles/$FILE