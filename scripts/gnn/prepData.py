import json, sys
import numpy as np

'''
File - prepData.py

This file will take the graphs produced by dataFormatter.py and
produce the final representation of the graphs. It will produce
the representation of each node in the graph for the GNN to
perform calculations on. It will also produce a set of edge files
which will contain the edges
'''
tokenDict = json.load(open(sys.argv[2]+"/tokenDict.json"))

def makeFinalRep(graph):
    graphDict = dict()
    graphDict = json.load(open(graph))
    
    nodeRepresentations = []
    counter = 0
    tokenToNum = dict()
    for token in graphDict["tokens"]:
        if token in tokenToNum:
            continue
        else:
            tokenToNum[token] = counter
            counter+=1
        aList = np.array([0]*len(tokenDict))
        aList[tokenDict[graphDict["tokens"][token]]] = 1
        nodeRepresentations.append(aList)
    
    ASTDict = []
    for outNode in graphDict["AST"]:
        for inNode in graphDict["AST"][outNode]:
            assert(outNode in tokenToNum)
            assert(inNode in tokenToNum)
            ASTDict.append([tokenToNum[outNode],tokenToNum[inNode]])

    CFGDict = []
    for outNode in graphDict["CFG"]:
        for inNode in graphDict["CFG"][outNode]:
            if outNode not in tokenToNum:
                print(graph)
                print(graphDict['CFG'][outNode])
                print(outNode)
                assert()
            assert(inNode in tokenToNum)
            CFGDict.append([tokenToNum[outNode],tokenToNum[inNode]])
            
    DFGDict = []
    try:
        for outNode in graphDict["DFG"]:
            for inNode in graphDict["DFG"][outNode]:
                if type(inNode) == list:
                    for node in inNode:
                        DFGDict.append([tokenToNum[outNode],tokenToNum[node]])
                else:
                    if inNode not in tokenToNum:
                        continue
                    if outNode not in tokenToNum:
                        continue
                    DFGDict.append([tokenToNum[outNode],tokenToNum[inNode]])
    except KeyError:
        print(graph)
    nodeRepresentations = np.array(nodeRepresentations)
    np.savez_compressed(graph+"Edges.npz", AST = np.array(ASTDict), CFG = np.array(CFGDict), DFG = np.array(DFGDict))
    np.savez_compressed(graph+".npz", node_rep = nodeRepresentations)
    

makeFinalRep(sys.argv[1])