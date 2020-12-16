import glob, json, os.path, sys
import numpy as np, torch

tokenDict = json.load(open(sys.argv[2]+"/tokenDict.json"))

def handler(graph):
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
        aList = np.array([0]*150)
        try:
            aList[tokenDict[graphDict["tokens"][token]]] = 1
        except:
            aList[-1] = 1
        nodeRepresentations.append(aList)
    
    ASTDict = []
    for outNode in graphDict["AST"]:
        for inNode in graphDict["AST"][outNode]:
            assert(outNode in tokenToNum)
            assert(inNode in tokenToNum)
            ASTDict.append([tokenToNum[inNode],tokenToNum[outNode]])

    CFGDict = []
    for outNode in graphDict["CFG"]:
        for inNode in graphDict["CFG"][outNode]:
            assert(outNode in tokenToNum)
            assert(inNode in tokenToNum)
            CFGDict.append([tokenToNum[inNode],tokenToNum[outNode]])
            
    DFGDict = []
    for outNode in graphDict["DFG"]:
        for inNode in graphDict["DFG"][outNode]:
            if type(inNode) == list:
                for node in inNode:
                   DFGDict.append([tokenToNum[node],tokenToNum[outNode]])
            else:
                if inNode not in tokenToNum:
                    continue
                if outNode not in tokenToNum:
                    continue
                DFGDict.append([tokenToNum[inNode],tokenToNum[outNode]])
    nodeRepresentations = np.array(nodeRepresentations)
    np.savez_compressed(graph+"Edges.npz", AST = np.array(ASTDict), CFG = np.array(CFGDict), DFG = np.array(DFGDict))
    np.savez_compressed(graph+".npz", node_rep = nodeRepresentations)
    

handler(sys.argv[1])